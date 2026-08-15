package org.cotato.gongmozip.domains.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.domains.chat.service.ChatService;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.cotato.gongmozip.global.ai.AiClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * "@챗봇" 자유 질의에 대한 AI 응답 생성을 전담한다. AI Gateway 응답을 기다리는 동안(최대 20초,
 * {@code AiGatewayClient}) 웹소켓 메시지 처리 스레드(STOMP inbound channel)를 막지 않도록
 * {@code ChatbotOrchestrationService#respondToMentionIfAny}와 분리해 {@code aiSummaryExecutor}에서
 * 비동기로 실행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotMentionAsyncService {

    private final AiClient aiClient;
    private final TeamRepository teamRepository;
    private final ChatService chatService;

    @Async("aiSummaryExecutor")
    public void answerMentionAsync(Long teamId, String question) {
        String answer;
        try {
            answer = aiClient.answerTeamQuestion(question);
        } catch (Exception e) {
            log.error("챗봇 자유질의 AI 호출 실패 - teamId: {}", teamId, e);
            return;
        }

        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) {
            return;
        }
        chatService.postChatbotMessage(team, answer);
    }
}
