package org.cotato.gongmozip.domains.member.exception;

import lombok.Getter;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.cotato.gongmozip.global.exception.CustomException;

@Getter
public class MemberException extends CustomException {

    public MemberException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
