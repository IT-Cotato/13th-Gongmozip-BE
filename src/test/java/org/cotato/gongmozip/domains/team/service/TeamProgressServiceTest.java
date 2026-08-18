package org.cotato.gongmozip.domains.team.service;

import static org.assertj.core.api.Assertions.assertThat;
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
        LocalDateTime before = LocalDateTime.now();
        teamProgressService.submitCompletion(1L, 10L, false);
        LocalDateTime after = LocalDateTime.now();

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.IN_PROGRESS);
        assertThat(team.isSubmitted()).isFalse();
        assertThat(team.getSubmissionCheckReminderAt()).isBetween(before.plusHours(2), after.plusHours(2));
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
