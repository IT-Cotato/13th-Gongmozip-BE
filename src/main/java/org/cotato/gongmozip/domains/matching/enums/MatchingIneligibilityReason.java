package org.cotato.gongmozip.domains.matching.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/** 신청 가능 여부 API가 여러 검증 실패를 안정된 코드 목록으로 전달할 수 있게 정의한 사유다. */
@Schema(
        description =
                "매칭 신청 불가 사유: PROFILE_REQUIRED=프로필 필요, SURVEY_REQUIRED=협업 유형 검사 필요, APPLICATION_DEADLINE_PASSED=매칭 진행 중(14~16시) 신청 불가, ALREADY_APPLIED_TODAY=대상 신청일 신청 이력 존재, MATCHING_RESTRICTED=협업거리 감소로 참여 제한, PROJECT_EVALUATION_NOT_READY=프로젝트 AI 평가 미완료, REASSIGNMENT_PENDING=이전 결과 응답 또는 자동 재배정 대기")
public enum MatchingIneligibilityReason {
    PROFILE_REQUIRED,
    SURVEY_REQUIRED,
    APPLICATION_DEADLINE_PASSED,
    ALREADY_APPLIED_TODAY,
    MATCHING_RESTRICTED,
    // 프로젝트가 있는 프로필 중 저장된 AI 평가가 모두 준비된 프로필이 하나도 없는 경우다.
    PROJECT_EVALUATION_NOT_READY,
    REASSIGNMENT_PENDING
}
