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
            MemberAvatarResponse senderAvatar,
            long unreadCount) {}

    /** hasNext: 이 목록보다 더 오래된 메시지가 남아있는지 여부 — true면 가장 오래된 메시지의 messageId를 cursor로 다시 요청한다. */
    public record MessageListResponse(List<MessageItemResponse> messages, boolean hasNext) {}

    /** 팀원이 읽음 처리를 해서 기존 메시지들의 안읽음 수가 줄었을 때 실시간으로 내려주는 갱신 이벤트. */
    public record MessageUnreadUpdateResponse(List<MessageUnreadUpdate> updates) {}

    public record MessageUnreadUpdate(Long messageId, long unreadCount) {}
}
