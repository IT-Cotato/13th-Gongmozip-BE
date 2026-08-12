package org.cotato.gongmozip.domains.member.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.response.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberSuccessCode implements BaseSuccessCode {

    // 200
    EMAIL_VERIFY_CODE_SENT(HttpStatus.OK, "MEMBER_200_1", "인증코드가 발송되었습니다."),
    EMAIL_VERIFY_SUCCESS(HttpStatus.OK, "MEMBER_200_2", "이메일 인증에 성공하였습니다."),

    // 200
    REQUIRED_INFO_REGISTERED(HttpStatus.OK, "MEMBER_200_3", "필수 정보가 등록되었습니다."),
    MEMBER_DETAIL_RETRIEVED(HttpStatus.OK, "MEMBER_200_4", "내 기본 정보 조회 성공"),
    MEMBER_DETAIL_UPDATED(HttpStatus.OK, "MEMBER_200_5", "내 기본 정보 수정 성공"),
    MARKETING_CONSENT_UPDATED(HttpStatus.OK, "MEMBER_200_6", "마케팅 수신 동의 수정 성공"),
    PROFILE_IMAGE_PRESIGNED_URL_GENERATED(HttpStatus.OK, "MEMBER_200_7", "프로필 이미지 업로드 Presigned URL 발급 성공"),
    PROFILE_IMAGE_UPDATED(HttpStatus.OK, "MEMBER_200_8", "프로필 이미지 수정 성공"),
    MEMBER_WITHDRAWN(HttpStatus.OK, "MEMBER_200_9", "회원탈퇴가 완료되었습니다."),

    // 201
    SIGN_UP_SUCCESS(HttpStatus.CREATED, "MEMBER_201_1", "회원가입에 성공하였습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
