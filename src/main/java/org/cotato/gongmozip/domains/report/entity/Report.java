package org.cotato.gongmozip.domains.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.report.enums.ReportReason;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id", nullable = false, updatable = false)
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_member_id", nullable = false)
    private Member reporterMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_member_id", nullable = false)
    private Member reportedMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 30)
    private ReportReason reasonCode;

    @Column(name = "custom_reason_text", length = 500)
    private String customReasonText;
}
