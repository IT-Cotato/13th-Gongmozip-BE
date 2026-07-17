package org.cotato.gongmozip.global.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ApiResponse<Void>를 직접 반환하면 HTTP 상태코드가 항상 200으로 고정된다.
    // ResponseEntity로 감싸야 응답 헤더의 HTTP 상태코드가 에러 코드에 맞게 설정된다.
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<BaseResponse<Void>> handleCustomException(CustomException e) {
        BaseErrorCode errorCode = e.getErrorCode();
        log.warn("[CustomException] code={}, message={}", errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(BaseResponse.error(errorCode));
    }

    // @RequestBody @Valid 검증 실패 → MethodArgumentNotValidException
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("[ValidationException] message={}", message);
        return ResponseEntity.badRequest().body(BaseResponse.error(GlobalErrorCode.INVALID_REQUEST, message));
    }

    // @RequestParam, @PathVariable 등 메서드 파라미터 @Validated 검증 실패 → ConstraintViolationException
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining(", "));
        log.warn("[ConstraintViolation] message={}", message);
        return ResponseEntity.badRequest().body(BaseResponse.error(GlobalErrorCode.INVALID_PARAMETER, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("[HttpMessageNotReadable] message={}", e.getMessage());
        return ResponseEntity.badRequest().body(BaseResponse.error(GlobalErrorCode.INVALID_JSON));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<BaseResponse<Void>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException e) {
        String message = "필수 파라미터 '" + e.getParameterName() + "'이(가) 없습니다.";
        log.warn("[MissingParameter] message={}", message);
        return ResponseEntity.badRequest().body(BaseResponse.error(GlobalErrorCode.INVALID_PARAMETER, message));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<BaseResponse<Void>> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException e) {
        log.warn("[MethodNotSupported] method={}", e.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(BaseResponse.error(GlobalErrorCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<BaseResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("[DataIntegrityViolation] message={}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(BaseResponse.error(GlobalErrorCode.DATA_INTEGRITY_CONFLICT));
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<BaseResponse<Void>> handleLockConflict(PessimisticLockingFailureException e) {
        log.warn("[LockConflict] message={}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(BaseResponse.error(GlobalErrorCode.RESOURCE_LOCK_CONFLICT));
    }

    // 위 핸들러에서 잡히지 않은 모든 예외의 최후 방어선. 반드시 마지막에 위치해야 한다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleException(Exception e) {
        log.error("[UnhandledException] ", e);
        return ResponseEntity.internalServerError().body(BaseResponse.error(GlobalErrorCode.INTERNAL_SERVER_ERROR));
    }
}
