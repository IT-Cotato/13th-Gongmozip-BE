package org.cotato.gongmozip.domains.profile.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProfileErrorCode implements BaseErrorCode {

    // 400
    INVALID_GPA(HttpStatus.BAD_REQUEST, "PROFILE_400_1", "학점 입력값이 올바르지 않습니다."),
    INVALID_PROJECT_PERIOD(HttpStatus.BAD_REQUEST, "PROFILE_400_2", "프로젝트 기간이 올바르지 않습니다."),
    UNSUPPORTED_CERTIFICATION_CATEGORY(HttpStatus.BAD_REQUEST, "PROFILE_400_3", "지원하지 않는 자격증 카테고리입니다."),
    NO_FIELDS_TO_UPDATE(HttpStatus.BAD_REQUEST, "PROFILE_400_4", "수정할 필드가 없습니다."),
    INVALID_DATE(HttpStatus.BAD_REQUEST, "PROFILE_400_5", "미래 날짜는 입력할 수 없습니다."),
    AI_SUMMARY_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "PROFILE_400_6", "이미 프로젝트 AI 요약이 생성되었거나 생성 중입니다."),

    // 403
    PROFILE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "PROFILE_403_1", "해당 프로필에 접근할 권한이 없습니다."),

    // 404
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "PROFILE_404_1", "프로필을 찾을 수 없습니다."),
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROFILE_404_2", "프로젝트 경험을 찾을 수 없습니다."),
    AWARD_NOT_FOUND(HttpStatus.NOT_FOUND, "PROFILE_404_3", "수상 경험을 찾을 수 없습니다."),
    CERTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "PROFILE_404_4", "자격증을 찾을 수 없습니다."),
    AI_SUMMARY_NOT_FOUND(HttpStatus.NOT_FOUND, "PROFILE_404_5", "프로젝트 AI 요약이 존재하지 않습니다."),

    // 409
    ALREADY_MAIN_PROFILE(HttpStatus.CONFLICT, "PROFILE_409_1", "이미 대표 프로필로 설정되어 있습니다."),
    DUPLICATE_CERTIFICATION(HttpStatus.CONFLICT, "PROFILE_409_2", "동일한 자격증이 중복 등록되었습니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "PROFILE_409_3", "동일한 닉네임의 프로필이 이미 존재합니다."),
    CANNOT_DELETE_REFERENCED_PROFILE(HttpStatus.CONFLICT, "PROFILE_409_4", "매칭 신청 이력이 존재하여 삭제할 수 없는 프로필입니다."),
    AI_SUMMARY_GENERATION_IN_PROGRESS(HttpStatus.CONFLICT, "PROFILE_409_5", "프로젝트 AI 요약 생성이 이미 진행 중입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
