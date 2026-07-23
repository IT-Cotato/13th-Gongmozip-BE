package org.cotato.gongmozip.domains.upload.exception;

import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.cotato.gongmozip.global.exception.CustomException;

public class UploadException extends CustomException {
    public UploadException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
