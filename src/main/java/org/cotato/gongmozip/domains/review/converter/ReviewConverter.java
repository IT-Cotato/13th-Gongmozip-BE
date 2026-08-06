package org.cotato.gongmozip.domains.review.converter;

import java.util.List;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.MemberAvatarResponse;
import org.cotato.gongmozip.domains.review.dto.request.ReviewRequest.WriteReviewRequest;
import org.cotato.gongmozip.domains.review.dto.response.ReviewResponse.ReviewResultResponse;
import org.cotato.gongmozip.domains.review.dto.response.ReviewResponse.ReviewTargetResponse;
import org.cotato.gongmozip.domains.review.entity.Review;
import org.cotato.gongmozip.domains.review.enums.ReviewKeyword;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;

public final class ReviewConverter {

    private ReviewConverter() {}

    public static Review toReview(Team team, TeamMember reviewer, TeamMember reviewee, WriteReviewRequest request) {
        return Review.builder()
                .team(team)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .communicationScore(request.communicationScore())
                .participationScore(request.participationScore())
                .keywords(request.keywords().stream().map(Enum::name).toList())
                .build();
    }

    public static ReviewResultResponse toReviewResultResponse(Review review) {
        return new ReviewResultResponse(
                review.getReviewId(),
                review.getReviewee().getTeamMemberId(),
                review.getCommunicationScore(),
                review.getParticipationScore(),
                toKeywords(review.getKeywords()),
                review.getCreatedAt());
    }

    private static List<ReviewKeyword> toKeywords(List<String> keywords) {
        return keywords.stream().map(ReviewKeyword::valueOf).toList();
    }

    public static ReviewTargetResponse toReviewTargetResponse(
            TeamMember target, boolean alreadyReviewed, MemberAvatarResponse avatar) {
        return new ReviewTargetResponse(
                target.getTeamMemberId(),
                target.getMember().getMemberId(),
                target.getProfile().getProfileId(),
                target.getProfile().getNickname(),
                target.getRole().name(),
                avatar,
                alreadyReviewed);
    }
}
