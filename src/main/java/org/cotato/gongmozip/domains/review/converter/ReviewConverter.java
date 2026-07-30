package org.cotato.gongmozip.domains.review.converter;

import org.cotato.gongmozip.domains.review.dto.response.ReviewResponse.ReviewResultResponse;
import org.cotato.gongmozip.domains.review.entity.Review;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;

public final class ReviewConverter {

    private ReviewConverter() {}

    public static Review toReview(Team team, TeamMember reviewer, TeamMember reviewee, String content) {
        return Review.builder()
                .team(team)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .content(content)
                .build();
    }

    public static ReviewResultResponse toReviewResultResponse(Review review) {
        return new ReviewResultResponse(
                review.getReviewId(),
                review.getReviewee().getTeamMemberId(),
                review.getContent(),
                review.getCreatedAt());
    }
}
