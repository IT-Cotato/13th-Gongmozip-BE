package org.cotato.gongmozip.global.response;

import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.springframework.http.ResponseEntity;

public final class BaseResponseFormatter {

    private BaseResponseFormatter() {}

    public static <T> ResponseEntity<BaseResponse<T>> toResponseEntity(BaseResponse<T> baseResponse) {
        return ResponseEntity.status(baseResponse.getStatus()).body(baseResponse);
    }

    public static <T> ResponseEntity<BaseResponse<T>> success(BaseSuccessCode baseSuccessCode, T data) {
        BaseResponse<T> response = BaseResponse.success(baseSuccessCode, data);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    public static ResponseEntity<BaseResponse<Void>> success(BaseSuccessCode baseSuccessCode) {
        BaseResponse<Void> response = BaseResponse.success(baseSuccessCode);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    public static ResponseEntity<BaseResponse<Void>> error(BaseErrorCode errorCode) {
        BaseResponse<Void> response = BaseResponse.error(errorCode);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
