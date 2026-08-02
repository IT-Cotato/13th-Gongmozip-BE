package org.cotato.gongmozip.domains.matching.vo;

import java.math.BigDecimal;

// 신청 시점 역량 계산 결과 — 항목별 원점수와 가중치 적용 총점·그룹을 함께 보존한다
public record SkillScoreSnapshot(
        BigDecimal gpaScore,
        BigDecimal projectScore,
        BigDecimal awardScore,
        BigDecimal certificationScore,
        BigDecimal collaborationScore,
        BigDecimal totalScore,
        int skillGroup,
        boolean firstMatching) {}
