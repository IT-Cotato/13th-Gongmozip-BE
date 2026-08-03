package org.cotato.gongmozip.domains.matching.algorithm.model.result;

import java.math.BigDecimal;

/**
 * 총점뿐 아니라 팀 구성 판단의 각 근거를 결과 저장 계층까지 전달하기 위해 만든 점수 모델이다.
 * 세부 점수를 함께 저장하면 이후 결과 설명이나 가중치 검증 시 알고리즘을 다시 실행하지 않아도 된다.
 */
public record TeamCompatibilityScore(
        BigDecimal leaderHarmonyScore,
        BigDecimal goalSimilarityScore,
        BigDecimal workStyleSimilarityScore,
        BigDecimal communicationSimilarityScore,
        BigDecimal agreeablenessSimilarityScore,
        BigDecimal conscientiousnessSimilarityScore,
        BigDecimal honestyHumilitySimilarityScore,
        BigDecimal extroversionComplementScore,
        BigDecimal totalScore) {}
