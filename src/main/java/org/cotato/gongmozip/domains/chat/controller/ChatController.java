package org.cotato.gongmozip.domains.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.chat.dto.response.ChatResponse.MessageListResponse;
import org.cotato.gongmozip.domains.chat.exception.codes.ChatSuccessCode;
import org.cotato.gongmozip.domains.chat.service.ChatService;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 메시지 전송은 WebSocket(STOMP)으로 이동했다 — {@code chat.websocket.ChatWebSocketController}
 * 참고. 여기서는 채팅방 진입 시 과거 메시지를 불러오는 이력 조회와, 실시간 브로드캐스트가
 * 필요 없는 읽음 처리만 REST로 남긴다 (docs/decisions/03-chat.md).
 */
@Tag(name = "Chat", description = "채팅 메시지 관련 API")
@RestController
@RequestMapping("/api/teams/{teamId}")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "메시지 목록 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = TeamErrorCode.class)
    @GetMapping("/messages")
    public ResponseEntity<BaseResponse<MessageListResponse>> getMessages(
            @PathVariable("teamId") Long teamId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        MessageListResponse response = chatService.getMessages(teamId, userDetails.getMemberId());
        return BaseResponseFormatter.success(ChatSuccessCode.MESSAGE_LIST_RETRIEVED, response);
    }

    @Operation(summary = "채팅방 읽음 처리")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = TeamErrorCode.class)
    @PatchMapping("/read")
    public ResponseEntity<BaseResponse<Void>> markAsRead(
            @PathVariable("teamId") Long teamId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        chatService.markAsRead(teamId, userDetails.getMemberId());
        return BaseResponseFormatter.success(ChatSuccessCode.MESSAGES_MARKED_READ);
    }
}
