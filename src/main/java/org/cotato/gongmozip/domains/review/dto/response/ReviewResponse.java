package org.cotato.gongmozip.domains.review.dto.response;

import java.time.LocalDateTime;

public final class ReviewResponse {

    private ReviewResponse() {}

    public record ReviewResultResponse(
            Long reviewId, Long revieweeTeamMemberId, String content, LocalDateTime createdAt) {}
}
