package org.cotato.gongmozip.domains.matching.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description =
                "매칭 신청 불가 사유: PROFILE_REQUIRED=프로필 필요, SURVEY_REQUIRED=협업 유형 검사 필요, APPLICATION_DEADLINE_PASSED=14시 마감 경과, ALREADY_APPLIED_TODAY=오늘 신청 이력 존재, MATCHING_RESTRICTED=협업거리 감소로 참여 제한")
public enum MatchingIneligibilityReason {
    PROFILE_REQUIRED,
    SURVEY_REQUIRED,
    APPLICATION_DEADLINE_PASSED,
    ALREADY_APPLIED_TODAY,
    MATCHING_RESTRICTED
}
