package org.cotato.gongmozip.domains.team.entity;

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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.cotato.gongmozip.domains.team.enums.LeaderCandidacyStatus;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.enums.TeamRole;
import org.cotato.gongmozip.global.entity.BaseEntity;
import org.hibernate.annotations.DynamicUpdate;

@Getter
@Entity
@DynamicUpdate
@Table(
        name = "team_members",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_team_members_team_member",
                    columnNames = {"team_id", "member_id"})
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class TeamMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_member_id", nullable = false, updatable = false)
    private Long teamMemberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 팀 생성 시점(매칭 신청)에 사용된 프로필 스냅샷. 회원이 여러 프로필을 가질 수 있어
    // "이 팀에서는 어떤 프로필로 참여했는지"를 고정한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    // 팀 생성 시점(매칭 신청)의 팀장 희망 여부 스냅샷. leaderSelectionMode 판정과 팀장 추천
    // 알고리즘(docs/decisions/02-leader-election.md)에 쓰인다.
    @Enumerated(EnumType.STRING)
    @Column(name = "leader_preference", nullable = false, length = 30)
    private LeaderPreference leaderPreference;

    // 팀 생성 시점의 협업 유형 검사 외향성 유형(I/A/E) 스냅샷. 팀장 추천 알고리즘의
    // "잔여 팀원 다수 유형" 판정에 쓰인다.
    @Enumerated(EnumType.STRING)
    @Column(name = "extroversion_type", nullable = false, length = 1)
    private ExtroversionType extroversionType;

    // 외향성 원점수(3문항 평균, 1.0~5.0). 팀장 추천 동률 처리 3순위(원점수 3~15점 환산,
    // 이 값 * 3)에 쓰인다.
    @Column(name = "extroversion_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal extroversionScore;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private TeamRole role = TeamRole.MEMBER;

    @Column(name = "is_pre_leader_candidate", nullable = false)
    private boolean isPreLeaderCandidate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "leader_candidacy", nullable = false, length = 20)
    private LeaderCandidacyStatus leaderCandidacy = LeaderCandidacyStatus.UNDECIDED;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TeamMemberStatus status = TeamMemberStatus.ACTIVE;

    @Builder.Default
    @Column(name = "is_completed_project_deleted", nullable = false)
    private boolean isCompletedProjectDeleted = false;

    @Column(name = "greeted_at")
    private LocalDateTime greetedAt;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    public void leave(LocalDateTime leftAt) {
        this.status = TeamMemberStatus.LEFT;
        this.leftAt = leftAt;
    }

    public void markRead(LocalDateTime readAt) {
        this.lastReadAt = readAt;
    }

    public void markGreeted(LocalDateTime greetedAt) {
        this.greetedAt = greetedAt;
    }

    public void updateLeaderCandidacy(LeaderCandidacyStatus leaderCandidacy) {
        this.leaderCandidacy = leaderCandidacy;
    }

    public void assignAsLeader() {
        this.role = TeamRole.LEADER;
    }

    public void deleteCompletedProjectRecord() {
        this.isCompletedProjectDeleted = true;
    }
}
