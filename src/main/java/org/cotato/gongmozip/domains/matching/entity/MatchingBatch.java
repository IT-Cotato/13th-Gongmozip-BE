package org.cotato.gongmozip.domains.matching.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.matching.algorithm.model.result.MatchingPlan;
import org.cotato.gongmozip.domains.matching.enums.MatchingAlgorithmType;
import org.cotato.gongmozip.domains.matching.enums.MatchingBatchStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupingMode;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.global.entity.BaseEntity;

/**
 * 특정 날짜·카테고리·유효 풀의 실행 상태와 재현 정보를 한 행으로 관리하기 위해 만든 엔티티다.
 * 풀 분류 근거, 최초·최종 알고리즘, 시드, 실행시간과 실패 이력을 보존해 재시도와 운영 분석이 가능하게 한다.
 */
@Getter
@Entity
@Table(
        name = "matching_batches",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_matching_batches_pool",
                        columnNames = {"application_date", "category", "skill_group"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MatchingBatch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "matching_batch_id", nullable = false, updatable = false)
    private Long matchingBatchId;

    // 어떤 날짜의 매칭 신청을 처리한 배치인지 나타낸다.
    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;

    // 카테고리가 다른 신청자는 섞이지 않으므로 배치도 카테고리별로 생성한다.
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private InterestCategory category;

    // 카테고리 안에서 분할·병합이 끝난 유효 풀 번호다. DB 물리 컬럼명은 호환성을 위해 skill_group을 사용한다.
    // 전체 풀 번호가 아니므로 값은 항상 1~4 범위다.
    @Column(name = "skill_group", nullable = false)
    private Integer poolOrdinal;

    // 카테고리 단일 풀인지, 정상 4분위인지, 인접 분위가 병합된 풀인지 기록한다.
    @Enumerated(EnumType.STRING)
    @Column(name = "grouping_mode", nullable = false, length = 30)
    private MatchingGroupingMode groupingMode;

    // 이 유효 풀이 원래 몇 분위부터 몇 분위까지 포함하는지 기록한다.
    // 예: 2·3분위가 병합된 풀은 from=2, to=3이다.
    @Column(name = "source_quartile_from", nullable = false)
    private Integer sourceQuartileFrom;

    @Column(name = "source_quartile_to", nullable = false)
    private Integer sourceQuartileTo;

    // 배치 생명주기: PENDING → RUNNING → SUCCEEDED 또는 FAILED
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private MatchingBatchStatus status = MatchingBatchStatus.PENDING;

    // 풀 인원수를 기준으로 배치 시작 시 선택한 알고리즘이다.
    // Brute Force가 시간 초과되어 Greedy로 전환되더라도 최초 선택값은 그대로 남긴다.
    @Enumerated(EnumType.STRING)
    @Column(name = "initially_selected_algorithm", length = 40)
    private MatchingAlgorithmType initiallySelectedAlgorithm;

    // fallback까지 모두 끝난 뒤 실제 최종 결과를 만든 알고리즘이다.
    @Enumerated(EnumType.STRING)
    @Column(name = "final_algorithm", length = 40)
    private MatchingAlgorithmType finalAlgorithm;

    // Greedy 후보 순서를 재현하기 위한 고정 시드다. 같은 풀을 재시도해도 같은 시작 순서를 만든다.
    @Column(name = "random_seed", nullable = false)
    private long randomSeed;

    // 순수 알고리즘 실행에 걸린 총 시간이다. fallback이 발생하면 최초 탐색과 fallback 시간을 함께 반영한다.
    @Column(name = "elapsed_millis", nullable = false)
    private long elapsedMillis;

    // RUNNING으로 전환된 시각이다.
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    // SUCCEEDED 또는 FAILED로 처리가 종료된 시각이다.
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // 사용자에게 결과를 공개하기로 예약한 시각이다. 현재 기본값은 신청일 당일 16시다.
    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    // FAILED 배치를 다시 시작한 횟수다. 최초 실행은 0이며 실패 후 재시작할 때 증가한다.
    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    // 마지막 실패 유형과 메시지를 보존한다. DB 컬럼 과다 사용을 막기 위해 최대 2,000자로 자른다.
    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;

    // 배치를 선점해 계산을 시작할 때 이전 실행 결과를 초기화하고 RUNNING 상태로 전환한다.
    public void start(MatchingAlgorithmType selectedAlgorithm, LocalDateTime startedAt) {
        if (status == MatchingBatchStatus.SUCCEEDED || status == MatchingBatchStatus.RUNNING) {
            throw new IllegalStateException("완료됐거나 실행 중인 매칭 배치는 다시 시작할 수 없습니다.");
        }
        if (status == MatchingBatchStatus.FAILED) {
            retryCount++;
        }
        status = MatchingBatchStatus.RUNNING;
        initiallySelectedAlgorithm = selectedAlgorithm;
        finalAlgorithm = null;
        elapsedMillis = 0;
        this.startedAt = startedAt;
        completedAt = null;
        failureMessage = null;
    }

    // 최종 알고리즘과 순수 실행시간을 기록하고 SUCCEEDED 상태로 전환한다.
    public void succeed(MatchingPlan plan, LocalDateTime completedAt) {
        status = MatchingBatchStatus.SUCCEEDED;
        finalAlgorithm = plan.selectedAlgorithm();
        elapsedMillis = plan.elapsedTime().toMillis();
        this.completedAt = completedAt;
        failureMessage = null;
    }

    // 실패 원인을 남기고 FAILED 상태로 전환한다. 다음 오케스트레이터 실행에서 재시도할 수 있다.
    public void fail(String failureMessage, LocalDateTime completedAt) {
        status = MatchingBatchStatus.FAILED;
        this.failureMessage = abbreviate(failureMessage);
        this.completedAt = completedAt;
    }

    private String abbreviate(String message) {
        // 비정상적으로 긴 예외 메시지가 배치 행 크기를 키우지 않도록 저장 한도에 맞춰 자른다.
        if (message == null) {
            return "알 수 없는 매칭 배치 오류";
        }
        return message.length() <= 2000 ? message : message.substring(0, 2000);
    }
}
