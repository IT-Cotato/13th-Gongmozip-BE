package org.cotato.gongmozip.domains.team.exception;

import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.cotato.gongmozip.global.exception.CustomException;

public class TeamException extends CustomException {
    public TeamException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
