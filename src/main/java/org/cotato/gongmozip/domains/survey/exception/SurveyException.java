package org.cotato.gongmozip.domains.survey.exception;

import lombok.Getter;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.cotato.gongmozip.global.exception.CustomException;

@Getter
public class SurveyException extends CustomException {

    public SurveyException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
