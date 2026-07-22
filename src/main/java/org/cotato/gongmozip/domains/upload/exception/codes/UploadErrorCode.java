package org.cotato.gongmozip.domains.upload.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UploadErrorCode implements BaseErrorCode {

    // 400
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "UPLOAD_400_1", "허용되지 않는 파일 형식입니다. 이미지 파일만 업로드할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
