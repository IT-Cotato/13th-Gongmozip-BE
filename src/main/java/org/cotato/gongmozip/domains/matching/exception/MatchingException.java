package org.cotato.gongmozip.domains.matching.exception;

import lombok.Getter;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.cotato.gongmozip.global.exception.CustomException;

@Getter
public class MatchingException extends CustomException {

    public MatchingException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
