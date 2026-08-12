package org.cotato.gongmozip.domains.member.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {

    // 400
    INVALID_VERIFY_CODE(HttpStatus.BAD_REQUEST, "MEMBER_400_1", "인증코드가 올바르지 않습니다."),
    EXPIRED_VERIFY_CODE(HttpStatus.BAD_REQUEST, "MEMBER_400_2", "인증코드가 만료되었습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "MEMBER_400_3", "이메일 인증이 완료되지 않았습니다."),
    VERIFY_CODE_NOT_ISSUED(HttpStatus.BAD_REQUEST, "MEMBER_400_4", "인증코드가 발급된 적 없습니다. 먼저 이메일 인증을 요청해 주세요."),
    WITHDRAW_PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "MEMBER_400_5", "비밀번호를 입력해 주세요."),
    WITHDRAW_INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "MEMBER_400_6", "비밀번호가 올바르지 않습니다."),

    // 403
    WITHDRAWN_MEMBER(HttpStatus.FORBIDDEN, "MEMBER_403_1", "탈퇴한 회원입니다."),
    REJOIN_RESTRICTED(HttpStatus.FORBIDDEN, "MEMBER_403_2", "탈퇴 후 14일간 재가입할 수 없습니다."),

    // 404
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_404_1", "존재하지 않는 회원입니다."),

    // 409
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "MEMBER_409_1", "이미 가입된 이메일입니다."),
    WITHDRAWAL_BLOCKED_BY_MATCHING(HttpStatus.CONFLICT, "MEMBER_409_2", "진행 중인 매칭이 있어 탈퇴할 수 없습니다."),
    WITHDRAWAL_BLOCKED_BY_ACTIVE_TEAM(HttpStatus.CONFLICT, "MEMBER_409_3", "진행 중인 팀 프로젝트가 있어 탈퇴할 수 없습니다."),

    // 429
    TOO_MANY_VERIFY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "MEMBER_429_1", "인증 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요."),

    // 500
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MEMBER_500_1", "이메일 전송에 실패했습니다. 잠시 후 다시 시도해 주세요."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
