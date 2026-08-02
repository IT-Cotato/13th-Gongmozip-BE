package org.cotato.gongmozip.domains.chat.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.MemberAvatarResponse;

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
            LocalDateTime createdAt,
            MemberAvatarResponse senderAvatar) {}

    public record MessageListResponse(List<MessageItemResponse> messages) {}
}
