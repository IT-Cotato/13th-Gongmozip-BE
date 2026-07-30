package org.cotato.gongmozip.domains.chat.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public final class ChatResponse {

    private ChatResponse() {}

    public record MessageItemResponse(
            Long messageId,
            String senderType,
            Long senderTeamMemberId,
            String senderNickname,
            String messageType,
            String content,
            String metadata,
            LocalDateTime createdAt) {}

    public record MessageListResponse(List<MessageItemResponse> messages) {}
}
