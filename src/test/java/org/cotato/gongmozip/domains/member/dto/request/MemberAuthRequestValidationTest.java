package org.cotato.gongmozip.domains.member.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.cotato.gongmozip.domains.member.dto.request.MemberAuthRequest.RegisterRequiredInfoRequest;
import org.cotato.gongmozip.domains.member.dto.request.MemberAuthRequest.SignUpRequest;
import org.cotato.gongmozip.domains.member.enums.Gender;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberAuthRequestValidationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 28);
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        validatorFactory = Validation.byDefaultProvider()
                .configure()
                .clockProvider(() -> clock)
                .buildValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @DisplayName("회원가입과 소셜 필수 정보 등록 시 미래 생년월일을 허용하지 않는다.")
    @Test
    void 미래_생년월일은_허용하지_않는다() {
        LocalDate futureBirthDate = TODAY.plusDays(1);

        assertInvalidBirthDate(signUpRequest(futureBirthDate), "생년월일은 미래일 수 없습니다.");
        assertInvalidBirthDate(registerRequiredInfoRequest(futureBirthDate), "생년월일은 미래일 수 없습니다.");
    }

    @DisplayName("회원가입과 소셜 필수 정보 등록 시 만 14세 미만 생년월일을 허용하지 않는다.")
    @Test
    void 만_14세_미만_생년월일은_허용하지_않는다() {
        LocalDate underAgeBirthDate = TODAY.minusYears(14).plusDays(1);

        assertInvalidBirthDate(signUpRequest(underAgeBirthDate), "만 14세 이상만 가입할 수 있습니다.");
        assertInvalidBirthDate(registerRequiredInfoRequest(underAgeBirthDate), "만 14세 이상만 가입할 수 있습니다.");
    }

    @DisplayName("회원가입과 소셜 필수 정보 등록 시 만 14세가 되는 당일부터 허용한다.")
    @Test
    void 만_14세가_되는_당일부터_허용한다() {
        LocalDate minimumBirthDate = TODAY.minusYears(14);

        assertThat(validator.validate(signUpRequest(minimumBirthDate))).isEmpty();
        assertThat(validator.validate(registerRequiredInfoRequest(minimumBirthDate))).isEmpty();
    }

    private SignUpRequest signUpRequest(LocalDate birthDate) {
        return new SignUpRequest("user@gongmozip.com", "password123!", Gender.MALE, birthDate);
    }

    private RegisterRequiredInfoRequest registerRequiredInfoRequest(LocalDate birthDate) {
        return new RegisterRequiredInfoRequest(Gender.MALE, birthDate);
    }

    private void assertInvalidBirthDate(Object request, String expectedMessage) {
        assertThat(validator.validate(request))
                .extracting(ConstraintViolation::getMessage)
                .containsExactly(expectedMessage);
    }
}
