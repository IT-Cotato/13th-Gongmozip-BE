package org.cotato.gongmozip.domains.notification.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public final class NotificationResponse {

    private NotificationResponse() {}

    public record NotificationItemResponse(
            Long notificationId,
            String category,
            String body,
            Long relatedTeamId,
            boolean isRead,
            LocalDateTime createdAt) {}

    /** hasNext: 이 목록보다 더 오래된 알림이 남아있는지 여부 — true면 가장 오래된 알림의 notificationId를 cursor로 다시 요청한다. */
    public record NotificationListResponse(List<NotificationItemResponse> notifications, boolean hasNext) {}

    public record NotificationUnreadExistsResponse(boolean unreadExists) {}
}
