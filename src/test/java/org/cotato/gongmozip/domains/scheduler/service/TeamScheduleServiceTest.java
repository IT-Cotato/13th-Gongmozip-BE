package org.cotato.gongmozip.domains.scheduler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.chat.enums.MessageType;
import org.cotato.gongmozip.domains.chat.service.ChatService;
import org.cotato.gongmozip.domains.chatbot.service.ChatbotOrchestrationService;
import org.cotato.gongmozip.domains.contest.service.ContestVotingService;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.domains.team.exception.TeamException;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.cotato.gongmozip.domains.team.service.LeaderElectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamScheduleServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private ContestVotingService contestVotingService;

    @Mock
    private ChatService chatService;

    @Mock
    private ChatbotOrchestrationService chatbotOrchestrationService;

    @Mock
    private LeaderElectionService leaderElectionService;

    @InjectMocks
    private TeamScheduleService teamScheduleService;

    @DisplayName("공모전 투표 마감이 지난 팀 id 목록을 조회한다.")
    @Test
    void 공모전_투표_마감이_지난_팀_id_목록을_조회한다() {
        // given
        Team team1 =
                Team.builder().teamId(1L).status(TeamStatus.CONTEST_SELECTING).build();
        Team team2 =
                Team.builder().teamId(2L).status(TeamStatus.CONTEST_SELECTING).build();
        given(teamRepository.findByStatusAndContestCandidateDeadlineAtLessThanEqual(
                        eq(TeamStatus.CONTEST_SELECTING), any()))
                .willReturn(List.of(team1, team2));

        // when
        List<Long> dueTeamIds = teamScheduleService.findDueContestVotingDeadlineTeamIds();

        // then
        assertThat(dueTeamIds).containsExactly(1L, 2L);
    }

    @DisplayName("팀 1개의 공모전 투표 마감을 처리하면 해당 팀만 개표를 호출한다.")
    @Test
    void 팀_1개의_공모전_투표_마감을_처리하면_해당_팀만_개표를_호출한다() {
        // when
        teamScheduleService.resolveContestVotingDeadlineForTeam(1L);

        // then
        verify(contestVotingService).resolveDeadlineIfDue(1L);
    }

    @DisplayName("마감 대상 팀이 없으면 빈 목록을 반환한다.")
    @Test
    void 마감_대상_팀이_없으면_빈_목록을_반환한다() {
        // given
        given(teamRepository.findByStatusAndContestCandidateDeadlineAtLessThanEqual(
                        eq(TeamStatus.CONTEST_SELECTING), any()))
                .willReturn(List.of());

        // when
        List<Long> dueTeamIds = teamScheduleService.findDueContestVotingDeadlineTeamIds();

        // then
        assertThat(dueTeamIds).isEmpty();
        verify(contestVotingService, never()).resolveDeadlineIfDue(any());
    }

    @DisplayName("공모전 투표 마감 2시간 이내로 남았고 아직 리마인더를 안 보낸 팀 id 목록을 조회한다.")
    @Test
    void 공모전_투표_마감_2시간_이내로_남은_팀_id_목록을_조회한다() {
        // given
        Team team1 = Team.builder()
                .teamId(1L)
                .status(TeamStatus.CONTEST_SELECTING)
                .contestCandidateDeadlineAt(java.time.LocalDateTime.now().plusMinutes(5))
                .build();
        given(
                        teamRepository
                                .findByStatusAndContestCandidateDeadlineAtLessThanEqualAndContestVoteReminderNotifiedAtIsNull(
                                        eq(TeamStatus.CONTEST_SELECTING), any()))
                .willReturn(List.of(team1));

        // when
        List<Long> dueTeamIds = teamScheduleService.findDueContestVoteReminderTeamIds();

        // then
        assertThat(dueTeamIds).containsExactly(1L);
    }

    @DisplayName("마감이 이미 지난 팀은 리마인더 조회 대상에서 제외된다.")
    @Test
    void 마감이_이미_지난_팀은_리마인더_조회_대상에서_제외된다() {
        // given
        Team expired = Team.builder()
                .teamId(1L)
                .status(TeamStatus.CONTEST_SELECTING)
                .contestCandidateDeadlineAt(java.time.LocalDateTime.now().minusMinutes(1))
                .build();
        given(
                        teamRepository
                                .findByStatusAndContestCandidateDeadlineAtLessThanEqualAndContestVoteReminderNotifiedAtIsNull(
                                        eq(TeamStatus.CONTEST_SELECTING), any()))
                .willReturn(List.of(expired));

        // when
        List<Long> dueTeamIds = teamScheduleService.findDueContestVoteReminderTeamIds();

        // then
        assertThat(dueTeamIds).isEmpty();
    }

    @DisplayName("마감이 이미 지난 팀에는 리마인더 카드를 발행하지 않는다.")
    @Test
    void 마감이_이미_지난_팀에는_리마인더_카드를_발행하지_않는다() {
        // given
        Team team = Team.builder()
                .teamId(1L)
                .status(TeamStatus.CONTEST_SELECTING)
                .contestCandidateDeadlineAt(java.time.LocalDateTime.now().minusMinutes(1))
                .build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when
        teamScheduleService.sendContestVoteReminderForTeam(1L);

        // then
        assertThat(team.getContestVoteReminderNotifiedAt()).isNull();
        verify(chatService, never()).postChatbotCardMessage(any(), any(), anyString(), any());
    }

    @DisplayName("공모전 투표 마감이 임박한 팀에게 리마인더 카드를 발행하고 알림 처리한다.")
    @Test
    void 공모전_투표_마감이_임박한_팀에게_리마인더_카드를_발행하고_알림_처리한다() {
        // given
        Team team = Team.builder()
                .teamId(1L)
                .status(TeamStatus.CONTEST_SELECTING)
                .contestCandidateDeadlineAt(java.time.LocalDateTime.now().plusMinutes(5))
                .build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when
        teamScheduleService.sendContestVoteReminderForTeam(1L);

        // then
        assertThat(team.getContestVoteReminderNotifiedAt()).isNotNull();
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.CONTEST_VOTE_REMINDER_CARD), anyString(), eq(null));
    }

    @DisplayName("이미 공모전 투표 리마인더를 보낸 팀은 다시 발행하지 않는다.")
    @Test
    void 이미_공모전_투표_리마인더를_보낸_팀은_다시_발행하지_않는다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.CONTEST_SELECTING).build();
        team.markContestVoteReminderNotified(java.time.LocalDateTime.now());
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when
        teamScheduleService.sendContestVoteReminderForTeam(1L);

        // then
        verify(chatService, never()).postChatbotCardMessage(any(), any(), anyString(), any());
    }

    @DisplayName("중간점검 시각이 지난 팀에게 진행률 체크 메시지를 발행하고 알림 처리한다.")
    @Test
    void 중간점검_시각이_지난_팀에게_진행률_체크_메시지를_발행하고_알림_처리한다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.IN_PROGRESS).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when
        teamScheduleService.sendProgressCheckForTeam(1L);

        // then
        assertThat(team.getProgressCheckNotifiedAt()).isNotNull();
        verify(chatService).postChatbotMessage(eq(team), anyString());
    }

    @DisplayName("이미 중간점검 알림을 보낸 팀은 다시 발행하지 않는다.")
    @Test
    void 이미_중간점검_알림을_보낸_팀은_다시_발행하지_않는다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.IN_PROGRESS).build();
        team.markProgressCheckNotified(java.time.LocalDateTime.now());
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when
        teamScheduleService.sendProgressCheckForTeam(1L);

        // then
        verify(chatService, never()).postChatbotMessage(any(), anyString());
    }

    @DisplayName("존재하지 않는 팀의 중간점검을 처리하려 하면 예외가 발생한다.")
    @Test
    void 존재하지_않는_팀의_중간점검을_처리하려_하면_예외가_발생한다() {
        // given
        given(teamRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> teamScheduleService.sendProgressCheckForTeam(999L))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.TEAM_NOT_FOUND);
    }

    @DisplayName("제출확인 시각이 지난 팀에게 제출 여부 확인 카드를 발행하고 알림 처리한다.")
    @Test
    void 제출확인_시각이_지난_팀에게_제출_여부_확인_카드를_발행하고_알림_처리한다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.IN_PROGRESS).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when
        teamScheduleService.sendSubmissionCheckForTeam(1L);

        // then
        assertThat(team.getSubmissionCheckNotifiedAt()).isNotNull();
        assertThat(team.getSubmissionCheckReminderAt())
                .isAfter(java.time.LocalDateTime.now().plusHours(1));
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.SUBMISSION_CHECK_CARD), anyString(), eq(null));
    }

    @DisplayName("이미 제출확인 알림을 보낸 팀은 다시 발행하지 않는다.")
    @Test
    void 이미_제출확인_알림을_보낸_팀은_다시_발행하지_않는다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.IN_PROGRESS).build();
        team.markSubmissionCheckNotified(java.time.LocalDateTime.now());
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when
        teamScheduleService.sendSubmissionCheckForTeam(1L);

        // then
        verify(chatService, never()).postChatbotCardMessage(any(), any(), anyString(), any());
    }

    @DisplayName("제출 여부 재알림 시각이 지난 팀 id 목록을 조회한다.")
    @Test
    void 제출_여부_재알림_시각이_지난_팀_id_목록을_조회한다() {
        // given
        Team team1 = Team.builder().teamId(1L).status(TeamStatus.IN_PROGRESS).build();
        given(teamRepository.findByStatusAndSubmissionCheckReminderAtLessThanEqual(eq(TeamStatus.IN_PROGRESS), any()))
                .willReturn(List.of(team1));

        // when
        List<Long> dueTeamIds = teamScheduleService.findDueSubmissionCheckReminderTeamIds();

        // then
        assertThat(dueTeamIds).containsExactly(1L);
    }

    @DisplayName("진행 완료 응답이 없는 팀에게 재알림 카드를 발행하고 다음 재알림 시각을 다시 미룬다.")
    @Test
    void 진행_완료_응답이_없는_팀에게_재알림_카드를_발행하고_다음_재알림_시각을_다시_미룬다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.IN_PROGRESS).build();
        team.scheduleSubmissionCheckReminder(java.time.LocalDateTime.now().minusMinutes(1));
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when
        teamScheduleService.sendSubmissionCheckReminderForTeam(1L);

        // then
        assertThat(team.getSubmissionCheckReminderAt())
                .isAfter(java.time.LocalDateTime.now().plusHours(1));
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.SUBMISSION_CHECK_CARD), anyString(), eq(null));
    }

    @DisplayName("재알림 시각이 아직 미래로 재예약돼 있으면(예: 방금 미완료 응답) 카드를 발행하지 않는다.")
    @Test
    void 재알림_시각이_아직_미래이면_카드를_발행하지_않는다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.IN_PROGRESS).build();
        team.scheduleSubmissionCheckReminder(java.time.LocalDateTime.now().plusHours(2));
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when
        teamScheduleService.sendSubmissionCheckReminderForTeam(1L);

        // then
        verify(chatService, never()).postChatbotCardMessage(any(), any(), anyString(), any());
    }

    @DisplayName("이미 진행 완료로 응답해 SUBMITTED가 된 팀은 재알림하지 않는다.")
    @Test
    void 이미_진행_완료로_응답해_SUBMITTED가_된_팀은_재알림하지_않는다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.SUBMITTED).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when
        teamScheduleService.sendSubmissionCheckReminderForTeam(1L);

        // then
        verify(chatService, never()).postChatbotCardMessage(any(), any(), anyString(), any());
    }

    @DisplayName("인사 유도 타임아웃이 지난 팀 id 목록을 조회한다.")
    @Test
    void 인사_유도_타임아웃이_지난_팀_id_목록을_조회한다() {
        // given
        Team team1 = Team.builder().teamId(1L).status(TeamStatus.GREETING).build();
        given(teamRepository.findByStatusAndCreatedAtLessThanEqual(eq(TeamStatus.GREETING), any()))
                .willReturn(List.of(team1));

        // when
        List<Long> dueTeamIds = teamScheduleService.findDueGreetingTimeoutTeamIds();

        // then
        assertThat(dueTeamIds).containsExactly(1L);
    }

    @DisplayName("팀 1개의 인사 유도 타임아웃을 처리하면 해당 팀의 강제 전이만 호출한다.")
    @Test
    void 팀_1개의_인사_유도_타임아웃을_처리하면_해당_팀의_강제_전이만_호출한다() {
        // when
        teamScheduleService.forceAdvanceGreetingForTeam(1L);

        // then
        verify(chatbotOrchestrationService).forceAdvanceGreetingIfDue(1L);
    }

    @DisplayName("팀장 후보 등록 마감이 지난 팀 id 목록을 조회한다.")
    @Test
    void 팀장_후보_등록_마감이_지난_팀_id_목록을_조회한다() {
        // given
        Team team1 =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        given(teamRepository.findByStatusAndLeaderCandidacyDeadlineAtLessThanEqual(
                        eq(TeamStatus.LEADER_SELECTING), any()))
                .willReturn(List.of(team1));

        // when
        List<Long> dueTeamIds = teamScheduleService.findDueLeaderCandidacyDeadlineTeamIds();

        // then
        assertThat(dueTeamIds).containsExactly(1L);
    }

    @DisplayName("팀 1개의 팀장 후보 등록 마감을 처리하면 해당 팀만 마감 처리를 호출한다.")
    @Test
    void 팀_1개의_팀장_후보_등록_마감을_처리하면_해당_팀만_마감_처리를_호출한다() {
        // when
        teamScheduleService.resolveLeaderCandidacyDeadlineForTeam(1L);

        // then
        verify(leaderElectionService).resolveCandidacyDeadlineIfDue(1L);
    }

    @DisplayName("팀장 투표 마감이 지난 팀 id 목록을 조회한다.")
    @Test
    void 팀장_투표_마감이_지난_팀_id_목록을_조회한다() {
        // given
        Team team1 =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        given(teamRepository.findByStatusAndLeaderVoteDeadlineAtLessThanEqual(eq(TeamStatus.LEADER_SELECTING), any()))
                .willReturn(List.of(team1));

        // when
        List<Long> dueTeamIds = teamScheduleService.findDueLeaderVoteDeadlineTeamIds();

        // then
        assertThat(dueTeamIds).containsExactly(1L);
    }

    @DisplayName("팀 1개의 팀장 투표 마감을 처리하면 해당 팀만 마감 처리를 호출한다.")
    @Test
    void 팀_1개의_팀장_투표_마감을_처리하면_해당_팀만_마감_처리를_호출한다() {
        // when
        teamScheduleService.resolveLeaderVoteDeadlineForTeam(1L);

        // then
        verify(leaderElectionService).resolveVoteDeadlineIfDue(1L);
    }

    @DisplayName("팀장 투표 마감 30분 이내로 남았고 아직 리마인더를 안 보낸 팀 id 목록을 조회한다.")
    @Test
    void 팀장_투표_마감_30분_이내로_남은_팀_id_목록을_조회한다() {
        // given
        Team team1 = Team.builder()
                .teamId(1L)
                .status(TeamStatus.LEADER_SELECTING)
                .leaderVoteDeadlineAt(java.time.LocalDateTime.now().plusMinutes(10))
                .build();
        given(teamRepository.findByStatusAndLeaderVoteDeadlineAtLessThanEqualAndLeaderVoteReminderNotifiedAtIsNull(
                        eq(TeamStatus.LEADER_SELECTING), any()))
                .willReturn(List.of(team1));

        // when
        List<Long> dueTeamIds = teamScheduleService.findDueLeaderVoteReminderTeamIds();

        // then
        assertThat(dueTeamIds).containsExactly(1L);
    }

    @DisplayName("마감이 이미 지난 팀은 팀장 투표 리마인더 조회 대상에서 제외된다.")
    @Test
    void 마감이_이미_지난_팀은_팀장_투표_리마인더_조회_대상에서_제외된다() {
        // given
        Team expired = Team.builder()
                .teamId(1L)
                .status(TeamStatus.LEADER_SELECTING)
                .leaderVoteDeadlineAt(java.time.LocalDateTime.now().minusMinutes(1))
                .build();
        given(teamRepository.findByStatusAndLeaderVoteDeadlineAtLessThanEqualAndLeaderVoteReminderNotifiedAtIsNull(
                        eq(TeamStatus.LEADER_SELECTING), any()))
                .willReturn(List.of(expired));

        // when
        List<Long> dueTeamIds = teamScheduleService.findDueLeaderVoteReminderTeamIds();

        // then
        assertThat(dueTeamIds).isEmpty();
    }

    @DisplayName("마감이 이미 지난 팀에는 팀장 투표 리마인더 카드를 발행하지 않는다.")
    @Test
    void 마감이_이미_지난_팀에는_팀장_투표_리마인더_카드를_발행하지_않는다() {
        // given
        Team team = Team.builder()
                .teamId(1L)
                .status(TeamStatus.LEADER_SELECTING)
                .leaderVoteDeadlineAt(java.time.LocalDateTime.now().minusMinutes(1))
                .build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when
        teamScheduleService.sendLeaderVoteReminderForTeam(1L);

        // then
        assertThat(team.getLeaderVoteReminderNotifiedAt()).isNull();
        verify(chatService, never()).postChatbotCardMessage(any(), any(), anyString(), any());
    }

    @DisplayName("팀장 투표 마감이 임박한 팀에게 리마인더 카드를 발행하고 알림 처리한다.")
    @Test
    void 팀장_투표_마감이_임박한_팀에게_리마인더_카드를_발행하고_알림_처리한다() {
        // given
        Team team = Team.builder()
                .teamId(1L)
                .status(TeamStatus.LEADER_SELECTING)
                .leaderVoteDeadlineAt(java.time.LocalDateTime.now().plusMinutes(10))
                .build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when
        teamScheduleService.sendLeaderVoteReminderForTeam(1L);

        // then
        assertThat(team.getLeaderVoteReminderNotifiedAt()).isNotNull();
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.LEADER_VOTE_REMINDER_CARD), anyString(), eq(null));
    }

    @DisplayName("이미 팀장 투표 리마인더를 보낸 팀은 다시 발행하지 않는다.")
    @Test
    void 이미_팀장_투표_리마인더를_보낸_팀은_다시_발행하지_않는다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        team.markLeaderVoteReminderNotified(java.time.LocalDateTime.now());
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when
        teamScheduleService.sendLeaderVoteReminderForTeam(1L);

        // then
        verify(chatService, never()).postChatbotCardMessage(any(), any(), anyString(), any());
    }
}
