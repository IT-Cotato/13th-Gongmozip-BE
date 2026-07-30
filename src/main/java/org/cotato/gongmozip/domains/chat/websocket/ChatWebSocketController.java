package org.cotato.gongmozip.domains.chat.websocket;

import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.domains.chat.dto.request.ChatRequest.SendMessageRequest;
import org.cotato.gongmozip.domains.chat.service.ChatService;
import org.cotato.gongmozip.domains.chatbot.service.ChatbotOrchestrationService;
import org.cotato.gongmozip.global.exception.CustomException;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

/** REST가 아닌 STOMP를 통한 메시지 전송 진입점. 저장/브로드캐스트는 ChatService가 담당한다. */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final ChatbotOrchestrationService chatbotOrchestrationService;

    @MessageMapping("/teams/{teamId}/messages")
    public void sendMessage(
            @DestinationVariable Long teamId, @Payload @Valid SendMessageRequest request, Principal principal) {
        Long memberId = resolveMemberId(principal);
        chatService.sendMessage(teamId, memberId, request);
        chatbotOrchestrationService.recordGreetingAndAdvance(teamId, memberId);
        chatbotOrchestrationService.respondToMentionIfAny(teamId, request.content());
    }

    @MessageExceptionHandler(CustomException.class)
    @SendToUser("/queue/errors")
    public String handleCustomException(CustomException exception) {
        log.warn("WebSocket 메시지 처리 실패: {}", exception.getMessage());
        return exception.getMessage();
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public String handleException(Exception exception) {
        log.error("WebSocket 메시지 처리 중 알 수 없는 오류", exception);
        return "메시지 처리 중 오류가 발생했습니다.";
    }

    private Long resolveMemberId(Principal principal) {
        Authentication authentication = (Authentication) principal;
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getMemberId();
    }
}
