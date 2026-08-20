package org.cotato.gongmozip.domains.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.notification.dto.request.PushTokenRequest.TokenRequest;
import org.cotato.gongmozip.domains.notification.exception.codes.NotificationSuccessCode;
import org.cotato.gongmozip.domains.notification.service.PushTokenService;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** OS 푸시(FCM) 등록 토큰 관리 API (docs/decisions/13-fcm-push.md). */
@Tag(name = "PushToken", description = "FCM 푸시 토큰 등록/해제 API")
@RestController
@RequestMapping("/api/notifications/push-tokens")
@RequiredArgsConstructor
public class PushTokenController {

    private final PushTokenService pushTokenService;

    @Operation(summary = "푸시 토큰 등록", description = "로그인한 회원에 FCM 토큰을 등록한다. 이미 존재하는 토큰이면 소유자만 갈아끼운다(upsert).")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class)
    @PostMapping
    public ResponseEntity<BaseResponse<Void>> register(
            @RequestBody @Valid TokenRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        pushTokenService.register(userDetails.getMemberId(), request.token());
        return BaseResponseFormatter.success(NotificationSuccessCode.PUSH_TOKEN_REGISTERED);
    }

    @Operation(summary = "푸시 토큰 해제", description = "로그아웃 시 호출한다. 본인 소유 토큰만 해제되며, 이미 없는 토큰이어도 에러 없이 끝난다.")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class)
    @DeleteMapping
    public ResponseEntity<BaseResponse<Void>> unregister(
            @RequestBody @Valid TokenRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        pushTokenService.unregister(userDetails.getMemberId(), request.token());
        return BaseResponseFormatter.success(NotificationSuccessCode.PUSH_TOKEN_UNREGISTERED);
    }
}
