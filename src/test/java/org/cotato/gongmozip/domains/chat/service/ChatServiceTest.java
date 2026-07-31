package org.cotato.gongmozip.domains.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.cotato.gongmozip.domains.chat.dto.request.ChatRequest.SendMessageRequest;
import org.cotato.gongmozip.domains.chat.dto.response.ChatResponse.MessageItemResponse;
import org.cotato.gongmozip.domains.chat.entity.Message;
import org.cotato.gongmozip.domains.chat.repository.MessageRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatService chatService;

    @DisplayName("팀 소속 회원이 메시지를 보내면 메시지가 저장되고 실시간으로 브로드캐스트된다.")
    @Test
    void 팀_소속_회원이_메시지를_보내면_메시지가_저장되고_실시간으로_브로드캐스트된다() {
        // given
        Team team = Team.builder().teamId(100L).build();
        TeamMember sender = teamMemberOf(team, 1L, "김철수");
        SendMessageRequest request = new SendMessageRequest("안녕하세요.");

        given(teamRepository.findById(100L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(100L, 1L))
                .willReturn(Optional.of(sender));
        given(messageRepository.save(any(Message.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        MessageItemResponse response = chatService.sendMessage(100L, 1L, request);

        // then
        assertThat(response.content()).isEqualTo("안녕하세요.");
        assertThat(response.senderType()).isEqualTo("MEMBER");
        verify(messagingTemplate).convertAndSend(eq("/topic/teams/100"), any(MessageItemResponse.class));
    }

    @DisplayName("존재하지 않는 팀에 메시지를 보내면 예외가 발생한다.")
    @Test
    void 존재하지_않는_팀에_메시지를_보내면_예외가_발생한다() {
        // given
        given(teamRepository.findById(999L)).willReturn(Optional.empty());
        SendMessageRequest request = new SendMessageRequest("안녕하세요.");

        // when & then
        assertThatThrownBy(() -> chatService.sendMessage(999L, 1L, request))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.TEAM_NOT_FOUND);
    }

    @DisplayName("팀 소속이 아닌 회원이 메시지를 보내면 예외가 발생한다.")
    @Test
    void 팀_소속이_아닌_회원이_메시지를_보내면_예외가_발생한다() {
        // given
        Team team = Team.builder().teamId(100L).build();
        given(teamRepository.findById(100L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(100L, 999L))
                .willReturn(Optional.empty());
        SendMessageRequest request = new SendMessageRequest("안녕하세요.");

        // when & then
        assertThatThrownBy(() -> chatService.sendMessage(100L, 999L, request))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.NOT_TEAM_MEMBER);
    }

    @DisplayName("읽음 처리를 하면 lastReadAt이 갱신된다.")
    @Test
    void 읽음_처리를_하면_lastReadAt이_갱신된다() {
        // given
        Team team = Team.builder().teamId(100L).build();
        TeamMember me = teamMemberOf(team, 1L, "김철수");
        given(teamRepository.findById(100L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(100L, 1L))
                .willReturn(Optional.of(me));

        // when
        chatService.markAsRead(100L, 1L);

        // then
        assertThat(me.getLastReadAt()).isNotNull();
    }

    private TeamMember teamMemberOf(Team team, Long memberId, String nickname) {
        Member member = Member.builder().memberId(memberId).build();
        Profile profile = Profile.builder().nickname(nickname).build();
        return TeamMember.builder()
                .team(team)
                .member(member)
                .profile(profile)
                .status(TeamMemberStatus.ACTIVE)
                .build();
    }
}
