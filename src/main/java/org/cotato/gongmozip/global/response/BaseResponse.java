package org.cotato.gongmozip.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;
import org.cotato.gongmozip.global.exception.BaseErrorCode;

@Getter
@Builder
@JsonPropertyOrder({"status", "code", "message", "data"})
public class BaseResponse<T> {

    private final int status;
    private final String code;
    private final String message;

    // 클래스 레벨에 두면 status(0), code(null) 같은 기본값 필드도 직렬화에서 제외돼 에러 응답이 깨지므로
    // data 필드에만 적용해 에러 시 "data": null 이 응답에 포함되지 않도록 한다
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    public static <T> BaseResponse<T> success(BaseSuccessCode baseSuccessCode, T data) {
        return BaseResponse.<T>builder()
                .status(baseSuccessCode.getHttpStatus().value())
                .code(baseSuccessCode.getCode())
                .message(baseSuccessCode.getMessage())
                .data(data)
                .build();
    }

    public static BaseResponse<Void> success(BaseSuccessCode baseSuccessCode) {
        return BaseResponse.<Void>builder()
                .status(baseSuccessCode.getHttpStatus().value())
                .code(baseSuccessCode.getCode())
                .message(baseSuccessCode.getMessage())
                .build();
    }

    public static BaseResponse<Void> error(BaseErrorCode errorCode) {
        return BaseResponse.<Void>builder()
                .status(errorCode.getHttpStatus().value())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }

    // @Valid 검증 실패 시 각 필드의 구체적인 에러 메시지를 내려줘야 해서 코드 기본 메시지를 덮어쓸 수 있도록 오버로드
    public static BaseResponse<Void> error(BaseErrorCode errorCode, String customMessage) {
        return BaseResponse.<Void>builder()
                .status(errorCode.getHttpStatus().value())
                .code(errorCode.getCode())
                .message(customMessage)
                .build();
    }
}
