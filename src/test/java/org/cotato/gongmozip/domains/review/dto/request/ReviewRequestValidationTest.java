package org.cotato.gongmozip.domains.review.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Arrays;
import java.util.List;
import org.cotato.gongmozip.domains.review.dto.request.ReviewRequest.WriteReviewRequest;
import org.cotato.gongmozip.domains.review.enums.ReviewAgreementLevel;
import org.cotato.gongmozip.domains.review.enums.ReviewKeyword;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @DisplayName("키워드 목록에 null 원소가 있으면 다른 필드가 유효해도 검증에 실패한다.")
    @Test
    void 키워드_목록에_null_원소가_있으면_검증에_실패한다() {
        WriteReviewRequest request = new WriteReviewRequest(
                20L,
                ReviewAgreementLevel.AGREE,
                ReviewAgreementLevel.AGREE,
                Arrays.asList(ReviewKeyword.TRUSTWORTHY, null));

        assertThat(validator.validate(request))
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("키워드 목록에 빈 값을 포함할 수 없습니다.");
    }

    @DisplayName("키워드를 하나도 선택하지 않으면 검증에 실패한다.")
    @Test
    void 키워드가_비어있으면_검증에_실패한다() {
        WriteReviewRequest request =
                new WriteReviewRequest(20L, ReviewAgreementLevel.AGREE, ReviewAgreementLevel.AGREE, List.of());

        assertThat(validator.validate(request))
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("팀원을 표현하는 키워드를 최소 1개 선택해야 합니다.");
    }

    @DisplayName("유효한 요청은 검증을 통과한다.")
    @Test
    void 유효한_요청은_검증을_통과한다() {
        WriteReviewRequest request = new WriteReviewRequest(
                20L, ReviewAgreementLevel.AGREE, ReviewAgreementLevel.DISAGREE, List.of(ReviewKeyword.TRUSTWORTHY));

        assertThat(validator.validate(request)).isEmpty();
    }
}
