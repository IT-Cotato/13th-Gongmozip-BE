package org.cotato.gongmozip.domains.notification.converter;

import java.util.List;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.notification.dto.response.NotificationResponse.NotificationItemResponse;
import org.cotato.gongmozip.domains.notification.dto.response.NotificationResponse.NotificationListResponse;
import org.cotato.gongmozip.domains.notification.entity.Notification;
import org.cotato.gongmozip.domains.notification.enums.NotificationCategory;

public final class NotificationConverter {

    private NotificationConverter() {}

    public static Notification toNotification(
            Member receiver, NotificationCategory category, String body, Long relatedTeamId) {
        return Notification.builder()
                .receiverMember(receiver)
                .category(category)
                .body(body)
                .relatedTeamId(relatedTeamId)
                .build();
    }

    public static NotificationListResponse toListResponse(List<Notification> notifications, boolean hasNext) {
        return new NotificationListResponse(
                notifications.stream()
                        .map(NotificationConverter::toItemResponse)
                        .toList(),
                hasNext);
    }

    private static NotificationItemResponse toItemResponse(Notification notification) {
        return new NotificationItemResponse(
                notification.getNotificationId(),
                notification.getCategory().name(),
                notification.getBody(),
                notification.getRelatedTeamId(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
