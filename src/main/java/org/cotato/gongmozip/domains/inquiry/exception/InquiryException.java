package org.cotato.gongmozip.domains.inquiry.exception;

import lombok.Getter;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.cotato.gongmozip.global.exception.CustomException;

@Getter
public class InquiryException extends CustomException {

    public InquiryException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
