package org.cotato.gongmozip.global.validation;

public final class PasswordPolicy {

    public static final String REGEX = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s])\\S{8,20}$";
    public static final String MESSAGE = "비밀번호는 영문, 숫자, 특수문자를 포함한 8~20자여야 합니다.";

    private PasswordPolicy() {}
}
