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
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        TeamMember requester = requireActiveMember(teamId, memberId);

        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);
        // 투표가 아직 열려있는 팀만 "다음 라운드가 뭘지" 예측해야 한다(currentRound는 직전
        // 라운드가 꽉 찼으면 다음 라운드로 미리 넘어간다). 이미 확정된 팀에 이 예측을 그대로
        // 쓰면 승자를 결정지은 마지막 라운드가 아니라 그다음(투표가 하나도 없는) 라운드를 조회해
        // 득표수가 전부 0으로 보이는 버그가 생긴다 — 확정된 팀은 실제로 표가 쌓인 마지막
        // 라운드(maxRound)를 그대로 조회한다.
        int round = team.getStatus() == TeamStatus.CONTEST_SELECTING
                ? currentRound(teamId, activeMembers.size())
                : lastVotedRound(teamId);
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

    /**
     * 원하는 공모전을 최대 2개까지 선택해 투표한다. 활성 팀원 전원이 투표하면 자동 개표한다.
     * 마감 전이면 같은 라운드 안에서 몇 번이든 다시 투표해 선택을 바꿀 수 있다 — 기존 표는
     * 지우고 새 선택으로 덮어쓴다.
     */
    @Transactional
    public void submitVote(Long teamId, Long voterMemberId, List<Long> contestCandidateIds) {
        Team team = requireTeamInContestSelectingWithLock(teamId);
        TeamMember voter = requireActiveMember(teamId, voterMemberId);

        // 마감 확정 스케줄러(resolveDeadlineIfDue)가 아직 안 돌아 팀이 여전히 CONTEST_SELECTING
        // 이더라도, 마감 시각이 지난 뒤의 투표는 집계에 반영되면 안 된다(투표 마감 리마인더가
        // 마감 임박 사용자를 계속 투표로 유도하므로 이 경계에서 실제로 발생할 수 있는 race다).
        LocalDateTime deadline = team.getContestCandidateDeadlineAt();
        if (deadline != null && !LocalDateTime.now().isBefore(deadline)) {
            throw new ContestException(ContestErrorCode.CONTEST_VOTE_DEADLINE_PASSED);
        }

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
        // 마감 전이면 재투표를 허용한다 — 새로 저장하기 전에 이번 라운드의 기존 표를 지운다.
        // ContestVote는 IDENTITY 채번이라 뒤이은 save()가 즉시 INSERT를 실행하므로, 삭제를
        // flush로 먼저 DB에 반영해두지 않으면 겹치는 후보(예: {A,B}→{A,C})에서 unique 제약
        // (uq_contest_votes_candidate_voter_round)을 위반한다 — SurveyService.submitSurvey의
        // 동일 패턴과 같은 이유로 flush 필요.
        contestVoteRepository.deleteByTeam_TeamIdAndVoterTeamMember_TeamMemberIdAndRound(
                teamId, voter.getTeamMemberId(), round);
        contestVoteRepository.flush();

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

        // submitVote(마지막 투표 제출)/resolveDeadlineIfDue(마감 스케줄러)와 거의 동시에
        // 팀원이 나가면 셋 다 "개표 조건 충족"을 각자 판단해 tally()를 중복 실행할 수 있다 —
        // 잠금으로 세 경로를 직렬화하고, 호출자가 넘겨준 team은 잠금 이전 스냅샷이라 신뢰하지
        // 않고 잠금 획득 직후 상태를 다시 확인한다.
        Team locked = teamRepository
                .findByIdWithLock(team.getTeamId())
                .orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        if (locked.getStatus() != TeamStatus.CONTEST_SELECTING) {
            return;
        }

        List<TeamMember> activeMembers =
                teamMemberRepository.findByTeamIdAndStatus(locked.getTeamId(), TeamMemberStatus.ACTIVE);
        if (activeMembers.isEmpty()) {
            return;
        }

        Integer maxRound = contestVoteRepository.findMaxRoundByTeamId(locked.getTeamId());
        if (maxRound == null) {
            return;
        }
        boolean leaverAlreadyVoted = contestVoteRepository.existsByTeam_TeamIdAndVoterTeamMember_TeamMemberIdAndRound(
                locked.getTeamId(), leftTeamMemberId, maxRound);
        if (leaverAlreadyVoted) {
            return;
        }

        long distinctVoters = contestVoteRepository.countDistinctVotersByTeamIdAndRound(locked.getTeamId(), maxRound);
        if (distinctVoters >= activeMembers.size()) {
            tally(locked, maxRound);
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
            // 동률: 동률 후보들만 대상으로 다음 라운드 재투표를 안내한다. 남은 시간을 그대로
            // 물려받지 않고 새 라운드에도 24시간을 새로 준다. 직전 라운드에서 마감 리마인더가
            // 이미 발행됐을 수 있으므로(findDueContestVoteReminderTeamIds가
            // contestVoteReminderNotifiedAt IS NULL로 조회), 새 라운드에서 다시 리마인더가
            // 나갈 수 있도록 플래그도 같이 초기화한다.
            team.scheduleContestCandidateDeadline(
                    LocalDateTime.now().plusHours(ChatbotOrchestrationService.CONTEST_CANDIDATE_TIMEOUT_HOURS));
            team.markContestVoteReminderNotified(null);
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
        // submitVote/recheckAfterMemberLeft와 동시에 실행될 수 있으므로 잠금으로 직렬화한다.
        Team team = teamRepository
                .findByIdWithLock(teamId)
                .orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
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

    // 투표가 끝난(더 이상 CONTEST_SELECTING이 아닌) 팀의 마지막 라운드. 표가 하나도 없으면
    // (참여자 0명으로 무작위 확정된 경우) 1라운드로 취급한다 — eligibleCandidates(1)이 전체
    // 후보 목록을 반환하므로 득표수 0인 결과 화면으로 자연스럽게 표시된다.
    private int lastVotedRound(Long teamId) {
        Integer maxRound = contestVoteRepository.findMaxRoundByTeamId(teamId);
        return maxRound == null ? 1 : maxRound;
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

    // submitVote가 개표(tally → decideContest)로 이어질 수 있는 유일한 사용자 요청 경로라,
    // recheckAfterMemberLeft/resolveDeadlineIfDue와 팀 행을 잠가 직렬화한다. 뒤에 잠금을 얻는
    // 트랜잭션은 앞선 트랜잭션이 커밋한 최신 상태(CONTEST_DECIDED)를 보고 INVALID_TEAM_STATUS로
    // 안전하게 실패한다 — addCandidate/removeCandidate처럼 개표와 무관한 조작까지 잠글 필요는
    // 없어 requireTeamInContestSelecting과 별도로 둔다.
    private Team requireTeamInContestSelectingWithLock(Long teamId) {
        Team team = teamRepository
                .findByIdWithLock(teamId)
                .orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
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
