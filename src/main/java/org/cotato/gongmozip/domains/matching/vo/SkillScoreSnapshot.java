package org.cotato.gongmozip.domains.matching.vo;

import java.math.BigDecimal;

/**
 * 역량 계산기의 여러 항목 점수와 가중 합산 결과가 서로 흩어지지 않도록 하나로 전달하기 위한 불변 값 객체다.
 * 신청 엔티티가 이 값을 그대로 저장해 원본 프로필이 바뀌어도 신청 시점의 계산 근거를 보존한다. 유효 풀은 14시 배치에서
 * 결정되므로 이 값에는 포함하지 않는다.
 */
public record SkillScoreSnapshot(
        BigDecimal gpaScore,
        BigDecimal projectScore,
        BigDecimal awardScore,
        BigDecimal certificationScore,
        BigDecimal collaborationScore,
        BigDecimal totalScore,
        boolean firstMatching) {}
