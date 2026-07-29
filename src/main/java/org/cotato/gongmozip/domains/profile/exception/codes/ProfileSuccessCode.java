package org.cotato.gongmozip.domains.profile.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.response.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProfileSuccessCode implements BaseSuccessCode {

    // 200
    PROFILE_LIST_RETRIEVED(HttpStatus.OK, "PROFILE_200_1", "내 프로필 목록 조회에 성공하였습니다."),
    PROFILE_RETRIEVED(HttpStatus.OK, "PROFILE_200_2", "프로필 상세 조회에 성공하였습니다."),
    PROFILE_UPDATED(HttpStatus.OK, "PROFILE_200_3", "프로필 수정에 성공하였습니다."),
    PROFILE_DELETED(HttpStatus.OK, "PROFILE_200_4", "프로필 삭제에 성공하였습니다."),
    MAIN_PROFILE_SET(HttpStatus.OK, "PROFILE_200_5", "대표 프로필 설정에 성공하였습니다."),
    PROFILE_VISIBILITY_CHANGED(HttpStatus.OK, "PROFILE_200_6", "프로필 공개 여부 변경에 성공하였습니다."),
    PROFILE_PREVIEW_RETRIEVED(HttpStatus.OK, "PROFILE_200_7", "프로필 미리보기 조회에 성공하였습니다."),
    PUBLIC_PROFILE_RETRIEVED(HttpStatus.OK, "PROFILE_200_8", "공개 프로필 조회에 성공하였습니다."),

    PROJECT_LIST_RETRIEVED(HttpStatus.OK, "PROFILE_200_9", "프로젝트 경험 목록 조회에 성공하였습니다."),
    PROJECT_RETRIEVED(HttpStatus.OK, "PROFILE_200_10", "프로젝트 경험 상세 조회에 성공하였습니다."),
    PROJECT_UPDATED(HttpStatus.OK, "PROFILE_200_11", "프로젝트 경험 수정에 성공하였습니다."),
    PROJECT_DELETED(HttpStatus.OK, "PROFILE_200_12", "프로젝트 경험 삭제에 성공하였습니다."),

    AWARD_LIST_RETRIEVED(HttpStatus.OK, "PROFILE_200_13", "수상 경험 목록 조회에 성공하였습니다."),
    AWARD_RETRIEVED(HttpStatus.OK, "PROFILE_200_14", "수상 경험 상세 조회에 성공하였습니다."),
    AWARD_UPDATED(HttpStatus.OK, "PROFILE_200_15", "수상 경험 수정에 성공하였습니다."),
    AWARD_DELETED(HttpStatus.OK, "PROFILE_200_16", "수상 경험 삭제에 성공하였습니다."),

    CERTIFICATION_CATEGORIES_RETRIEVED(HttpStatus.OK, "PROFILE_200_17", "자격증 카테고리 조회에 성공하였습니다."),
    CERTIFICATIONS_SEARCHED(HttpStatus.OK, "PROFILE_200_18", "자격증 검색에 성공하였습니다."),
    CERTIFICATION_LIST_RETRIEVED(HttpStatus.OK, "PROFILE_200_19", "자격증 목록 조회에 성공하였습니다."),
    CERTIFICATION_RETRIEVED(HttpStatus.OK, "PROFILE_200_20", "자격증 상세 조회에 성공하였습니다."),
    CERTIFICATION_UPDATED(HttpStatus.OK, "PROFILE_200_21", "자격증 수정에 성공하였습니다."),
    CERTIFICATION_DELETED(HttpStatus.OK, "PROFILE_200_22", "자격증 삭제에 성공하였습니다."),
    AI_SUMMARY_RETRIEVED(HttpStatus.OK, "PROFILE_200_23", "프로젝트 AI 요약 조회에 성공하였습니다."),

    AI_SUMMARY_GENERATION_REQUESTED(HttpStatus.ACCEPTED, "PROFILE_202_1", "프로젝트 AI 요약 생성 요청에 성공하였습니다."),

    // 201
    PROFILE_CREATED(HttpStatus.CREATED, "PROFILE_201_1", "프로필 생성에 성공하였습니다."),
    PROJECT_CREATED(HttpStatus.CREATED, "PROFILE_201_2", "프로젝트 경험 등록에 성공하였습니다."),
    AWARD_CREATED(HttpStatus.CREATED, "PROFILE_201_3", "수상 경험 등록에 성공하였습니다."),
    CERTIFICATION_CREATED(HttpStatus.CREATED, "PROFILE_201_4", "자격증 등록에 성공하였습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
