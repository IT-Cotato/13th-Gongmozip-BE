package org.cotato.gongmozip.domains.upload.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.response.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UploadSuccessCode implements BaseSuccessCode {

    // 200
    PRESIGNED_URL_GENERATED(HttpStatus.OK, "UPLOAD_200_1", "Presigned URL 발급에 성공하였습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
