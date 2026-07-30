package org.cotato.gongmozip.domains.report.exception;

import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.cotato.gongmozip.global.exception.CustomException;

public class ReportException extends CustomException {
    public ReportException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
