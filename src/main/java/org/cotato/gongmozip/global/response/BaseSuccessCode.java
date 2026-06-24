package org.cotato.gongmozip.global.response;

import org.springframework.http.HttpStatus;

public interface BaseSuccessCode {
    HttpStatus getHttpStatus();

    String getCode();

    String getMessage();
}
