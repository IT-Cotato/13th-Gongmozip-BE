package org.cotato.gongmozip.domains.survey.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.survey.enums.QuestionType;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "survey_questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SurveyQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id", nullable = false, updatable = false)
    private Long questionId;

    // 문항을 코드로 식별하는 키 (예: AGREEABLENESS_1, LEADER_PREFERENCE)
    @Column(name = "question_key", nullable = false, length = 100)
    private String questionKey;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    // SINGLE_CHOICE: 선택지 중 하나 선택 / RATING: 5점 척도
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private QuestionType questionType;

    @Column(name = "is_required", nullable = false)
    private boolean isRequired;

    // true이면 점수 계산 시 역채점 적용 (score_weight가 역방향으로 설정된 선택지 사용)
    @Column(name = "is_reverse_scored", nullable = false)
    private boolean isReverseScored;

    // 화면 표시 순서
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "help_text", columnDefinition = "TEXT")
    private String helpText;

    @Column(name = "placeholder_text", length = 255)
    private String placeholderText;

    // RATING 문항의 척도 범위 (예: min=1, max=5)
    @Column(name = "min_value", precision = 12, scale = 2)
    private BigDecimal minValue;

    @Column(name = "max_value", precision = 12, scale = 2)
    private BigDecimal maxValue;
}
