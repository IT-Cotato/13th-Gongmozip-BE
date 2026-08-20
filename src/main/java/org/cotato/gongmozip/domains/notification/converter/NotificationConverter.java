package org.cotato.gongmozip.domains.notification.converter;

import java.util.List;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.notification.dto.response.NotificationResponse.NotificationItemResponse;
import org.cotato.gongmozip.domains.notification.dto.response.NotificationResponse.NotificationListResponse;
import org.cotato.gongmozip.domains.notification.entity.Notification;
import org.cotato.gongmozip.domains.notification.enums.NotificationCategory;
import org.cotato.gongmozip.domains.notification.exception.NotificationException;
import org.cotato.gongmozip.domains.notification.exception.codes.NotificationErrorCode;

public final class NotificationConverter {

    private NotificationConverter() {}

    // Spring의 기본 enum @RequestParam 바인딩은 값이 잘못됐을 때 GlobalExceptionHandler가 못 잡는
    // MethodArgumentTypeMismatchException을 던져 400이 아닌 500으로 응답한다(report 도메인의
    // ReportConverter.toReportReason과 동일하게, 컨트롤러가 String으로 받아 여기서 직접 검증한다).
    public static NotificationCategory toNotificationCategory(String category) {
        if (category == null) {
            return null;
        }
        try {
            return NotificationCategory.valueOf(category.trim());
        } catch (IllegalArgumentException e) {
            throw new NotificationException(NotificationErrorCode.INVALID_CATEGORY);
        }
    }

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
