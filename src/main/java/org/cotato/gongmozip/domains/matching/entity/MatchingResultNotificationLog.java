package org.cotato.gongmozip.domains.matching.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.global.entity.BaseEntity;

/**
 * 매칭 결과 공개 알림(MatchingResultNotificationJobs)을 하루에 한 번만 보내기 위한 멱등성 기록이다.
 * 신청일당 한 행만 존재하며, 존재 여부로 "오늘 이미 알림을 보냈는지"를 판단한다
 * (docs/decisions/11-notification.md — 공개 시각을 cron에 하드코딩하지 않고 5분마다
 * MatchingTimePolicy.isResultPublished로 직접 확인하는 방식으로 바꾸면서 필요해졌다).
 */
@Getter
@Entity
@Table(
        name = "matching_result_notification_logs",
        uniqueConstraints =
                @UniqueConstraint(name = "uq_matching_result_notification_logs_date", columnNames = "application_date"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MatchingResultNotificationLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "matching_result_notification_log_id", nullable = false, updatable = false)
    private Long matchingResultNotificationLogId;

    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;
}
