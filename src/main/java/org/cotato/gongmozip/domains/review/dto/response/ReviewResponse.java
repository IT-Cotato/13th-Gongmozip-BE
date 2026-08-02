package org.cotato.gongmozip.domains.review.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.MemberAvatarResponse;

public final class ReviewResponse {

    private ReviewResponse() {}

    public record ReviewResultResponse(
            Long reviewId, Long revieweeTeamMemberId, String content, LocalDateTime createdAt) {}

    public record ReviewTargetResponse(
            Long teamMemberId,
            Long memberId,
            Long profileId,
            String nickname,
            String role,
            MemberAvatarResponse avatar,
            boolean alreadyReviewed) {}

    public record ReviewTargetListResponse(List<ReviewTargetResponse> targets) {}
}
