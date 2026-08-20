package org.cotato.gongmozip.domains.notification.dto.request;

import jakarta.validation.constraints.NotBlank;

public final class PushTokenRequest {

    private PushTokenRequest() {}

    public record TokenRequest(@NotBlank(message = "FCM 토큰은 필수 입력 항목입니다.") String token) {}
}
