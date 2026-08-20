package org.cotato.gongmozip.domains.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.notification.converter.NotificationConverter;
import org.cotato.gongmozip.domains.notification.dto.response.NotificationResponse.NotificationListResponse;
import org.cotato.gongmozip.domains.notification.dto.response.NotificationResponse.NotificationUnreadExistsResponse;
import org.cotato.gongmozip.domains.notification.exception.codes.NotificationErrorCode;
import org.cotato.gongmozip.domains.notification.exception.codes.NotificationSuccessCode;
import org.cotato.gongmozip.domains.notification.service.NotificationService;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification", description = "알림함 관련 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "알림 목록 조회",
            description = "category를 생략하면 전체 탭, OTHER/MATCHING/CHATROOM을 넘기면 해당 탭만 조회한다. "
                    + "cursor를 생략하면 최신 페이지를, 직전 응답에서 받은 가장 오래된 알림의 notificationId를 cursor로 넘기면 이어서 조회한다.")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = NotificationErrorCode.class)
    @GetMapping
    public ResponseEntity<BaseResponse<NotificationListResponse>> getNotifications(
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "cursor", required = false) Long cursor,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        NotificationListResponse response = notificationService.getNotifications(
                userDetails.getMemberId(), NotificationConverter.toNotificationCategory(category), cursor);
        return BaseResponseFormatter.success(NotificationSuccessCode.NOTIFICATION_LIST_RETRIEVED, response);
    }

    @Operation(summary = "안읽은 알림 존재 여부 조회", description = "홈 화면 종 아이콘의 빨간 배지 표시 여부를 판단하는 데 사용한다.")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class)
    @GetMapping("/unread-exists")
    public ResponseEntity<BaseResponse<NotificationUnreadExistsResponse>> getUnreadExists(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean unreadExists = notificationService.existsUnread(userDetails.getMemberId());
        return BaseResponseFormatter.success(
                NotificationSuccessCode.NOTIFICATION_UNREAD_EXISTS_RETRIEVED,
                new NotificationUnreadExistsResponse(unreadExists));
    }

    @Operation(summary = "알림 전체 읽음 처리", description = "알림함 화면에 진입하는 시점에 호출한다. 카테고리 탭과 무관하게 전체를 읽음 처리한다.")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class)
    @PatchMapping("/read-all")
    public ResponseEntity<BaseResponse<Void>> markAllAsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getMemberId());
        return BaseResponseFormatter.success(NotificationSuccessCode.NOTIFICATIONS_MARKED_READ);
    }
}
