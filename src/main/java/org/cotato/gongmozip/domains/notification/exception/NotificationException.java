package org.cotato.gongmozip.domains.notification.exception;

import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.cotato.gongmozip.global.exception.CustomException;

public class NotificationException extends CustomException {
    public NotificationException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
