package org.cotato.gongmozip.domains.matching.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = "오늘의 매칭 결과 상태: NOT_APPLIED=신청 없음, NOT_PUBLISHED=공개 전, PROCESSING=공개 시각 이후에도 처리 중, "
                + "MATCHED=팀 배정, UNMATCHED=미배정, WITHDRAWN=취소 또는 패스")
public enum MatchingResultStatus {
    NOT_APPLIED,
    NOT_PUBLISHED,
    PROCESSING,
    MATCHED,
    UNMATCHED,
    WITHDRAWN
}
