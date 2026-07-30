package org.cotato.gongmozip.domains.review.repository;

import org.cotato.gongmozip.domains.review.entity.Review;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByTeam_TeamIdAndReviewer_TeamMemberIdAndReviewee_TeamMemberId(
            Long teamId, Long reviewerTeamMemberId, Long revieweeTeamMemberId);

    long countByTeam_TeamIdAndReviewer_StatusAndReviewee_Status(
            Long teamId, TeamMemberStatus reviewerStatus, TeamMemberStatus revieweeStatus);
}
