package org.cotato.gongmozip.domains.review.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.cotato.gongmozip.domains.review.enums.ReviewAgreementLevel;
import org.cotato.gongmozip.domains.review.enums.ReviewKeyword;

public final class ReviewRequest {

    private ReviewRequest() {}

    public record WriteReviewRequest(
            @NotNull(message = "리뷰 대상 팀원은 필수 입력 항목입니다.") Long revieweeTeamMemberId,
            @NotNull(message = "소통이 원활했는지 응답은 필수 입력 항목입니다.") ReviewAgreementLevel communicationScore,
            @NotNull(message = "프로젝트에 적극적으로 참여했는지 응답은 필수 입력 항목입니다.") ReviewAgreementLevel participationScore,
            @NotEmpty(message = "팀원을 표현하는 키워드를 최소 1개 선택해야 합니다.") List<ReviewKeyword> keywords) {}
}
