package org.cotato.gongmozip.domains.contest.exception;

import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.cotato.gongmozip.global.exception.CustomException;

public class ContestException extends CustomException {
    public ContestException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
