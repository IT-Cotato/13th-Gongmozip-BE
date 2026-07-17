package org.cotato.gongmozip.domains.contest.entity;

import jakarta.persistence.*;
import lombok.*;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(
        name = "contest_scrap",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_contest_scrap_member_contest",
                    columnNames = {"member_id", "contest_id"})
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ContestScrap extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contest_scrap_id", nullable = false, updatable = false)
    private Long contestScrapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id", nullable = false)
    private Contest contest;
}
