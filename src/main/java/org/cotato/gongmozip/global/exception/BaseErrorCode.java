package org.cotato.gongmozip.global.exception;

import org.springframework.http.HttpStatus;

public interface BaseErrorCode {
    HttpStatus getHttpStatus();

    String getCode();

    String getMessage();
}
