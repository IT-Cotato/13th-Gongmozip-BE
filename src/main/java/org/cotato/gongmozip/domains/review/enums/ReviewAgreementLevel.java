package org.cotato.gongmozip.domains.review.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "팀원 리뷰 3점 척도 응답: DISAGREE=아니다, NEUTRAL=보통이다, AGREE=그렇다")
public enum ReviewAgreementLevel {
    DISAGREE,
    NEUTRAL,
    AGREE
}
