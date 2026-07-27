package org.cotato.gongmozip.domains.survey.vo;

import java.math.BigDecimal;
import lombok.Builder;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;

@Builder
public record SurveyScoreSnapshot(
        BigDecimal agreeablenessScore,
        BigDecimal conscientiousnessScore,
        BigDecimal honestyHumilityScore,
        BigDecimal extroversionScore,
        BigDecimal goalPreferenceScore,
        BigDecimal workStyleScore,
        BigDecimal communicationStyleScore,
        BigDecimal extroversion2Score,
        BigDecimal extroversion3Score,
        ExtroversionType extroversionType,
        CharacterType characterType,
        BigDecimal characterXScore,
        BigDecimal characterYScore) {}
