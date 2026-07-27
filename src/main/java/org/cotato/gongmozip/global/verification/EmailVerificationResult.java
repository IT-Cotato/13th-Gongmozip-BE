package org.cotato.gongmozip.global.verification;

public enum EmailVerificationResult {
    VERIFIED,
    INVALID_CODE,
    EXPIRED_CODE,
    CODE_NOT_ISSUED,
    TOO_MANY_ATTEMPTS
}
