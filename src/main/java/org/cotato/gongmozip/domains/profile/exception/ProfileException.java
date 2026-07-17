package org.cotato.gongmozip.domains.profile.exception;

import lombok.Getter;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.cotato.gongmozip.global.exception.CustomException;

@Getter
public class ProfileException extends CustomException {

    public ProfileException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
