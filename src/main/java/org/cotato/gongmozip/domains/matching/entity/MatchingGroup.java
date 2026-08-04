package org.cotato.gongmozip.domains.matching.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupStatus;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.global.entity.BaseEntity;

/**
 * 알고리즘이 제안한 한 팀과 호환도 계산 근거를 영속 결과로 남기기 위해 확장한 엔티티다.
 * 배치를 직접 참조하고 총점·세부 점수를 함께 저장해 이후 결과 공개와 설명 생성에서 재계산이 필요 없게 한다.
 */
@Getter
@Entity
@Table(name = "matching_groups")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MatchingGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "matching_group_id", nullable = false, updatable = false)
    private Long matchingGroupId;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "matching_batch_id")
    private MatchingBatch matchingBatch;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private InterestCategory category;

    @Column(name = "skill_group", nullable = false)
    private Integer skillGroup;

    // 현재 알고리즘이 허용하는 3인 또는 4인 팀 크기를 결과 시점에 고정한다.
    @Column(name = "team_size", nullable = false)
    private Integer teamSize;

    @Column(name = "matching_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal matchingScore;

    // 아래 항목별 점수는 총점의 근거이며 후속 결과 설명에서 사용한다.
    @Column(name = "leader_harmony_score", precision = 5, scale = 2)
    private BigDecimal leaderHarmonyScore;

    @Column(name = "goal_similarity_score", precision = 5, scale = 2)
    private BigDecimal goalSimilarityScore;

    @Column(name = "work_style_similarity_score", precision = 5, scale = 2)
    private BigDecimal workStyleSimilarityScore;

    @Column(name = "communication_similarity_score", precision = 5, scale = 2)
    private BigDecimal communicationSimilarityScore;

    @Column(name = "agreeableness_similarity_score", precision = 5, scale = 2)
    private BigDecimal agreeablenessSimilarityScore;

    @Column(name = "conscientiousness_similarity_score", precision = 5, scale = 2)
    private BigDecimal conscientiousnessSimilarityScore;

    @Column(name = "honesty_humility_similarity_score", precision = 5, scale = 2)
    private BigDecimal honestyHumilitySimilarityScore;

    @Column(name = "extroversion_complement_score", precision = 5, scale = 2)
    private BigDecimal extroversionComplementScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MatchingGroupStatus status;
}
