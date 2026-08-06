package org.cotato.gongmozip.domains.contest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.chat.enums.MessageType;
import org.cotato.gongmozip.domains.chat.service.ChatService;
import org.cotato.gongmozip.domains.chatbot.service.ChatbotOrchestrationService;
import org.cotato.gongmozip.domains.contest.converter.ContestVotingConverter;
import org.cotato.gongmozip.domains.contest.dto.response.ContestResponse.ContestCandidateItemResponse;
import org.cotato.gongmozip.domains.contest.dto.response.ContestResponse.ContestCandidateListResponse;
import org.cotato.gongmozip.domains.contest.dto.response.ContestResponse.ContestVoteStatusResponse;
import org.cotato.gongmozip.domains.contest.dto.response.ContestResponse.ContestVoteTallyItemResponse;
import org.cotato.gongmozip.domains.contest.entity.Contest;
import org.cotato.gongmozip.domains.contest.entity.ContestCandidate;
import org.cotato.gongmozip.domains.contest.entity.ContestVote;
import org.cotato.gongmozip.domains.contest.exception.ContestException;
import org.cotato.gongmozip.domains.contest.exception.codes.ContestErrorCode;
import org.cotato.gongmozip.domains.contest.repository.ContestCandidateRepository;
import org.cotato.gongmozip.domains.contest.repository.ContestRepository;
import org.cotato.gongmozip.domains.contest.repository.ContestVoteRepository;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.domains.team.exception.TeamException;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 팀의 공모전 후보 추가/투표 (ContestCandidate, ContestVote). 공모전 투표는 최대 2개까지
 * 다중선택 가능하다 (docs/decisions/04-contest-voting.md). 후보 추가/투표는 팀이
 * CONTEST_SELECTING 상태인 동안 계속 열려있다 — 후보 마감/투표 마감을 나누는 스케줄러는
 * Phase 7에서 붙는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContestVotingService {

    private static final int MAX_VOTE_SELECTION = 2;
    private static final Random RANDOM = new Random();

    private final ChatService chatService;
    private final ChatbotOrchestrationService chatbotOrchestrationService;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ContestRepository contestRepository;
    private final ContestCandidateRepository contestCandidateRepository;
    private final ContestVoteRepository contestVoteRepository;
    // 이 프로젝트에는 Spring이 자동 구성한 ObjectMapper 빈이 없어 직접 생성한다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public ContestCandidateItemResponse addCandidate(Long teamId, Long memberId, Long contestId) {
        Team team = requireTeamInContestSelecting(teamId);
        TeamMember member = requireActiveMember(teamId, memberId);
        Contest contest = contestRepository
                .findById(contestId)
                .orElseThrow(() -> new ContestException(ContestErrorCode.CONTEST_NOT_FOUND));

        if (contestCandidateRepository.existsByTeam_TeamIdAndContest_ContestId(teamId, contestId)) {
            throw new ContestException(ContestErrorCode.DUPLICATE_CONTEST_CANDIDATE);
        }

        ContestCandidate saved = contestCandidateRepository.save(ContestCandidate.builder()
                .team(team)
                .contest(contest)
                .addedByTeamMember(member)
                .build());
        chatService.postSystemMessage(team, member.getProfile().getNickname() + "님이 공모전을 후보로 추가했습니다.");
        return ContestVotingConverter.toContestCandidateItemResponse(saved, LocalDateTime.now());
    }

    /**
     * 공모전 탭에서 공유한 공모전을 채팅방에 카드로 남긴다(기능명세서 3.4.2/5.1.4). 이 시점에는
     * 후보로 등록되지 않는다 — 후보 등록은 카드의 "+" 버튼을 눌러 {@link #addCandidate}를
     * 별도로 호출해야 한다. 공유 자체는 공모전 선정 단계가 아니어도(다른 팀 상태에서도) 할 수
     * 있게 팀 상태를 제한하지 않는다.
     */
    @Transactional
    public void shareContest(Long teamId, Long memberId, Long contestId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        TeamMember sharer = requireActiveMember(teamId, memberId);
        Contest contest = contestRepository
                .findById(contestId)
                .orElseThrow(() -> new ContestException(ContestErrorCode.CONTEST_NOT_FOUND));

        chatService.postChatbotCardMessage(
                team,
                MessageType.CONTEST_SHARE_CARD,
                sharer.getProfile().getNickname() + "님이 공모전을 공유했어요. 후보로 추가하려면 + 버튼을 눌러주세요.",
                toContestIdMetadata(contest.getContestId()));
    }

    @Transactional
    public void removeCandidate(Long teamId, Long memberId, Long contestCandidateId) {
        requireTeamInContestSelecting(teamId);
        requireActiveMember(teamId, memberId);

        ContestCandidate candidate = contestCandidateRepository
                .findByTeam_TeamIdAndContestCandidateId(teamId, contestCandidateId)
                .orElseThrow(() -> new ContestException(ContestErrorCode.CONTEST_CANDIDATE_NOT_FOUND));
        contestCandidateRepository.delete(candidate);
    }

    public ContestCandidateListResponse getCandidates(Long teamId, Long memberId) {
        teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        requireActiveMember(teamId, memberId);

        List<ContestCandidate> candidates = contestCandidateRepository.findByTeamId(teamId);
        return ContestVotingConverter.toContestCandidateListResponse(candidates, LocalDateTime.now());
    }

    /**
     * 현재 라운드의 투표 진행 상황을 조회한다. 전원이 투표를 마치기 전에도 호출할 수 있어
     * "N명 참여중" 카운터와 후보별 득표 막대그래프(Figma "공모전 투표"/"투표 결과" 화면)를
     * 그릴 수 있다 — 확정 여부(승자 결정, 동률 재투표 등)는 이 API가 아니라 평소처럼 팀 채팅의
     * 카드 메시지로 온다.
     */
    public ContestVoteStatusResponse getVoteStatus(Long teamId, Long memberId) {
        teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        TeamMember requester = requireActiveMember(teamId, memberId);

        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);
        int round = currentRound(teamId, activeMembers.size());
        List<ContestCandidate> eligible = eligibleCandidates(teamId, round);
        List<ContestVote> votes = contestVoteRepository.findByTeam_TeamIdAndRound(teamId, round);

        Map<Long, Long> voteCountByCandidateId = votes.stream()
                .collect(Collectors.groupingBy(
                        vote -> vote.getContestCandidate().getContestCandidateId(), Collectors.counting()));
        long participatedVoterCount = votes.stream()
                .map(vote -> vote.getVoterTeamMember().getTeamMemberId())
                .distinct()
                .count();
        boolean myVoted = votes.stream()
                .anyMatch(vote -> vote.getVoterTeamMember().getTeamMemberId().equals(requester.getTeamMemberId()));

        LocalDateTime now = LocalDateTime.now();
        List<ContestVoteTallyItemResponse> results = eligible.stream()
                .map(candidate -> ContestVotingConverter.toContestVoteTallyItemResponse(
                        candidate, voteCountByCandidateId.getOrDefault(candidate.getContestCandidateId(), 0L), now))
                .sorted(Comparator.comparingLong(ContestVoteTallyItemResponse::voteCount)
                        .reversed())
                .toList();

        return new ContestVoteStatusResponse(round, activeMembers.size(), participatedVoterCount, myVoted, results);
    }

    /** 원하는 공모전을 최대 2개까지 선택해 투표한다. 활성 팀원 전원이 투표하면 자동 개표한다. */
    @Transactional
    public void submitVote(Long teamId, Long voterMemberId, List<Long> contestCandidateIds) {
        Team team = requireTeamInContestSelecting(teamId);
        TeamMember voter = requireActiveMember(teamId, voterMemberId);

        if (contestCandidateIds == null
                || contestCandidateIds.isEmpty()
                || contestCandidateIds.size() > MAX_VOTE_SELECTION
                || contestCandidateIds.size() != new HashSet<>(contestCandidateIds).size()) {
            throw new ContestException(ContestErrorCode.INVALID_CONTEST_VOTE_SELECTION);
        }

        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);
        int round = currentRound(teamId, activeMembers.size());
        Map<Long, ContestCandidate> eligibleById = eligibleCandidates(teamId, round).stream()
                .collect(Collectors.toMap(ContestCandidate::getContestCandidateId, candidate -> candidate));

        for (Long candidateId : contestCandidateIds) {
            if (!eligibleById.containsKey(candidateId)) {
                throw new ContestException(ContestErrorCode.CONTEST_CANDIDATE_NOT_FOUND);
            }
        }
        if (contestVoteRepository.existsByTeam_TeamIdAndVoterTeamMember_TeamMemberIdAndRound(
                teamId, voter.getTeamMemberId(), round)) {
            throw new ContestException(ContestErrorCode.ALREADY_VOTED_CONTEST);
        }

        for (Long candidateId : contestCandidateIds) {
            contestVoteRepository.save(ContestVote.builder()
                    .team(team)
                    .contestCandidate(eligibleById.get(candidateId))
                    .voterTeamMember(voter)
                    .round(round)
                    .build());
        }

        long distinctVoters = contestVoteRepository.countDistinctVotersByTeamIdAndRound(teamId, round);
        if (distinctVoters >= activeMembers.size()) {
            tally(team, round);
        }
    }

    /**
     * 팀원이 공모전 투표 도중 나갔을 때(TeamService.leaveTeam) 호출된다. submitVote는 투표가
     * 제출되는 시점에만 개표 조건을 확인하므로, 나간 사람이 그 라운드에 아직 투표하지 않은
     * 상태였다면(=개표를 막고 있던 사람이었다면) 남은 활성 팀원 수 기준으로 이미 조건이
     * 충족됐는지 즉시 재확인한다. 나간 사람이 이미 투표했었다면 개표가 이미 실행됐거나 다른
     * 미투표자가 남아있는 것이므로 아무 것도 하지 않는다(중복 개표 방지).
     */
    @Transactional
    public void recheckAfterMemberLeft(Team team, Long leftTeamMemberId) {
        if (team.getStatus() != TeamStatus.CONTEST_SELECTING) {
            return;
        }

        List<TeamMember> activeMembers =
                teamMemberRepository.findByTeamIdAndStatus(team.getTeamId(), TeamMemberStatus.ACTIVE);
        if (activeMembers.isEmpty()) {
            return;
        }

        Integer maxRound = contestVoteRepository.findMaxRoundByTeamId(team.getTeamId());
        if (maxRound == null) {
            return;
        }
        boolean leaverAlreadyVoted = contestVoteRepository.existsByTeam_TeamIdAndVoterTeamMember_TeamMemberIdAndRound(
                team.getTeamId(), leftTeamMemberId, maxRound);
        if (leaverAlreadyVoted) {
            return;
        }

        long distinctVoters = contestVoteRepository.countDistinctVotersByTeamIdAndRound(team.getTeamId(), maxRound);
        if (distinctVoters >= activeMembers.size()) {
            tally(team, maxRound);
        }
    }

    private void tally(Team team, int round) {
        List<ContestVote> votes = contestVoteRepository.findByTeam_TeamIdAndRound(team.getTeamId(), round);
        Map<Long, Long> voteCountByCandidateId = votes.stream()
                .collect(Collectors.groupingBy(
                        vote -> vote.getContestCandidate().getContestCandidateId(), Collectors.counting()));

        long maxVotes =
                voteCountByCandidateId.values().stream().max(Long::compareTo).orElse(0L);
        List<Long> topCandidateIds = voteCountByCandidateId.entrySet().stream()
                .filter(entry -> entry.getValue() == maxVotes)
                .map(Map.Entry::getKey)
                .toList();

        if (topCandidateIds.size() == 1) {
            ContestCandidate winner = votes.stream()
                    .map(ContestVote::getContestCandidate)
                    .filter(candidate -> candidate.getContestCandidateId().equals(topCandidateIds.get(0)))
                    .findFirst()
                    .orElseThrow(() -> new ContestException(ContestErrorCode.CONTEST_CANDIDATE_NOT_FOUND));
            decideContest(team, winner, "투표 결과, \"" + winner.getContest().getTitle() + "\"이(가) 팀 공모전으로 확정되었습니다!");
        } else {
            // 동률: 동률 후보들만 대상으로 다음 라운드 재투표를 안내한다.
            chatService.postChatbotCardMessage(
                    team,
                    MessageType.CONTEST_VOTE_CARD,
                    "동률이 발생했어요. 동률이 나온 공모전들끼리 재투표를 진행할게요.",
                    toCandidateMetadata(topCandidateIds));
        }
    }

    /**
     * 후보/투표 마감 시각이 지났는데도 팀이 CONTEST_SELECTING이면 스케줄러가 호출한다. 투표가
     * 아예 없었다면(참여자 0명) 챗봇이 후보 중 하나를 무작위로 대신 골라 확정한다 (실제 AI 추천은
     * Phase 8에서 연결). 일부라도 투표가 있었다면 평소와 같은 개표 로직을 그대로 적용한다.
     */
    @Transactional
    public void resolveDeadlineIfDue(Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        if (team.getStatus() != TeamStatus.CONTEST_SELECTING) {
            return;
        }

        List<ContestCandidate> candidates = contestCandidateRepository.findByTeamId(teamId);
        if (candidates.isEmpty()) {
            return;
        }

        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);
        int round = currentRound(teamId, activeMembers.size());
        List<ContestVote> votes = contestVoteRepository.findByTeam_TeamIdAndRound(teamId, round);

        if (votes.isEmpty()) {
            ContestCandidate picked = candidates.get(RANDOM.nextInt(candidates.size()));
            decideContest(
                    team,
                    picked,
                    "투표에 참여한 인원이 없어, 여러분이 더 좋은 결과를 낼 수 있을 것 같은 공모전을 대신 골라드렸어요: \""
                            + picked.getContest().getTitle() + "\"");
        } else {
            tally(team, round);
        }
    }

    private void decideContest(Team team, ContestCandidate winner, String announcement) {
        team.assignContest(winner.getContest());
        team.advanceStatus(TeamStatus.CONTEST_DECIDED);
        LocalDateTime applyEndAt = winner.getContest().getApplyEndAt();
        LocalDateTime teamCreatedAt = team.getCreatedAt();
        Duration halfway = Duration.between(teamCreatedAt, applyEndAt).dividedBy(2);
        team.scheduleCheckpoints(teamCreatedAt.plus(halfway), applyEndAt.minusDays(1));
        chatService.postChatbotCardMessage(
                team,
                MessageType.CONTEST_RESULT_CARD,
                announcement,
                toContestIdMetadata(winner.getContest().getContestId()));
        chatbotOrchestrationService.advanceToInProgress(team);
    }

    // 라운드가 없으면 1, 마지막 라운드가 활성 팀원 전원의 투표를 다 받았는데도 결정되지
    // 않았다면(=동률로 끝남) 다음 라운드로 넘어간다.
    private int currentRound(Long teamId, int activeMemberCount) {
        Integer maxRound = contestVoteRepository.findMaxRoundByTeamId(teamId);
        if (maxRound == null) {
            return 1;
        }
        long distinctVotersInMaxRound = contestVoteRepository.countDistinctVotersByTeamIdAndRound(teamId, maxRound);
        return distinctVotersInMaxRound >= activeMemberCount ? maxRound + 1 : maxRound;
    }

    private List<ContestCandidate> eligibleCandidates(Long teamId, int round) {
        if (round == 1) {
            return contestCandidateRepository.findByTeamId(teamId);
        }

        List<ContestVote> previousRoundVotes = contestVoteRepository.findByTeam_TeamIdAndRound(teamId, round - 1);
        Map<Long, Long> voteCountByCandidateId = previousRoundVotes.stream()
                .collect(Collectors.groupingBy(
                        vote -> vote.getContestCandidate().getContestCandidateId(), Collectors.counting()));
        long maxVotes =
                voteCountByCandidateId.values().stream().max(Long::compareTo).orElse(0L);

        Map<Long, ContestCandidate> topCandidatesById = new LinkedHashMap<>();
        for (ContestVote vote : previousRoundVotes) {
            ContestCandidate candidate = vote.getContestCandidate();
            if (voteCountByCandidateId.get(candidate.getContestCandidateId()) == maxVotes) {
                topCandidatesById.putIfAbsent(candidate.getContestCandidateId(), candidate);
            }
        }
        return List.copyOf(topCandidatesById.values());
    }

    private String toCandidateMetadata(List<Long> contestCandidateIds) {
        try {
            return objectMapper.writeValueAsString(Map.of("contestCandidateIds", contestCandidateIds));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("공모전 후보 메타데이터 직렬화에 실패했습니다.", e);
        }
    }

    // CONTEST_SHARE_CARD/CONTEST_RESULT_CARD 모두 {contestId: id} 형태로 내려, 프론트가 카드
    // 종류와 무관하게 같은 방식으로 썸네일/제목/D-day를 조회해 렌더링할 수 있게 한다.
    private String toContestIdMetadata(Long contestId) {
        try {
            return objectMapper.writeValueAsString(Map.of("contestId", contestId));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("공모전 카드 메타데이터 직렬화에 실패했습니다.", e);
        }
    }

    private Team requireTeamInContestSelecting(Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        if (team.getStatus() != TeamStatus.CONTEST_SELECTING) {
            throw new TeamException(TeamErrorCode.INVALID_TEAM_STATUS);
        }
        return team;
    }

    private TeamMember requireActiveMember(Long teamId, Long memberId) {
        return teamMemberRepository
                .findByTeam_TeamIdAndMember_MemberId(teamId, memberId)
                .filter(teamMember -> teamMember.getStatus() == TeamMemberStatus.ACTIVE)
                .orElseThrow(() -> new TeamException(TeamErrorCode.NOT_TEAM_MEMBER));
    }
}
