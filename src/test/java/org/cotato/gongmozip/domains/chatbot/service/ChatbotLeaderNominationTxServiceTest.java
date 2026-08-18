package org.cotato.gongmozip.domains.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.chat.enums.MessageType;
import org.cotato.gongmozip.domains.chat.service.ChatService;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.domains.team.exception.TeamException;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
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
class ChatbotLeaderNominationTxServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatbotLeaderNominationTxService txService;

    @DisplayName("추천된 팀원 이름을 문구에 포함해 카드를 발행한다.")
    @Test
    void 추천된_팀원_이름을_포함해_카드를_발행한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        TeamMember member = teamMemberOf(team, 10L, "김민정");
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(member));

        // when
        txService.announceLeaderNomination(1L, List.of(10L));

        // then
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatService)
                .postChatbotCardMessage(
                        eq(team), eq(MessageType.LEADER_NOMINATION_CARD), contentCaptor.capture(), anyString());
        assertThat(contentCaptor.getValue()).contains("김민정");
    }

    @DisplayName("추천이 2명이면 Figma 5.1.3.2 문구 그대로 '혹은'으로 이어붙인다.")
    @Test
    void 추천이_2명이면_혹은으로_이어붙인다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        TeamMember member1 = teamMemberOf(team, 10L, "김민정");
        TeamMember member2 = teamMemberOf(team, 20L, "이해은");
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(member1, member2));

        // when
        txService.announceLeaderNomination(1L, List.of(10L, 20L));

        // then
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatService)
                .postChatbotCardMessage(
                        eq(team), eq(MessageType.LEADER_NOMINATION_CARD), contentCaptor.capture(), anyString());
        assertThat(contentCaptor.getValue())
                .isEqualTo("이제, 팀장을 선출해볼게요. 매칭 전에 팀장을 지원해주신 분이 없으셔서 사용자 프로필 및 협업 유형"
                        + " 검사 결과 김민정님 혹은 이해은님이 팀장을 잘하실 수 있을 거라 추천드립니다. 다른 분들도 모두"
                        + " 팀장을 하기 충분한 자질을 가지신 분들이니, 팀장 여부를 모두 투표해주세요.");
    }

    @DisplayName("추천 결과가 없으면 이름 없이 기본 문구로 카드를 발행한다.")
    @Test
    void 추천_결과가_없으면_기본_문구로_카드를_발행한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of());

        // when
        txService.announceLeaderNomination(1L, List.of());

        // then
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatService)
                .postChatbotCardMessage(
                        eq(team), eq(MessageType.LEADER_NOMINATION_CARD), contentCaptor.capture(), anyString());
        assertThat(contentCaptor.getValue()).doesNotContain("AI 추천");
    }

    @DisplayName("존재하지 않는 팀이면 예외가 발생한다.")
    @Test
    void 존재하지_않는_팀이면_예외가_발생한다() {
        // given
        given(teamRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> txService.announceLeaderNomination(999L, List.of()))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.TEAM_NOT_FOUND);
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
