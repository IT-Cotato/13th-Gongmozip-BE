package org.cotato.gongmozip.domains.survey.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "survey_options")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SurveyOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id", nullable = false, updatable = false)
    private Long optionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private SurveyQuestion question;

    // 선택지를 코드로 식별하는 키 (예: LEADER_YES, RATING_3)
    @Column(name = "option_key", nullable = false, length = 100)
    private String optionKey;

    // 화면에 표시되는 텍스트
    @Column(name = "option_label", nullable = false, length = 255)
    private String optionLabel;

    // 내부 처리용 값 (예: "1", "0.5", "0")
    @Column(name = "option_value", nullable = false, length = 255)
    private String optionValue;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    // 점수 계산에 사용되는 가중치. 역채점 문항은 역방향 값으로 미리 설정
    @Column(name = "score_weight", precision = 12, scale = 2)
    private BigDecimal scoreWeight;

    @Column(name = "is_other_option", nullable = false)
    private boolean isOtherOption;
}
