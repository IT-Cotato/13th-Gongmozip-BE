package org.cotato.gongmozip.domains.contest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(
        name = "contest_votes",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_contest_votes_candidate_voter_round",
                    columnNames = {"contest_candidate_id", "voter_team_member_id", "round"})
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ContestVote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contest_vote_id", nullable = false, updatable = false)
    private Long contestVoteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_candidate_id", nullable = false)
    private ContestCandidate contestCandidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_team_member_id", nullable = false)
    private TeamMember voterTeamMember;

    @Builder.Default
    @Column(name = "round", nullable = false)
    private int round = 1;
}
