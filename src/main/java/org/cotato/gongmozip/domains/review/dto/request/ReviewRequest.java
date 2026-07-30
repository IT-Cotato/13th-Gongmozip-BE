package org.cotato.gongmozip.domains.review.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ReviewRequest {

    private ReviewRequest() {}

    public record WriteReviewRequest(
            @NotNull(message = "리뷰 대상 팀원은 필수 입력 항목입니다.") Long revieweeTeamMemberId,
            @NotBlank(message = "리뷰 내용은 필수 입력 항목입니다.") @Size(max = 1000, message = "리뷰는 최대 1000자까지 입력 가능합니다.")
                    String content) {}
}
