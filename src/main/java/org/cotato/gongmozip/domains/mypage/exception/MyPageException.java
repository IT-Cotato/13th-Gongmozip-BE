package org.cotato.gongmozip.domains.mypage.exception;

import lombok.Getter;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.cotato.gongmozip.global.exception.CustomException;

@Getter
public class MyPageException extends CustomException {

    public MyPageException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
