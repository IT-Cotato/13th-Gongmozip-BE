package org.cotato.gongmozip.domains.survey.enums;

public enum SubmissionStatus {
    IN_PROGRESS, // 작성 중
    SUBMITTED, // 제출 완료 (현재 유효한 제출, 회원당 1개 보장)
    SUPERSEDED // 재검사로 대체됨 (이력 보존용, 여러 개 가능)
}
