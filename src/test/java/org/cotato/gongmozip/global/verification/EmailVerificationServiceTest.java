package org.cotato.gongmozip.global.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.util.concurrent.TimeUnit;
import org.cotato.gongmozip.global.redis.RedisUtil;
import org.cotato.gongmozip.global.verification.EmailVerificationService.Purpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    private static final String EMAIL = "test@gongmozip.com";
    private static final String CODE = "123456";

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailVerificationService, "mailUsername", "no-reply@gongmozip.com");
    }

    @Test
    @DisplayName("용도별 Redis 키에 6자리 인증코드를 저장하고 이메일을 전송한다.")
    void sendCode() {
        emailVerificationService.sendCode(Purpose.SIGN_UP, EMAIL, "회원가입 인증코드");

        then(redisUtil)
                .should()
                .set(
                        eq("email:verify:" + EMAIL),
                        argThat(code -> code.matches("\\d{6}")),
                        eq(5L),
                        eq(TimeUnit.MINUTES));
        then(redisUtil).should().set("email:verify:issued:" + EMAIL, "true", 10L, TimeUnit.MINUTES);
        then(redisUtil).should().delete("email:verify:fail:" + EMAIL);
        then(mailSender).should().send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("메일 전송에 실패하면 저장한 코드와 발급 이력을 삭제한다.")
    void sendCode_mailFailure() {
        willThrow(new MailSendException("SMTP timeout")).given(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailVerificationService.sendCode(Purpose.PASSWORD_RESET, EMAIL, "비밀번호 재설정 인증코드"))
                .isInstanceOf(MailSendException.class);

        then(redisUtil).should().delete("password-reset:code:" + EMAIL);
        then(redisUtil).should().delete("password-reset:code:issued:" + EMAIL);
    }

    @Test
    @DisplayName("틀린 코드 제출 후 실패 횟수가 5회에 도달하면 시도 횟수 초과 결과를 반환한다.")
    void verifyCode_tooManyAttempts() {
        given(redisUtil.get("email:verify:fail:" + EMAIL)).willReturn(null);
        given(redisUtil.get("email:verify:" + EMAIL)).willReturn("999999");
        given(redisUtil.incrementWithTtl("email:verify:fail:" + EMAIL, 5L, TimeUnit.MINUTES))
                .willReturn(5L);

        EmailVerificationResult result = emailVerificationService.verifyCode(Purpose.SIGN_UP, EMAIL, CODE);

        assertThat(result).isEqualTo(EmailVerificationResult.TOO_MANY_ATTEMPTS);
    }

    @Test
    @DisplayName("인증에 5회 실패한 뒤 올바른 코드를 입력해도 시도 횟수 초과 결과를 반환한다.")
    void verifyCode_correctCodeAfterTooManyAttempts() {
        given(redisUtil.get("email:verify:fail:" + EMAIL)).willReturn("5");

        EmailVerificationResult result = emailVerificationService.verifyCode(Purpose.SIGN_UP, EMAIL, CODE);

        assertThat(result).isEqualTo(EmailVerificationResult.TOO_MANY_ATTEMPTS);
        then(redisUtil).should(never()).get("email:verify:" + EMAIL);
    }

    @Test
    @DisplayName("코드가 없고 발급 이력도 없으면 미발급 결과를 반환한다.")
    void verifyCode_notIssued() {
        given(redisUtil.get("email:verify:fail:" + EMAIL)).willReturn(null);
        given(redisUtil.get("email:verify:" + EMAIL)).willReturn(null);
        given(redisUtil.exists("email:verify:issued:" + EMAIL)).willReturn(false);

        EmailVerificationResult result = emailVerificationService.verifyCode(Purpose.SIGN_UP, EMAIL, CODE);

        assertThat(result).isEqualTo(EmailVerificationResult.CODE_NOT_ISSUED);
    }

    @Test
    @DisplayName("코드는 없고 발급 이력만 있으면 만료 결과를 반환한다.")
    void verifyCode_expired() {
        given(redisUtil.get("password-reset:code:fail:" + EMAIL)).willReturn(null);
        given(redisUtil.get("password-reset:code:" + EMAIL)).willReturn(null);
        given(redisUtil.exists("password-reset:code:issued:" + EMAIL)).willReturn(true);

        EmailVerificationResult result = emailVerificationService.verifyCode(Purpose.PASSWORD_RESET, EMAIL, CODE);

        assertThat(result).isEqualTo(EmailVerificationResult.EXPIRED_CODE);
    }

    @Test
    @DisplayName("코드가 일치하지 않으면 실패 횟수를 증가시키고 불일치 결과를 반환한다.")
    void verifyCode_invalid() {
        given(redisUtil.get("password-reset:code:fail:" + EMAIL)).willReturn(null);
        given(redisUtil.get("password-reset:code:" + EMAIL)).willReturn(CODE);
        given(redisUtil.incrementWithTtl("password-reset:code:fail:" + EMAIL, 5L, TimeUnit.MINUTES))
                .willReturn(1L);

        EmailVerificationResult result = emailVerificationService.verifyCode(Purpose.PASSWORD_RESET, EMAIL, "000000");

        assertThat(result).isEqualTo(EmailVerificationResult.INVALID_CODE);
        then(redisUtil).should().incrementWithTtl("password-reset:code:fail:" + EMAIL, 5L, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("코드가 일치하면 관련 Redis 상태를 정리하고 성공 결과를 반환한다.")
    void verifyCode_success() {
        given(redisUtil.get("email:verify:fail:" + EMAIL)).willReturn(null);
        given(redisUtil.get("email:verify:" + EMAIL)).willReturn(CODE);

        EmailVerificationResult result = emailVerificationService.verifyCode(Purpose.SIGN_UP, EMAIL, CODE);

        assertThat(result).isEqualTo(EmailVerificationResult.VERIFIED);
        then(redisUtil).should().delete("email:verify:" + EMAIL);
        then(redisUtil).should().delete("email:verify:issued:" + EMAIL);
        then(redisUtil).should().delete("email:verify:fail:" + EMAIL);
    }
}
