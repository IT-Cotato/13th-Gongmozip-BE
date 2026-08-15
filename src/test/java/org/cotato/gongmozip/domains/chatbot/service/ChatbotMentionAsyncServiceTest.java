package org.cotato.gongmozip.domains.chatbot.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.cotato.gongmozip.domains.chat.service.ChatService;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.cotato.gongmozip.global.ai.AiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatbotMentionAsyncServiceTest {

    @Mock
    private AiClient aiClient;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatbotMentionAsyncService service;

    @DisplayName("AI 답변을 챗봇 메시지로 발행한다.")
    @Test
    void AI_답변을_챗봇_메시지로_발행한다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.IN_PROGRESS).build();
        given(aiClient.answerTeamQuestion("우리 역할 분담 추천해줘")).willReturn("역할 분담 추천이에요.");
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when
        service.answerMentionAsync(1L, "우리 역할 분담 추천해줘");

        // then
        verify(chatService).postChatbotMessage(team, "역할 분담 추천이에요.");
    }

    @DisplayName("AI 호출이 실패하면 메시지를 남기지 않는다.")
    @Test
    void AI_호출이_실패하면_메시지를_남기지_않는다() {
        // given
        given(aiClient.answerTeamQuestion(any())).willThrow(new RuntimeException("AI Gateway timeout"));

        // when
        service.answerMentionAsync(1L, "질문");

        // then
        verify(teamRepository, never()).findById(any());
        verify(chatService, never()).postChatbotMessage(any(), any());
    }

    @DisplayName("그 사이 팀이 사라졌으면 메시지를 남기지 않는다.")
    @Test
    void 팀이_없으면_메시지를_남기지_않는다() {
        // given
        given(aiClient.answerTeamQuestion("질문")).willReturn("답변");
        given(teamRepository.findById(1L)).willReturn(Optional.empty());

        // when
        service.answerMentionAsync(1L, "질문");

        // then
        verify(chatService, never()).postChatbotMessage(any(), any());
    }
}
