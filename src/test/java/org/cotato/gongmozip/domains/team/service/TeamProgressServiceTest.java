package org.cotato.gongmozip.domains.team.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.chat.service.ChatService;
import org.cotato.gongmozip.domains.collaboration.enums.CollaborationPointReason;
import org.cotato.gongmozip.domains.collaboration.service.CollaborationPointService;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.enums.TeamRole;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.domains.team.exception.TeamException;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamProgressServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private ChatService chatService;

    @Mock
    private CollaborationPointService collaborationPointService;

    @InjectMocks
    private TeamProgressService teamProgressService;

    @DisplayName("IN_PROGRESS 상태가 아니면 진행률 응답에 실패한다.")
    @Test
    void IN_PROGRESS_상태가_아니면_진행률_응답에_실패한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.CONTEST_SELECTING).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when & then
        assertThatThrownBy(() -> teamProgressService.updateProgress(1L, 10L, 50))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.INVALID_TEAM_STATUS);
    }

    @DisplayName("팀장이 아니면 진행률 응답에 실패한다.")
    @Test
    void 팀장이_아니면_진행률_응답에_실패한다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.IN_PROGRESS).build();
        TeamMember notLeader = teamMemberOf(team, 10L, "김철수", TeamRole.MEMBER);
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(notLeader));

        // when & then
        assertThatThrownBy(() -> teamProgressService.updateProgress(1L, 10L, 50))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.NOT_TEAM_LEADER);
    }

    @DisplayName("팀장이 처음 진행률에 응답하면 협업거리 포인트가 적립된다.")
    @Test
    void 팀장이_처음_진행률에_응답하면_협업거리_포인트가_적립된다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.IN_PROGRESS).build();
        TeamMember leader = teamMemberOf(team, 10L, "김철수", TeamRole.LEADER);
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(leader));

        // when
        teamProgressService.updateProgress(1L, 10L, 60);

        // then
        assertThat(team.getProgressPercent()).isEqualTo(60);
        assertThat(team.getProgressCheckRespondedAt()).isNotNull();
        verify(collaborationPointService)
                .awardPoint(leader.getMember(), team, CollaborationPointReason.PROGRESS_CHECK_RESPONSE);
        verify(chatService).postSystemMessage(eq(team), anyString());
    }

    @DisplayName("두 번째 진행률 응답부터는 포인트가 중복 적립되지 않는다.")
    @Test
    void 두_번째_진행률_응답부터는_포인트가_중복_적립되지_않는다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.IN_PROGRESS).build();
        TeamMember leader = teamMemberOf(team, 10L, "김철수", TeamRole.LEADER);
        team.recordProgress(30, LocalDateTime.now().minusHours(1));
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(leader));

        // when
        teamProgressService.updateProgress(1L, 10L, 80);

        // then
        assertThat(team.getProgressPercent()).isEqualTo(80);
        verify(collaborationPointService, never()).awardPoint(any(), any(), any());
    }

    @DisplayName("제출 완료로 응답하면 팀이 SUBMITTED로 바뀌고 팀원 전원에게 완주 포인트가 지급된다.")
    @Test
    void 제출_완료로_응답하면_팀이_SUBMITTED로_바뀌고_팀원_전원에게_완주_포인트가_지급된다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.IN_PROGRESS).build();
        TeamMember leader = teamMemberOf(team, 10L, "김철수", TeamRole.LEADER);
        TeamMember member = teamMemberOf(team, 20L, "이해은", TeamRole.MEMBER);
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(leader));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(leader, member));

        // when
        teamProgressService.submitCompletion(1L, 10L, true);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.SUBMITTED);
        assertThat(team.isSubmitted()).isTrue();
        verify(collaborationPointService)
                .awardPoint(leader.getMember(), team, CollaborationPointReason.PROJECT_COMPLETE_LEADER);
        verify(collaborationPointService)
                .awardPoint(member.getMember(), team, CollaborationPointReason.PROJECT_COMPLETE_MEMBER);
    }

    @DisplayName("미완료로 응답하면 상태 변화나 포인트 지급 없이 안내만 남고, 2시간 뒤 재알림이 예약된다.")
    @Test
    void 미완료로_응답하면_상태_변화나_포인트_지급_없이_안내만_남는다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.IN_PROGRESS).build();
        TeamMember leader = teamMemberOf(team, 10L, "김철수", TeamRole.LEADER);
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(leader));

        // when
        teamProgressService.submitCompletion(1L, 10L, false);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.IN_PROGRESS);
        assertThat(team.isSubmitted()).isFalse();
        assertThat(team.getSubmissionCheckReminderAt())
                .isAfter(LocalDateTime.now().plusHours(1));
        verify(collaborationPointService, never()).awardPoint(any(), any(), any());
        verify(chatService, times(1)).postSystemMessage(eq(team), anyString());
    }

    private TeamMember teamMemberOf(Team team, Long memberId, String nickname, TeamRole role) {
        Member member = Member.builder().memberId(memberId).build();
        Profile profile = Profile.builder().nickname(nickname).build();
        return TeamMember.builder()
                .teamMemberId(memberId)
                .team(team)
                .member(member)
                .profile(profile)
                .role(role)
                .status(TeamMemberStatus.ACTIVE)
                .build();
    }
}
