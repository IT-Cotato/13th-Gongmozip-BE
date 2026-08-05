package org.cotato.gongmozip.domains.matching.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MatchingErrorCode implements BaseErrorCode {
    // 400 Bad Request
    APPLICATION_DEADLINE_PASSED(HttpStatus.BAD_REQUEST, "MATCHING_400_1", "오늘의 매칭 신청이 마감되었습니다."),
    PROFILE_REQUIRED(HttpStatus.BAD_REQUEST, "MATCHING_400_2", "매칭에 사용할 프로필 작성이 필요합니다."),
    SURVEY_REQUIRED(HttpStatus.BAD_REQUEST, "MATCHING_400_3", "협업 유형 검사를 완료해야 합니다."),
    WITHDRAWAL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "MATCHING_400_4", "현재는 매칭 신청을 철회할 수 없습니다."),
    INVALID_PROFILE_GPA(HttpStatus.BAD_REQUEST, "MATCHING_400_5", "프로필의 학점 입력값이 올바르지 않습니다."),
    PROJECT_EVALUATION_NOT_READY(HttpStatus.BAD_REQUEST, "MATCHING_400_6", "프로젝트 AI 평가가 모두 완료된 후 매칭을 신청할 수 있습니다."),
    MATCHING_RESPONSE_DEADLINE_PASSED(HttpStatus.BAD_REQUEST, "MATCHING_400_7", "매칭 결과 응답 기한이 지났습니다."),

    // 403 Forbidden
    PROFILE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "MATCHING_403_1", "본인의 프로필만 매칭에 사용할 수 있습니다."),
    MATCHING_RESTRICTED(HttpStatus.FORBIDDEN, "MATCHING_403_2", "협업거리 감소로 인해 현재 매칭 참여가 제한되어 있습니다."),
    MATCHING_GROUP_ACCESS_DENIED(HttpStatus.FORBIDDEN, "MATCHING_403_3", "해당 매칭 결과에 접근할 권한이 없습니다."),
    TEAM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "MATCHING_403_4", "해당 팀에 대한 접근 권한이 없습니다."),
    MATCHING_RESULT_NOT_PUBLISHED(HttpStatus.FORBIDDEN, "MATCHING_403_5", "아직 매칭 결과가 공개되지 않았습니다."),

    // 404 Not Found
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "MATCHING_404_1", "매칭 신청을 찾을 수 없습니다."),
    MATCHING_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "MATCHING_404_2", "매칭 결과를 찾을 수 없습니다."),
    MATCHING_REASON_NOT_FOUND(HttpStatus.NOT_FOUND, "MATCHING_404_3", "AI 추천 사유를 찾을 수 없습니다."),
    EXPLANATION_NOT_FOUND(HttpStatus.NOT_FOUND, "MATCHING_404_4", "AI 분석 매칭 설명을 찾을 수 없습니다."),
    LEADER_RECOMMENDATION_NOT_FOUND(HttpStatus.NOT_FOUND, "MATCHING_404_5", "팀 또는 팀장 추천 결과를 찾을 수 없습니다."),
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "MATCHING_404_6", "팀을 찾을 수 없습니다."),

    // 409 Conflict
    ALREADY_APPLIED_TODAY(HttpStatus.CONFLICT, "MATCHING_409_1", "매칭풀에는 하루에 한 번만 입장할 수 있습니다."),
    INVALID_APPLICATION_STATUS(HttpStatus.CONFLICT, "MATCHING_409_2", "현재 신청 상태에서는 해당 요청을 처리할 수 없습니다."),
    MATCHING_REASON_IN_PROGRESS(HttpStatus.CONFLICT, "MATCHING_409_3", "해당 매칭 결과의 추천 사유가 이미 생성 중입니다."),
    LEADER_RECOMMENDATION_IN_PROGRESS(HttpStatus.CONFLICT, "MATCHING_409_4", "해당 팀의 팀장 추천이 이미 진행 중입니다."),
    MATCHING_RESPONSE_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "MATCHING_409_5", "이미 제출한 매칭 응답은 변경할 수 없습니다."),
    MATCHING_GROUP_ALREADY_CLOSED(HttpStatus.CONFLICT, "MATCHING_409_6", "이미 종료된 매칭 그룹입니다."),
    MATCHING_TEAM_ALREADY_CREATED(HttpStatus.CONFLICT, "MATCHING_409_7", "해당 매칭 그룹의 팀이 이미 생성되었습니다."),
    MATCHING_REASSIGNMENT_CONFLICT(HttpStatus.CONFLICT, "MATCHING_409_8", "자동 재매칭 신청과 기존 신청이 충돌했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
