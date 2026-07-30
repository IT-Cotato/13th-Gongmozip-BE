package org.cotato.gongmozip.domains.scheduler.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.cotato.gongmozip.domains.chat.enums.MessageType;
import org.cotato.gongmozip.domains.chat.service.ChatService;
import org.cotato.gongmozip.domains.contest.service.ContestVotingService;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
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

    @InjectMocks
    private TeamScheduleService teamScheduleService;

    @DisplayName("공모전 투표 마감이 지난 팀들을 각각 강제 개표한다.")
    @Test
    void 공모전_투표_마감이_지난_팀들을_각각_강제_개표한다() {
        // given
        Team team1 =
                Team.builder().teamId(1L).status(TeamStatus.CONTEST_SELECTING).build();
        Team team2 =
                Team.builder().teamId(2L).status(TeamStatus.CONTEST_SELECTING).build();
        given(teamRepository.findByStatusAndContestCandidateDeadlineAtLessThanEqual(
                        eq(TeamStatus.CONTEST_SELECTING), any()))
                .willReturn(List.of(team1, team2));

        // when
        teamScheduleService.resolveDueContestVotingDeadlines();

        // then
        verify(contestVotingService).resolveDeadlineIfDue(1L);
        verify(contestVotingService).resolveDeadlineIfDue(2L);
    }

    @DisplayName("중간점검 시각이 지난 팀에게 진행률 체크 카드를 발행하고 알림 처리한다.")
    @Test
    void 중간점검_시각이_지난_팀에게_진행률_체크_카드를_발행하고_알림_처리한다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.IN_PROGRESS).build();
        given(teamRepository.findByStatusAndProgressCheckAtLessThanEqualAndProgressCheckNotifiedAtIsNull(
                        eq(TeamStatus.IN_PROGRESS), any()))
                .willReturn(List.of(team));

        // when
        teamScheduleService.sendDueProgressChecks();

        // then
        org.assertj.core.api.Assertions.assertThat(team.getProgressCheckNotifiedAt())
                .isNotNull();
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.PROGRESS_CHECK_CARD), anyString(), eq(null));
    }

    @DisplayName("제출확인 시각이 지난 팀에게 제출 여부 확인 카드를 발행하고 알림 처리한다.")
    @Test
    void 제출확인_시각이_지난_팀에게_제출_여부_확인_카드를_발행하고_알림_처리한다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.IN_PROGRESS).build();
        given(teamRepository.findByStatusAndSubmissionCheckAtLessThanEqualAndSubmissionCheckNotifiedAtIsNull(
                        eq(TeamStatus.IN_PROGRESS), any()))
                .willReturn(List.of(team));

        // when
        teamScheduleService.sendDueSubmissionChecks();

        // then
        org.assertj.core.api.Assertions.assertThat(team.getSubmissionCheckNotifiedAt())
                .isNotNull();
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.SUBMISSION_CHECK_CARD), anyString(), eq(null));
    }

    @DisplayName("마감 대상 팀이 없으면 아무 것도 하지 않는다.")
    @Test
    void 마감_대상_팀이_없으면_아무_것도_하지_않는다() {
        // given
        given(teamRepository.findByStatusAndContestCandidateDeadlineAtLessThanEqual(
                        eq(TeamStatus.CONTEST_SELECTING), any()))
                .willReturn(List.of());

        // when
        teamScheduleService.resolveDueContestVotingDeadlines();

        // then
        verify(contestVotingService, times(0)).resolveDeadlineIfDue(any());
    }
}
