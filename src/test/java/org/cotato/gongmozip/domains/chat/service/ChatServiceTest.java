package org.cotato.gongmozip.domains.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.MemberAvatarResponse;
import org.cotato.gongmozip.domains.character.enums.CharacterPalette;
import org.cotato.gongmozip.domains.character.service.CharacterService;
import org.cotato.gongmozip.domains.chat.dto.request.ChatRequest.SendMessageRequest;
import org.cotato.gongmozip.domains.chat.dto.response.ChatResponse.MessageItemResponse;
import org.cotato.gongmozip.domains.chat.dto.response.ChatResponse.MessageListResponse;
import org.cotato.gongmozip.domains.chat.dto.response.ChatResponse.MessageUnreadUpdateResponse;
import org.cotato.gongmozip.domains.chat.entity.Message;
import org.cotato.gongmozip.domains.chat.enums.MessageSenderType;
import org.cotato.gongmozip.domains.chat.enums.MessageType;
import org.cotato.gongmozip.domains.chat.repository.MessageRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Mock
    private CharacterService characterService;

    @InjectMocks
    private ChatService chatService;

    @DisplayName("팀 소속 회원이 메시지를 보내면 메시지가 저장되고 실시간으로 브로드캐스트된다.")
    @Test
    void 팀_소속_회원이_메시지를_보내면_메시지가_저장되고_실시간으로_브로드캐스트된다() {
        // given
        Team team = Team.builder().teamId(100L).build();
        TeamMember sender = teamMemberOf(team, 1L, "김철수");
        TeamMember other = teamMemberOf(team, 2L, "이해은");
        SendMessageRequest request = new SendMessageRequest("안녕하세요.");
        MemberAvatarResponse avatar =
                new MemberAvatarResponse(1L, CharacterType.LEAD_RUNNER, CharacterPalette.DEFAULT, null, null);

        given(teamRepository.findById(100L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(100L, 1L))
                .willReturn(Optional.of(sender));
        given(teamMemberRepository.findByTeamIdAndStatus(100L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(sender, other));
        given(messageRepository.save(any(Message.class))).willAnswer(inv -> {
            Message message = inv.getArgument(0);
            ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.now());
            return message;
        });
        given(characterService.findAvatarsByMembers(List.of(sender.getMember())))
                .willReturn(Map.of(1L, avatar));

        // when
        MessageItemResponse response = chatService.sendMessage(100L, 1L, request);

        // then
        assertThat(response.content()).isEqualTo("안녕하세요.");
        assertThat(response.senderType()).isEqualTo("MEMBER");
        assertThat(response.senderAvatar()).isEqualTo(avatar);
        assertThat(response.unreadCount()).isEqualTo(1); // 보낸 사람 본인 제외, 나머지 1명(other)만 안읽음
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

    @DisplayName("메시지 목록을 조회하면 발신 팀원의 아바타가 함께 채워진다.")
    @Test
    void 메시지_목록을_조회하면_발신_팀원의_아바타가_함께_채워진다() {
        // given
        Team team = Team.builder().teamId(100L).build();
        TeamMember me = teamMemberOf(team, 1L, "김철수");
        TeamMember other = teamMemberOf(team, 2L, "이해은");
        Message memberMessage = Message.builder()
                .team(team)
                .senderType(MessageSenderType.MEMBER)
                .senderTeamMember(other)
                .messageType(MessageType.TEXT)
                .content("안녕하세요.")
                .build();
        Message chatbotMessage = Message.builder()
                .team(team)
                .senderType(MessageSenderType.CHATBOT)
                .messageType(MessageType.TEXT)
                .content("반가워요!")
                .build();
        ReflectionTestUtils.setField(
                chatbotMessage, "createdAt", java.time.LocalDateTime.now().minusMinutes(1));
        ReflectionTestUtils.setField(memberMessage, "createdAt", java.time.LocalDateTime.now());
        MemberAvatarResponse otherAvatar =
                new MemberAvatarResponse(2L, CharacterType.TRACK_RUNNER, CharacterPalette.DEFAULT, null, null);

        given(teamRepository.findById(100L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(100L, 1L))
                .willReturn(Optional.of(me));
        given(teamMemberRepository.findByTeamIdAndStatus(100L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(me, other));
        given(messageRepository.findByTeamIdBeforeCursor(any(Long.class), isNull(), any()))
                .willReturn(List.of(chatbotMessage, memberMessage));
        given(characterService.findAvatarsByMembers(List.of(other.getMember()))).willReturn(Map.of(2L, otherAvatar));

        // when
        MessageListResponse response = chatService.getMessages(100L, 1L, null);

        // then
        assertThat(response.hasNext()).isFalse();
        assertThat(response.messages())
                .filteredOn(m -> "MEMBER".equals(m.senderType()))
                .singleElement()
                .satisfies(m -> {
                    assertThat(m.senderAvatar()).isEqualTo(otherAvatar);
                    assertThat(m.unreadCount()).isEqualTo(1); // 보낸 other 제외, me만 안읽음
                });
        assertThat(response.messages())
                .filteredOn(m -> "CHATBOT".equals(m.senderType()))
                .singleElement()
                .satisfies(m -> {
                    assertThat(m.senderAvatar()).isNull();
                    assertThat(m.unreadCount()).isEqualTo(2); // 발신자 없음, me/other 모두 안읽음
                });
    }

    @DisplayName("cursor로 조회했을 때 페이지 크기보다 많은 메시지가 남아있으면 hasNext가 true이고 페이지 크기만큼만 반환된다.")
    @Test
    void cursor로_조회했을_때_페이지_크기보다_많은_메시지가_남아있으면_hasNext가_true이다() {
        // given
        Team team = Team.builder().teamId(100L).build();
        TeamMember me = teamMemberOf(team, 1L, "김철수");
        List<Message> latestFirstOverPageSize = new java.util.ArrayList<>();
        for (int i = 0; i < 51; i++) {
            Message message = Message.builder()
                    .team(team)
                    .senderType(MessageSenderType.SYSTEM)
                    .messageType(MessageType.SYSTEM_NOTICE)
                    .content("메시지" + i)
                    .build();
            ReflectionTestUtils.setField(
                    message, "createdAt", LocalDateTime.now().minusMinutes(i));
            latestFirstOverPageSize.add(message);
        }

        given(teamRepository.findById(100L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(100L, 1L))
                .willReturn(Optional.of(me));
        given(teamMemberRepository.findByTeamIdAndStatus(100L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(me));
        given(messageRepository.findByTeamIdBeforeCursor(eq(100L), eq(200L), any()))
                .willReturn(latestFirstOverPageSize);

        // when
        MessageListResponse response = chatService.getMessages(100L, 1L, 200L);

        // then
        assertThat(response.hasNext()).isTrue();
        assertThat(response.messages()).hasSize(50);
    }

    @DisplayName("챗봇이 꺼져있으면 챗봇 메시지를 남기지 않는다.")
    @Test
    void 챗봇이_꺼져있으면_챗봇_메시지를_남기지_않는다() {
        // given
        Team team = Team.builder().teamId(100L).chatbotEnabled(false).build();

        // when
        chatService.postChatbotMessage(team, "안녕하세요!");
        chatService.postChatbotCardMessage(team, MessageType.LEADER_VOTE_CARD, "투표해주세요", null);

        // then
        verify(messageRepository, never()).save(any(Message.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(MessageItemResponse.class));
    }

    @DisplayName("챗봇이 켜져있으면 챗봇 메시지가 저장되고 브로드캐스트된다.")
    @Test
    void 챗봇이_켜져있으면_챗봇_메시지가_저장되고_브로드캐스트된다() {
        // given
        Team team = Team.builder().teamId(100L).chatbotEnabled(true).build();
        given(messageRepository.save(any(Message.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        chatService.postChatbotMessage(team, "안녕하세요!");

        // then
        verify(messageRepository).save(any(Message.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/teams/100"), any(MessageItemResponse.class));
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

    @DisplayName("읽음 처리를 하면 새로 읽은 메시지들의 안읽음 수 갱신이 실시간으로 브로드캐스트된다.")
    @Test
    void 읽음_처리를_하면_새로_읽은_메시지들의_안읽음_수_갱신이_실시간으로_브로드캐스트된다() {
        // given
        Team team = Team.builder().teamId(100L).build();
        TeamMember reader = teamMemberOf(team, 1L, "김철수");
        TeamMember other = teamMemberOf(team, 2L, "이해은");
        Message message = Message.builder()
                .team(team)
                .senderType(MessageSenderType.MEMBER)
                .senderTeamMember(other)
                .messageType(MessageType.TEXT)
                .content("안녕하세요.")
                .build();
        ReflectionTestUtils.setField(message, "messageId", 10L);
        ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.now().minusMinutes(10));

        given(teamRepository.findById(100L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(100L, 1L))
                .willReturn(Optional.of(reader));
        given(teamMemberRepository.findByTeamIdAndStatus(100L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(reader, other));
        given(messageRepository.findByTeamIdBeforeCursor(eq(100L), isNull(), any()))
                .willReturn(List.of(message));

        // when
        chatService.markAsRead(100L, 1L);

        // then
        ArgumentCaptor<MessageUnreadUpdateResponse> captor = ArgumentCaptor.forClass(MessageUnreadUpdateResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/teams/100/read-updates"), captor.capture());
        assertThat(captor.getValue().updates()).singleElement().satisfies(update -> {
            assertThat(update.messageId()).isEqualTo(10L);
            assertThat(update.unreadCount()).isEqualTo(0); // 보낸 other 제외, reader는 방금 읽었으니 0명
        });
    }

    @DisplayName("새로 읽은 메시지가 없으면 안읽음 수 갱신을 브로드캐스트하지 않는다.")
    @Test
    void 새로_읽은_메시지가_없으면_안읽음_수_갱신을_브로드캐스트하지_않는다() {
        // given
        Team team = Team.builder().teamId(100L).build();
        TeamMember me = teamMemberOf(team, 1L, "김철수");
        given(teamRepository.findById(100L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(100L, 1L))
                .willReturn(Optional.of(me));
        given(messageRepository.findByTeamIdBeforeCursor(eq(100L), isNull(), any()))
                .willReturn(List.of());

        // when
        chatService.markAsRead(100L, 1L);

        // then
        verify(messagingTemplate, never())
                .convertAndSend(eq("/topic/teams/100/read-updates"), any(MessageUnreadUpdateResponse.class));
    }

    private TeamMember teamMemberOf(Team team, Long memberId, String nickname) {
        Member member = Member.builder().memberId(memberId).build();
        Profile profile = Profile.builder().nickname(nickname).build();
        return TeamMember.builder()
                .team(team)
                .member(member)
                .profile(profile)
                .status(TeamMemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now().minusDays(1))
                .build();
    }
}
