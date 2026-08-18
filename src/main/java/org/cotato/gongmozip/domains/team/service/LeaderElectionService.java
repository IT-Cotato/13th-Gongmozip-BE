package org.cotato.gongmozip.domains.team.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.domains.chat.entity.Message;
import org.cotato.gongmozip.domains.chat.enums.MessageType;
import org.cotato.gongmozip.domains.chat.repository.MessageRepository;
import org.cotato.gongmozip.domains.chat.service.ChatService;
import org.cotato.gongmozip.domains.chatbot.service.ChatbotOrchestrationService;
import org.cotato.gongmozip.domains.team.dto.response.TeamResponse.LeaderCandidacyStatusResponse;
import org.cotato.gongmozip.domains.team.dto.response.TeamResponse.LeaderVoteStatusResponse;
import org.cotato.gongmozip.domains.team.dto.response.TeamResponse.LeaderVoteTallyItemResponse;
import org.cotato.gongmozip.domains.team.entity.LeaderVote;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.LeaderCandidacyStatus;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.domains.team.exception.TeamException;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.domains.team.repository.LeaderVoteRepository;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 팀장 선출 플로우 (LeaderVote). 케이스①(AUTO_ASSIGNED)은 TeamService/ChatbotOrchestrationService
 * 선에서 끝나 이 서비스에 도달하지 않고, 케이스②(OPEN_NOMINATION)·③(CANDIDATE_VOTE)의 후보
 * 등록/투표/동률/마감 처리를 담당한다 (docs/decisions/02-leader-election.md).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaderElectionService {

    private static final Random RANDOM = new Random();
    // 팀장 투표 마감 시간(2026-08-15, 후보 등록 마감과 분리하면서 결정). 후보 등록 마감(3시간,
    // ChatbotOrchestrationService.LEADER_CANDIDACY_TIMEOUT_HOURS)과 달리, 투표는 라운드(동률
    // 재투표 포함)가 새로 열릴 때마다 이 시간만큼 마감을 다시 잡는다 — docs/decisions/02-leader-election.md 참고.
    private static final int LEADER_VOTE_TIMEOUT_HOURS = 8;

    private final ChatService chatService;
    private final ChatbotOrchestrationService chatbotOrchestrationService;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final LeaderVoteRepository leaderVoteRepository;
    private final MessageRepository messageRepository;
    private final LeaderTiebreakAsyncService leaderTiebreakAsyncService;
    // 이 프로젝트에는 Spring이 자동 구성한 ObjectMapper 빈이 없어 직접 생성한다
    // (단순 후보 id 목록 직렬화 용도라 커스텀 설정이 필요 없음).
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** "팀장 여부 투표" 응답을 저장한다. 활성 팀원 전원이 응답을 마치면 다음 단계로 넘어간다. */
    @Transactional
    public void submitCandidacy(Long teamId, Long memberId, boolean wants) {
        Team team = requireTeamInLeaderSelectingWithLock(teamId);
        if (leaderVoteRepository.existsByTeam_TeamId(teamId)) {
            // 이미 팀장 투표가 시작된 뒤에는 여부 투표를 되돌릴 수 없다.
            throw new TeamException(TeamErrorCode.INVALID_TEAM_STATUS);
        }

        TeamMember member = requireActiveMember(teamId, memberId);
        member.updateLeaderCandidacy(wants ? LeaderCandidacyStatus.WANTS : LeaderCandidacyStatus.DOES_NOT_WANT);

        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);
        boolean allResolved =
                activeMembers.stream().allMatch(tm -> tm.getLeaderCandidacy() != LeaderCandidacyStatus.UNDECIDED);
        if (allResolved) {
            resolveCandidacyPhase(team, activeMembers);
        }
    }

    /**
     * "팀장 여부 투표"(candidacy) 진행 상황을 조회한다. Figma는 이미 응답한 사람에게 "팀장 여부
     * 투표" 버튼을 비활성화해서 보여주는데, 이를 판단할 신호가 프론트에 없었다 — 공모전 투표의
     * {@code ContestVotingService.getVoteStatus}(myVoted)와 동일한 목적으로 추가했다
     * (2026-08-18, Figma 5.1.3.2 확인 후).
     */
    public LeaderCandidacyStatusResponse getCandidacyStatus(Long teamId, Long memberId) {
        TeamMember requester = requireActiveMember(teamId, memberId);
        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);
        long respondedCount = activeMembers.stream()
                .filter(tm -> tm.getLeaderCandidacy() != LeaderCandidacyStatus.UNDECIDED)
                .count();
        boolean myResponded = requester.getLeaderCandidacy() != LeaderCandidacyStatus.UNDECIDED;
        return new LeaderCandidacyStatusResponse(
                activeMembers.size(),
                respondedCount,
                myResponded,
                requester.getLeaderCandidacy().name());
    }

    /**
     * 팀장 후보 투표 진행 상황을 조회한다. 전원이 투표를 마치기 전에도 호출할 수 있어 "N명
     * 참여중" 카운터와 "팀장 투표하기" 버튼 비활성화(내가 이미 투표했는지)를 그릴 수 있다 —
     * {@code ContestVotingService.getVoteStatus}와 동일한 패턴(2026-08-18).
     */
    public LeaderVoteStatusResponse getVoteStatus(Long teamId, Long memberId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        TeamMember requester = requireActiveMember(teamId, memberId);
        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);

        // 투표가 아직 열려있는 팀만 "다음 라운드가 뭘지" 예측해야 한다 — 이미 확정된 팀에 이
        // 예측을 그대로 쓰면 승자를 결정지은 마지막 라운드가 아니라 그다음(투표가 하나도 없는)
        // 라운드를 조회해 득표수가 전부 0으로 보이는 버그가 생긴다
        // (ContestVotingService.getVoteStatus와 동일한 이유로 분기).
        int round = team.getStatus() == TeamStatus.LEADER_SELECTING
                ? currentRound(teamId, activeMembers.size())
                : lastVotedRound(teamId);
        List<LeaderVote> votes = leaderVoteRepository.findByTeam_TeamIdAndRound(teamId, round);

        Map<Long, Long> voteCountByCandidateId = votes.stream()
                .collect(Collectors.groupingBy(
                        vote -> vote.getCandidateTeamMember().getTeamMemberId(), Collectors.counting()));
        long participatedVoterCount = votes.stream()
                .map(vote -> vote.getVoterTeamMember().getTeamMemberId())
                .distinct()
                .count();
        boolean myVoted = votes.stream()
                .anyMatch(vote -> vote.getVoterTeamMember().getTeamMemberId().equals(requester.getTeamMemberId()));

        List<LeaderVoteTallyItemResponse> results = eligibleCandidateIds(teamId, round, activeMembers).stream()
                .map(candidateId -> new LeaderVoteTallyItemResponse(
                        candidateId, voteCountByCandidateId.getOrDefault(candidateId, 0L)))
                .sorted(Comparator.comparingLong(LeaderVoteTallyItemResponse::voteCount)
                        .reversed())
                .toList();

        return new LeaderVoteStatusResponse(round, activeMembers.size(), participatedVoterCount, myVoted, results);
    }

    /** 팀장 후보에게 투표한다. 활성 팀원 전원이 투표를 마치면 개표한다. */
    @Transactional
    public void castVote(Long teamId, Long voterMemberId, Long candidateTeamMemberId) {
        Team team = requireTeamInLeaderSelectingWithLock(teamId);
        TeamMember voter = requireActiveMember(teamId, voterMemberId);

        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);
        boolean allResolved =
                activeMembers.stream().allMatch(tm -> tm.getLeaderCandidacy() != LeaderCandidacyStatus.UNDECIDED);
        if (!allResolved) {
            throw new TeamException(TeamErrorCode.LEADER_CANDIDACY_PENDING);
        }

        int round = currentRound(teamId, activeMembers.size());
        List<Long> eligibleCandidateIds = eligibleCandidateIds(teamId, round, activeMembers);
        if (!eligibleCandidateIds.contains(candidateTeamMemberId)) {
            throw new TeamException(TeamErrorCode.INVALID_LEADER_CANDIDATE);
        }
        if (leaderVoteRepository.existsByTeam_TeamIdAndVoterTeamMember_TeamMemberIdAndRound(
                teamId, voter.getTeamMemberId(), round)) {
            throw new TeamException(TeamErrorCode.ALREADY_VOTED_LEADER);
        }

        TeamMember candidate = activeMembers.stream()
                .filter(tm -> tm.getTeamMemberId().equals(candidateTeamMemberId))
                .findFirst()
                .orElseThrow(() -> new TeamException(TeamErrorCode.INVALID_LEADER_CANDIDATE));

        leaderVoteRepository.save(LeaderVote.builder()
                .team(team)
                .voterTeamMember(voter)
                .candidateTeamMember(candidate)
                .round(round)
                .build());

        long votesInRound =
                leaderVoteRepository.findByTeam_TeamIdAndRound(teamId, round).size();
        if (votesInRound >= activeMembers.size()) {
            tally(team, round, activeMembers);
        }
    }

    // 팀장 여부 투표가 끝난 직후 후보 수에 따라 분기한다.
    private void resolveCandidacyPhase(Team team, List<TeamMember> activeMembers) {
        List<TeamMember> candidates = activeMembers.stream()
                .filter(tm -> tm.getLeaderCandidacy() == LeaderCandidacyStatus.WANTS)
                .toList();

        if (candidates.isEmpty()) {
            TeamMember randomLeader = activeMembers.get(RANDOM.nextInt(activeMembers.size()));
            assignLeader(
                    team,
                    randomLeader,
                    "아직 팀장 후보 지원자가 없어요 :( 원활한 팀 운영을 위해 팀원 중 1명을 임시 팀장으로 무작위 지정했어요. "
                            + randomLeader.getProfile().getNickname() + "님이 임시 팀장으로 선정되었습니다.");
        } else if (candidates.size() == 1) {
            TeamMember onlyCandidate = candidates.get(0);
            assignLeader(team, onlyCandidate, onlyCandidate.getProfile().getNickname() + "님이 팀장으로 선정되었습니다.");
        } else {
            List<Long> candidateIds =
                    candidates.stream().map(TeamMember::getTeamMemberId).toList();
            team.scheduleLeaderVoteDeadline(LocalDateTime.now().plusHours(LEADER_VOTE_TIMEOUT_HOURS));
            chatService.postChatbotCardMessage(
                    team,
                    MessageType.LEADER_VOTE_CARD,
                    "팀장 후보가 여러 명이에요. 팀장이 되면 좋을 것 같은 팀원에게 투표해주세요!",
                    toCandidateMetadata(candidateIds));
        }
    }

    /**
     * 팀원이 팀장 투표 도중 나갔을 때(TeamService.leaveTeam) 호출된다. castVote는 투표가
     * 제출되는 시점에만 개표 조건을 확인하므로, 나간 사람이 그 라운드에 아직 투표하지 않은
     * 상태였다면(=개표를 막고 있던 사람이었다면) 남은 활성 팀원 수 기준으로 이미 조건이
     * 충족됐는지 즉시 재확인한다. 나간 사람이 이미 투표했었다면 개표는 이미 실행됐거나 다른
     * 미투표자가 남아있는 것이므로 아무 것도 하지 않는다(중복 개표 방지).
     */
    @Transactional
    public void recheckAfterMemberLeft(Team team, Long leftTeamMemberId) {
        if (team.getStatus() != TeamStatus.LEADER_SELECTING) {
            return;
        }

        // submitCandidacy/castVote(팀원 요청)나 resolveCandidacyDeadlineIfDue/resolveVoteDeadlineIfDue
        // (스케줄러)와 거의 동시에 팀원이 나가면 여러 경로가 "확정 조건 충족"을 각자 판단해
        // resolveCandidacyPhase/tally를 중복 실행할 수 있다 — 잠금으로 직렬화하고, 호출자가
        // 넘겨준 team은 잠금 이전 스냅샷이라 신뢰하지 않고 잠금 획득 직후 상태를 다시 확인한다.
        Team locked = teamRepository
                .findByIdWithLock(team.getTeamId())
                .orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        if (locked.getStatus() != TeamStatus.LEADER_SELECTING) {
            return;
        }

        List<TeamMember> activeMembers =
                teamMemberRepository.findByTeamIdAndStatus(locked.getTeamId(), TeamMemberStatus.ACTIVE);
        if (activeMembers.isEmpty()) {
            return;
        }

        if (!leaderVoteRepository.existsByTeam_TeamId(locked.getTeamId())) {
            recheckCandidacyPhaseAfterMemberLeft(locked, leftTeamMemberId, activeMembers);
            return;
        }

        Integer maxRound = leaderVoteRepository.findMaxRoundByTeamId(locked.getTeamId());
        if (maxRound == null) {
            return;
        }
        boolean leaverAlreadyVoted = leaderVoteRepository.existsByTeam_TeamIdAndVoterTeamMember_TeamMemberIdAndRound(
                locked.getTeamId(), leftTeamMemberId, maxRound);
        if (leaverAlreadyVoted) {
            return;
        }

        long votesInRound = leaderVoteRepository
                .findByTeam_TeamIdAndRound(locked.getTeamId(), maxRound)
                .size();
        if (votesInRound >= activeMembers.size()) {
            tally(locked, maxRound, activeMembers);
        }
    }

    /**
     * 아직 "팀장 여부 투표"(candidacy) 단계라 {@code LeaderVote}가 하나도 없는 상태에서 팀원이
     * 나간 경우를 처리한다. {@code submitCandidacy}도 새 응답이 제출되는 시점에만 전원 응답
     * 여부를 확인하므로, 마지막으로 응답이 없던(UNDECIDED) 사람이 나가버리면 아무도 다시
     * 확인하지 않아 계속 LEADER_SELECTING에 머무를 수 있다. 나간 사람이 응답 전이었을 때만
     * 재확인한다 — 이미 응답을 마친 사람이 나간 경우는 이 단계의 완료 조건에 영향이 없으므로
     * (다른 미응답자가 남아있거나, 이미 다음 단계로 넘어갔거나) 재확인이 필요 없다.
     */
    private void recheckCandidacyPhaseAfterMemberLeft(
            Team team, Long leftTeamMemberId, List<TeamMember> activeMembers) {
        LeaderCandidacyStatus leaverCandidacy = teamMemberRepository
                .findById(leftTeamMemberId)
                .map(TeamMember::getLeaderCandidacy)
                .orElse(null);
        if (leaverCandidacy != LeaderCandidacyStatus.UNDECIDED) {
            return;
        }

        boolean allResolved =
                activeMembers.stream().allMatch(tm -> tm.getLeaderCandidacy() != LeaderCandidacyStatus.UNDECIDED);
        if (allResolved) {
            resolveCandidacyPhase(team, activeMembers);
        }
    }

    /**
     * 팀장 후보 등록(팀장 여부 투표) 마감 시각이 지났는데도 팀이 LEADER_SELECTING이고 아직
     * 후보 등록이 안 끝났으면(활성 팀원 중 {@code UNDECIDED}가 남아있으면) 스케줄러가 호출한다.
     * 이미 후보 등록이 끝나 투표 단계로 넘어간 팀이면 이 마감은 더 이상 의미가 없으므로(투표
     * 마감은 {@link #resolveVoteDeadlineIfDue}가 별도로 담당) 아무 것도 하지 않는다
     * (2026-08-15, 후보 등록/투표 마감 분리 — docs/decisions/02-leader-election.md 참고).
     */
    @Transactional
    public void resolveCandidacyDeadlineIfDue(Long teamId) {
        // submitCandidacy/recheckAfterMemberLeft와 동시에 실행될 수 있으므로 잠금으로 직렬화한다.
        Team team = teamRepository
                .findByIdWithLock(teamId)
                .orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        if (team.getStatus() != TeamStatus.LEADER_SELECTING) {
            return;
        }

        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);
        if (activeMembers.isEmpty()) {
            return;
        }

        boolean stillAwaitingCandidacy =
                activeMembers.stream().anyMatch(tm -> tm.getLeaderCandidacy() == LeaderCandidacyStatus.UNDECIDED);
        if (!stillAwaitingCandidacy) {
            return;
        }

        // 마감까지 응답하지 않은 사람은 "팀장 안 할래요"로 간주하고 확정한다.
        activeMembers.stream()
                .filter(tm -> tm.getLeaderCandidacy() == LeaderCandidacyStatus.UNDECIDED)
                .forEach(tm -> tm.updateLeaderCandidacy(LeaderCandidacyStatus.DOES_NOT_WANT));
        resolveCandidacyPhase(team, activeMembers);
    }

    /**
     * 팀장 투표 마감 시각이 지났는데도 팀이 LEADER_SELECTING이면 스케줄러가 호출한다. 후보
     * 등록이 아직 안 끝난 팀이면(이론상 후보 등록 마감이 먼저 지났어야 하는 경우) 이 마감은
     * 아직 세팅되지 않았거나 무관하므로 아무 것도 하지 않는다 — 그 처리는
     * {@link #resolveCandidacyDeadlineIfDue}의 몫이다.
     */
    @Transactional
    public void resolveVoteDeadlineIfDue(Long teamId) {
        // castVote/recheckAfterMemberLeft와 동시에 실행될 수 있으므로 잠금으로 직렬화한다.
        Team team = teamRepository
                .findByIdWithLock(teamId)
                .orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        if (team.getStatus() != TeamStatus.LEADER_SELECTING) {
            return;
        }

        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);
        if (activeMembers.isEmpty()) {
            return;
        }

        boolean stillAwaitingCandidacy =
                activeMembers.stream().anyMatch(tm -> tm.getLeaderCandidacy() == LeaderCandidacyStatus.UNDECIDED);
        if (stillAwaitingCandidacy) {
            return;
        }

        // 후보 등록(candidacy)은 끝났고 팀장 투표 단계에서 마감을 맞은 경우. 현재 라운드에 투표가
        // 있으면 있는 대로 개표하고(ContestVotingService.resolveDeadlineIfDue와 동일한 정책),
        // 하나도 없으면 무작위로 임시 팀장을 지정한다.
        int round = currentRound(teamId, activeMembers.size());
        List<LeaderVote> votes = leaderVoteRepository.findByTeam_TeamIdAndRound(teamId, round);
        if (votes.isEmpty()) {
            // 이 분기에 도달했다는 것 자체가 (1라운드면 WANTS 후보 2명 이상, 2라운드 이상이면
            // 직전 라운드 동률 후보가 있어) LEADER_VOTE_CARD가 이미 발행됐다는 뜻이다(0/1명이었다면
            // resolveCandidacyPhase에서 이미 LEADER_DECIDED로 빠졌을 것). castVote와 동일하게
            // eligibleCandidateIds로 이 라운드의 실제 후보 풀만 대상으로 제한한다 — 2라운드
            // 이상에서 이 제한이 없으면 1라운드에서 이미 탈락한(동률이 아니었던) 사람까지
            // 무작위 대상에 포함되는 버그가 있었다(2026-08-15 수정). 빈 리스트가 될 이론상
            // 불가능한 경우에만 방어적으로 activeMembers 전체를 대상으로 한다.
            Set<Long> eligibleIds = new HashSet<>(eligibleCandidateIds(teamId, round, activeMembers));
            List<TeamMember> candidates = activeMembers.stream()
                    .filter(tm -> eligibleIds.contains(tm.getTeamMemberId()))
                    .toList();
            List<TeamMember> randomPool = candidates.isEmpty() ? activeMembers : candidates;
            TeamMember randomLeader = randomPool.get(RANDOM.nextInt(randomPool.size()));
            assignLeader(
                    team,
                    randomLeader,
                    "팀장 투표 마감까지 아무도 투표하지 않아, 팀원 중 1명을 임시 팀장으로 무작위 지정했어요. "
                            + randomLeader.getProfile().getNickname() + "님이 임시 팀장으로 선정되었습니다.");
        } else {
            tally(team, round, activeMembers);
        }
    }

    private void tally(Team team, int round, List<TeamMember> activeMembers) {
        List<LeaderVote> votes = leaderVoteRepository.findByTeam_TeamIdAndRound(team.getTeamId(), round);
        // 투표 이후 득표 후보가 나갔을 수 있으므로, 현재도 활성 상태인 후보를 대상으로 한 표만
        // 집계한다 — 나간 후보가 최다 득표자였다는 이유로 팀장 선출(및 그 트랜잭션에 함께 묶인
        // TeamService.leaveTeam)이 예외로 실패해서는 안 된다.
        Set<Long> activeMemberIds =
                activeMembers.stream().map(TeamMember::getTeamMemberId).collect(Collectors.toSet());
        Map<Long, Long> voteCountByCandidateId = votes.stream()
                .filter(vote ->
                        activeMemberIds.contains(vote.getCandidateTeamMember().getTeamMemberId()))
                .collect(Collectors.groupingBy(
                        vote -> vote.getCandidateTeamMember().getTeamMemberId(), Collectors.counting()));

        if (voteCountByCandidateId.isEmpty()) {
            // 득표했던 후보가 전부 나가 유효 후보가 없으면, 후보가 아예 없었을 때와 동일하게
            // 활성 팀원 중 1명을 임시 팀장으로 무작위 지정한다.
            TeamMember randomLeader = activeMembers.get(RANDOM.nextInt(activeMembers.size()));
            assignLeader(
                    team,
                    randomLeader,
                    "투표했던 후보가 모두 팀을 나가서, 팀원 중 1명을 임시 팀장으로 무작위 지정했어요. "
                            + randomLeader.getProfile().getNickname() + "님이 임시 팀장으로 선정되었습니다.");
            return;
        }

        long maxVotes =
                voteCountByCandidateId.values().stream().max(Long::compareTo).orElse(0L);
        List<Long> topCandidateIds = voteCountByCandidateId.entrySet().stream()
                .filter(entry -> entry.getValue() == maxVotes)
                .map(Map.Entry::getKey)
                .toList();

        if (topCandidateIds.size() == 1) {
            TeamMember winner = activeMembers.stream()
                    .filter(tm -> tm.getTeamMemberId().equals(topCandidateIds.get(0)))
                    .findFirst()
                    .orElseThrow(() -> new TeamException(TeamErrorCode.INVALID_LEADER_CANDIDATE));
            assignLeader(team, winner, "투표 결과, " + winner.getProfile().getNickname() + "님이 팀장으로 선출되었습니다.");
        } else {
            // 동률 시 AI 추천 호출(AiGatewayClient, 최대 20초)은 트랜잭션 밖으로 뺐다 — tally()가
            // castVote(유저 요청)뿐 아니라 resolveVoteDeadlineIfDue(스케줄러)에서도 호출될 수
            // 있어, 이 자리에서 동기로 부르면 스레드/커넥션을 오래 붙잡는 문제가 있었다(스레드풀
            // 점유 이슈 점검, 2026-08-15). 동률 판정까지만 동기로 커밋하고, AI 호출/이후 처리는
            // 커밋 이후 LeaderTiebreakAsyncService가 이어받는다.
            Long teamId = team.getTeamId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        leaderTiebreakAsyncService.resolveTiebreakAsync(teamId, round, topCandidateIds);
                    } catch (TaskRejectedException e) {
                        log.error("팀장 투표 동률 처리 비동기 작업 제출 실패 - teamId: {}", teamId, e);
                    }
                }
            });
        }
    }

    /**
     * 팀장 투표 동률 시 마지막으로 발행된 AI 추천을 수락해 재투표 없이 바로 팀장을 확정한다.
     * 활성 팀원 누구나 수락할 수 있고, 먼저 수락한 사람으로 확정된다(선착순).
     */
    @Transactional
    public void acceptAiRecommendation(Long teamId, Long memberId) {
        Team team = requireTeamInLeaderSelectingWithLock(teamId);
        requireActiveMember(teamId, memberId);

        Message latestVoteCard = latestPendingTiebreakCard(teamId);
        Long recommendedTeamMemberId = extractAiRecommendedTeamMemberId(latestVoteCard.getMetadata());

        TeamMember recommended = teamMemberRepository
                .findById(recommendedTeamMemberId)
                .filter(tm -> tm.getTeam().getTeamId().equals(teamId))
                .filter(tm -> tm.getStatus() == TeamMemberStatus.ACTIVE)
                .orElseThrow(() -> new TeamException(TeamErrorCode.INVALID_LEADER_CANDIDATE));

        assignLeader(team, recommended, "AI 추천을 수락해 " + recommended.getProfile().getNickname() + "님이 팀장으로 확정되었습니다.");
    }

    /**
     * 팀장 투표 동률 시 "재투표하기"를 누른 팀원이 팀 전체에 재투표 시작을 알린다. 실제 투표
     * 대상(라운드, 후보 자격)은 {@link #castVote}가 저장된 표 개수로 자동 계산하므로 여기서는
     * 아무 상태도 바꾸지 않고, 다른 팀원들이 투표 UI를 다시 열 수 있도록 안내 카드만 재발행한다.
     */
    @Transactional
    public void requestRevote(Long teamId, Long memberId) {
        Team team = requireTeamInLeaderSelectingWithLock(teamId);
        requireActiveMember(teamId, memberId);

        Message latestVoteCard = latestPendingTiebreakCard(teamId);
        List<Long> candidateIds = extractCandidateTeamMemberIds(latestVoteCard.getMetadata());

        chatService.postChatbotCardMessage(
                team,
                MessageType.LEADER_VOTE_CARD,
                "팀원들의 의견에 따라 재투표를 진행합니다. 팀장을 다시 선출해 주세요.",
                toCandidateMetadata(candidateIds));
    }

    // 동률 카드(재투표하기/추천 수락하기 두 액션이 공유하는 상태)가 아직 남아있는지 확인한다.
    // 가장 최근 LEADER_VOTE_CARD에 aiRecommendedTeamMemberId가 없으면 동률이 아니라 최초
    // 투표 카드(후보 등록 직후)이므로, 두 액션 모두 대상이 없는 것으로 취급한다.
    private Message latestPendingTiebreakCard(Long teamId) {
        Message latestVoteCard = messageRepository
                .findFirstByTeam_TeamIdAndMessageTypeOrderByCreatedAtDesc(teamId, MessageType.LEADER_VOTE_CARD)
                .orElseThrow(() -> new TeamException(TeamErrorCode.NO_PENDING_AI_RECOMMENDATION));
        if (extractAiRecommendedTeamMemberId(latestVoteCard.getMetadata()) == null) {
            throw new TeamException(TeamErrorCode.NO_PENDING_AI_RECOMMENDATION);
        }
        return latestVoteCard;
    }

    private void assignLeader(Team team, TeamMember leader, String announcement) {
        leader.assignAsLeader();
        team.advanceStatus(TeamStatus.LEADER_DECIDED);
        chatService.postChatbotCardMessage(
                team, MessageType.LEADER_RESULT_CARD, announcement, toLeaderResultMetadata(leader.getTeamMemberId()));
        chatbotOrchestrationService.advanceToContestSelecting(team);
    }

    // 라운드가 없으면 1, 마지막 라운드가 활성 팀원 전원의 표를 다 받았는데도 아직 결정되지
    // 않았다면(=동률로 끝남) 다음 라운드로 넘어간다.
    private int currentRound(Long teamId, int activeMemberCount) {
        Integer maxRound = leaderVoteRepository.findMaxRoundByTeamId(teamId);
        if (maxRound == null) {
            return 1;
        }
        long votesInMaxRound =
                leaderVoteRepository.findByTeam_TeamIdAndRound(teamId, maxRound).size();
        return votesInMaxRound >= activeMemberCount ? maxRound + 1 : maxRound;
    }

    // 투표가 끝난(더 이상 LEADER_SELECTING이 아닌) 팀의 마지막 라운드. 표가 하나도 없으면
    // (참여자 0명으로 무작위 확정된 경우) 1라운드로 취급한다.
    private int lastVotedRound(Long teamId) {
        Integer maxRound = leaderVoteRepository.findMaxRoundByTeamId(teamId);
        return maxRound == null ? 1 : maxRound;
    }

    private List<Long> eligibleCandidateIds(Long teamId, int round, List<TeamMember> activeMembers) {
        if (round == 1) {
            return activeMembers.stream()
                    .filter(tm -> tm.getLeaderCandidacy() == LeaderCandidacyStatus.WANTS)
                    .map(TeamMember::getTeamMemberId)
                    .toList();
        }

        List<LeaderVote> previousRoundVotes = leaderVoteRepository.findByTeam_TeamIdAndRound(teamId, round - 1);
        Map<Long, Long> voteCountByCandidateId = previousRoundVotes.stream()
                .collect(Collectors.groupingBy(
                        vote -> vote.getCandidateTeamMember().getTeamMemberId(), Collectors.counting()));
        long maxVotes =
                voteCountByCandidateId.values().stream().max(Long::compareTo).orElse(0L);
        return voteCountByCandidateId.entrySet().stream()
                .filter(entry -> entry.getValue() == maxVotes)
                .map(Map.Entry::getKey)
                .toList();
    }

    private String toLeaderResultMetadata(Long leaderTeamMemberId) {
        try {
            return objectMapper.writeValueAsString(Map.of("leaderTeamMemberId", leaderTeamMemberId));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("팀장 결과 메타데이터 직렬화에 실패했습니다.", e);
        }
    }

    private String toCandidateMetadata(List<Long> candidateTeamMemberIds) {
        try {
            return objectMapper.writeValueAsString(Map.of("candidateTeamMemberIds", candidateTeamMemberIds));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("팀장 후보 메타데이터 직렬화에 실패했습니다.", e);
        }
    }

    private List<Long> extractCandidateTeamMemberIds(String metadataJson) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(metadataJson, new TypeReference<>() {});
            Object value = parsed.get("candidateTeamMemberIds");
            if (!(value instanceof List<?> rawList)) {
                throw new IllegalStateException("팀장 후보 메타데이터에 candidateTeamMemberIds가 없습니다.");
            }
            return rawList.stream().map(id -> Long.valueOf(id.toString())).toList();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("팀장 후보 메타데이터 역직렬화에 실패했습니다.", e);
        }
    }

    private Long extractAiRecommendedTeamMemberId(String metadataJson) {
        if (metadataJson == null) {
            return null;
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(metadataJson, new TypeReference<>() {});
            Object value = parsed.get("aiRecommendedTeamMemberId");
            return value == null ? null : Long.valueOf(value.toString());
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    // 팀장 여부 투표(resolveCandidacyPhase)든 실제 투표(tally)든, 여러 진입점(팀원 요청 +
    // recheckAfterMemberLeft + 마감 스케줄러)이 같은 팀의 "확정 조건 충족"을 동시에 판단할 수
    // 있어 팀 행을 잠가 직렬화한다. 원래는 requestRevote/acceptAiRecommendation의 동률 카드
    // 경합만 막던 잠금이었는데(두 요청이 거의 동시에 들어오면, 재투표 요청이 카드를 읽은 뒤 AI
    // 추천 수락이 먼저 커밋되어도 재투표 요청이 그 사실을 모른 채 "재투표 진행" 카드를
    // LEADER_DECIDED 이후에 발행할 수 있었다), submitCandidacy/castVote/recheckAfterMemberLeft/
    // resolveCandidacyDeadlineIfDue/resolveVoteDeadlineIfDue까지 전부 여기로 통일했다
    // (ContestVotingService의 findByIdWithLock 적용과 동일한 이유 — 낙관적 락만으로는 진 쪽
    // 트랜잭션이 롤백되기 전에 LEADER_RESULT_CARD/LEADER_VOTE_CARD를 이미 브로드캐스트해버려
    // "확정됐다고 떴다가 사라지는" 유령 카드 증상이 남는다, 2026-08-17).
    //
    // 뒤에 잠금을 얻는 트랜잭션은 앞선 트랜잭션이 커밋한 최신 상태를 보고 안전하게 종료(또는
    // INVALID_TEAM_STATUS)된다.
    private Team requireTeamInLeaderSelectingWithLock(Long teamId) {
        Team team = teamRepository
                .findByIdWithLock(teamId)
                .orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        if (team.getStatus() != TeamStatus.LEADER_SELECTING) {
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
