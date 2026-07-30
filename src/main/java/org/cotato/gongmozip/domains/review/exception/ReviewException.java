package org.cotato.gongmozip.domains.review.exception;

import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.cotato.gongmozip.global.exception.CustomException;

public class ReviewException extends CustomException {
    public ReviewException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
