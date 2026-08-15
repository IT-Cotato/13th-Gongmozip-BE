package org.cotato.gongmozip.domains.team.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeaderTiebreakTxServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private ChatService chatService;

    @Mock
    private ChatbotOrchestrationService chatbotOrchestrationService;

    @InjectMocks
    private LeaderTiebreakTxService txService;

    @DisplayName("1라운드 동률에서 AI 추천이 있으면, 추천 문구를 담아 투표 카드를 다시 발행하고 투표 마감을 새로 세팅한다.")
    @Test
    void 라운드1_동률_AI추천_있으면_추천_문구를_담아_카드를_발행한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        TeamMember tied1 = teamMemberOf(team, 10L, "김철수");
        TeamMember tied2 = teamMemberOf(team, 20L, "이해은");
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(tied1, tied2));

        // when
        txService.applyTiebreakResult(1L, 1, List.of(10L, 20L), 10L);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_SELECTING);
        assertThat(team.getLeaderVoteDeadlineAt()).isNotNull();
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> metadataCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatService)
                .postChatbotCardMessage(
                        eq(team), eq(MessageType.LEADER_VOTE_CARD), contentCaptor.capture(), metadataCaptor.capture());
        assertThat(contentCaptor.getValue()).contains("김철수");
        assertThat(metadataCaptor.getValue()).contains("10").contains("20").contains("aiRecommendedTeamMemberId");
        verify(chatbotOrchestrationService, never()).advanceToContestSelecting(any());
    }

    @DisplayName("1라운드 동률에서 AI 추천이 없으면(null), 기본 문구로 투표 카드를 다시 발행한다.")
    @Test
    void 라운드1_동률_AI추천_없으면_기본_문구로_카드를_발행한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        TeamMember tied1 = teamMemberOf(team, 10L, "김철수");
        TeamMember tied2 = teamMemberOf(team, 20L, "이해은");
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(tied1, tied2));

        // when
        txService.applyTiebreakResult(1L, 1, List.of(10L, 20L), null);

        // then
        assertThat(team.getLeaderVoteDeadlineAt()).isNotNull();
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatService)
                .postChatbotCardMessage(
                        eq(team), eq(MessageType.LEADER_VOTE_CARD), contentCaptor.capture(), anyString());
        assertThat(contentCaptor.getValue()).doesNotContain("AI가 보기엔");
    }

    @DisplayName("2라운드(재투표) 동률에서 AI 추천이 있으면, 재투표 없이 바로 팀장으로 확정한다.")
    @Test
    void 라운드2_동률_AI추천_있으면_바로_확정한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        TeamMember tied1 = teamMemberOf(team, 10L, "김철수");
        TeamMember tied2 = teamMemberOf(team, 20L, "이해은");
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(tied1, tied2));

        // when
        txService.applyTiebreakResult(1L, 2, List.of(10L, 20L), 10L);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_DECIDED);
        assertThat(tied1.getRole()).isEqualTo(TeamRole.LEADER);
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.LEADER_RESULT_CARD), anyString(), anyString());
        verify(chatService, never())
                .postChatbotCardMessage(any(), eq(MessageType.LEADER_VOTE_CARD), anyString(), anyString());
        verify(chatbotOrchestrationService).advanceToContestSelecting(team);
    }

    @DisplayName("2라운드(재투표) 동률에서도 AI 추천이 없으면(null), 확정하지 않고 다시 투표 카드를 발행한다.")
    @Test
    void 라운드2_동률_AI추천_없으면_다시_카드를_발행한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        TeamMember tied1 = teamMemberOf(team, 10L, "김철수");
        TeamMember tied2 = teamMemberOf(team, 20L, "이해은");
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(tied1, tied2));

        // when
        txService.applyTiebreakResult(1L, 2, List.of(10L, 20L), null);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_SELECTING);
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.LEADER_VOTE_CARD), anyString(), anyString());
        verify(chatService, never())
                .postChatbotCardMessage(any(), eq(MessageType.LEADER_RESULT_CARD), anyString(), anyString());
    }

    @DisplayName("비동기로 넘어오는 사이 팀이 이미 LEADER_SELECTING을 벗어났으면 아무 것도 하지 않는다.")
    @Test
    void 팀이_이미_다른_상태로_바뀌었으면_아무것도_하지_않는다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.LEADER_DECIDED).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when
        txService.applyTiebreakResult(1L, 1, List.of(10L, 20L), 10L);

        // then
        verify(teamMemberRepository, never()).findByTeamIdAndStatus(any(), any());
        verify(chatService, never()).postChatbotCardMessage(any(), any(), anyString(), any());
    }

    private TeamMember teamMemberOf(Team team, Long memberId, String nickname) {
        Member member = Member.builder().memberId(memberId).build();
        Profile profile = Profile.builder().nickname(nickname).build();
        return TeamMember.builder()
                .teamMemberId(memberId)
                .team(team)
                .member(member)
                .profile(profile)
                .status(TeamMemberStatus.ACTIVE)
                .build();
    }
}
