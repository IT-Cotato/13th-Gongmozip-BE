package org.cotato.gongmozip.domains.review.repository;

import java.util.List;
import org.cotato.gongmozip.domains.review.entity.Review;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByTeam_TeamIdAndReviewer_TeamMemberIdAndReviewee_TeamMemberId(
            Long teamId, Long reviewerTeamMemberId, Long revieweeTeamMemberId);

    long countByTeam_TeamIdAndReviewer_StatusAndReviewee_Status(
            Long teamId, TeamMemberStatus reviewerStatus, TeamMemberStatus revieweeStatus);

    long countByTeam_TeamIdAndReviewer_TeamMemberIdAndReviewee_Status(
            Long teamId, Long reviewerTeamMemberId, TeamMemberStatus revieweeStatus);

    List<Review> findByTeam_TeamIdAndReviewer_TeamMemberId(Long teamId, Long reviewerTeamMemberId);
}
