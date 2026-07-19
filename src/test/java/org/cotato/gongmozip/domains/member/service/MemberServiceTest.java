package org.cotato.gongmozip.domains.member.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import org.cotato.gongmozip.domains.member.dto.request.MemberAuthRequest.EmailVerifyConfirmRequest;
import org.cotato.gongmozip.domains.member.dto.request.MemberAuthRequest.EmailVerifyRequest;
import org.cotato.gongmozip.domains.member.dto.request.MemberAuthRequest.SignUpRequest;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.enums.Gender;
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.auth.repository.AuthAccountRepository;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.global.exception.CustomException;
import org.cotato.gongmozip.global.redis.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AuthAccountRepository authAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private MemberService memberService;

    private static final String TEST_EMAIL = "test@gongmozip.com";
    private static final String TEST_CODE = "123456";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(memberService, "mailUsername", "no-reply@gongmozip.com");
    }

    // ========== 이메일 코드 전송 메서드 테스트 ==========

    @DisplayName("이미 가입된 이메일로 인증코드를 요청하면 중복 이메일 예외가 발생한다.")
    @Test
    void 이미_가입된_이메일로_인증코드_요청시_중복_이메일_예외가_발생한다() {
        // given
        EmailVerifyRequest request = new EmailVerifyRequest(TEST_EMAIL);
        given(memberRepository.existsByEmail(TEST_EMAIL)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> memberService.sendVerificationCode(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(MemberErrorCode.DUPLICATE_EMAIL.getMessage());
    }

    @DisplayName("유효한 이메일로 인증코드를 요청하면 Redis에 인증코드와 발급 내역이 저장된다.")
    @Test
    void 유효한_이메일로_인증코드_요청시_Redis에_코드와_발급내역이_저장된다() {
        // given
        EmailVerifyRequest request = new EmailVerifyRequest(TEST_EMAIL);
        given(memberRepository.existsByEmail(TEST_EMAIL)).willReturn(false);

        // when
        memberService.sendVerificationCode(request);

        // then
        then(redisUtil).should().set(eq("email:verify:" + TEST_EMAIL), anyString(), eq(5L), eq(TimeUnit.MINUTES));
        then(redisUtil)
                .should()
                .set(eq("email:verify:issued:" + TEST_EMAIL), eq("true"), eq(10L), eq(TimeUnit.MINUTES));
        then(redisUtil).should().delete("email:verify:fail:" + TEST_EMAIL);
        then(mailSender).should().send(any(SimpleMailMessage.class));
    }

    @DisplayName("이메일 서버 오류로 인증코드 전송이 실패하면 이메일 전송 실패 예외가 발생한다.")
    @Test
    void 이메일_서버_오류로_인증코드_전송_실패시_이메일_전송_실패_예외가_발생한다() {
        // given
        EmailVerifyRequest request = new EmailVerifyRequest(TEST_EMAIL);
        given(memberRepository.existsByEmail(TEST_EMAIL)).willReturn(false);
        willThrow(new MailSendException("SMTP timeout")).given(mailSender).send(any(SimpleMailMessage.class));

        // when & then
        assertThatThrownBy(() -> memberService.sendVerificationCode(request))
                .isInstanceOf(MemberException.class)
                .hasMessage(MemberErrorCode.EMAIL_SEND_FAILED.getMessage());
    }

    // ========== 이메일 코드 확인 메서드 테스트 ==========

    @DisplayName("인증코드를 발급받은 적이 없는 이메일로 인증을 요청하면 코드 미발급 예외가 발생한다.")
    @Test
    void 인증코드_발급_이력이_없는_경우_코드_미발급_예외가_발생한다() {
        // given
        EmailVerifyConfirmRequest request = new EmailVerifyConfirmRequest(TEST_EMAIL, TEST_CODE);
        given(redisUtil.get("email:verify:fail:" + TEST_EMAIL)).willReturn(null);
        given(redisUtil.get("email:verify:" + TEST_EMAIL)).willReturn(null);
        given(redisUtil.exists("email:verify:issued:" + TEST_EMAIL)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> memberService.confirmVerificationCode(request))
                .isInstanceOf(MemberException.class)
                .hasMessage(MemberErrorCode.VERIFY_CODE_NOT_ISSUED.getMessage());
    }

    @DisplayName("인증코드 유효 시간이 만료된 경우 인증 시간 만료 예외가 발생한다.")
    @Test
    void 인증코드_유효시간이_만료된_경우_만료_예외가_발생한다() {
        // given
        EmailVerifyConfirmRequest request = new EmailVerifyConfirmRequest(TEST_EMAIL, TEST_CODE);
        given(redisUtil.get("email:verify:fail:" + TEST_EMAIL)).willReturn(null);
        given(redisUtil.get("email:verify:" + TEST_EMAIL)).willReturn(null);
        given(redisUtil.exists("email:verify:issued:" + TEST_EMAIL)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> memberService.confirmVerificationCode(request))
                .isInstanceOf(MemberException.class)
                .hasMessage(MemberErrorCode.EXPIRED_VERIFY_CODE.getMessage());
    }

    @DisplayName("올바르지 않은 인증코드를 입력하면 인증코드 불일치 예외가 발생하고 실패 카운터가 증가한다.")
    @Test
    void 올바르지_않은_인증코드_입력시_불일치_예외가_발생한다() {
        // given
        EmailVerifyConfirmRequest request = new EmailVerifyConfirmRequest(TEST_EMAIL, "000000");
        given(redisUtil.get("email:verify:fail:" + TEST_EMAIL)).willReturn(null);
        given(redisUtil.get("email:verify:" + TEST_EMAIL)).willReturn(TEST_CODE);

        // when & then
        assertThatThrownBy(() -> memberService.confirmVerificationCode(request))
                .isInstanceOf(MemberException.class)
                .hasMessage(MemberErrorCode.INVALID_VERIFY_CODE.getMessage());
        then(redisUtil).should().incrementWithTtl(eq("email:verify:fail:" + TEST_EMAIL), eq(5L), eq(TimeUnit.MINUTES));
    }

    @DisplayName("인증코드 실패 횟수가 최대치(5회)에 도달하면 시도 횟수 초과 예외가 발생한다.")
    @Test
    void 인증코드_실패_횟수가_최대치에_도달하면_시도_횟수_초과_예외가_발생한다() {
        // given
        EmailVerifyConfirmRequest request = new EmailVerifyConfirmRequest(TEST_EMAIL, TEST_CODE);
        given(redisUtil.get("email:verify:fail:" + TEST_EMAIL)).willReturn("5");

        // when & then
        assertThatThrownBy(() -> memberService.confirmVerificationCode(request))
                .isInstanceOf(MemberException.class)
                .hasMessage(MemberErrorCode.TOO_MANY_VERIFY_ATTEMPTS.getMessage());
    }

    @DisplayName("인증코드 실패 횟수가 최대치 미만이면 인증을 계속 시도할 수 있다.")
    @Test
    void 인증코드_실패_횟수가_최대치_미만이면_인증을_계속_시도할_수_있다() {
        // given
        EmailVerifyConfirmRequest request = new EmailVerifyConfirmRequest(TEST_EMAIL, "000000");
        given(redisUtil.get("email:verify:fail:" + TEST_EMAIL)).willReturn("4");
        given(redisUtil.get("email:verify:" + TEST_EMAIL)).willReturn(TEST_CODE);

        // when & then
        assertThatThrownBy(() -> memberService.confirmVerificationCode(request))
                .isInstanceOf(MemberException.class)
                .hasMessage(MemberErrorCode.INVALID_VERIFY_CODE.getMessage());
        then(redisUtil).should().incrementWithTtl(eq("email:verify:fail:" + TEST_EMAIL), eq(5L), eq(TimeUnit.MINUTES));
    }

    @DisplayName("올바른 인증코드를 입력하면 인증코드가 삭제되고 인증 완료 내역이 저장된다.")
    @Test
    void 올바른_인증코드_입력시_인증코드_삭제되고_인증완료_내역이_저장된다() {
        // given
        EmailVerifyConfirmRequest request = new EmailVerifyConfirmRequest(TEST_EMAIL, TEST_CODE);
        given(redisUtil.get("email:verify:fail:" + TEST_EMAIL)).willReturn(null);
        given(redisUtil.get("email:verify:" + TEST_EMAIL)).willReturn(TEST_CODE);

        // when
        memberService.confirmVerificationCode(request);

        // then
        then(redisUtil).should().delete("email:verify:" + TEST_EMAIL);
        then(redisUtil).should().delete("email:verify:fail:" + TEST_EMAIL);
        then(redisUtil).should().set(eq("email:verified:" + TEST_EMAIL), eq("true"), eq(30L), eq(TimeUnit.MINUTES));
    }

    // ========== 회원가입 메서드 ==========

    @DisplayName("이메일 인증을 완료하지 않은 상태에서 회원가입을 시도하면 이메일 미인증 예외가 발생한다.")
    @Test
    void 이메일_인증_미완료_상태에서_회원가입_시도시_미인증_예외가_발생한다() {
        // given
        SignUpRequest request = new SignUpRequest(TEST_EMAIL, "password123!", Gender.MALE, LocalDate.of(2000, 1, 1));
        given(redisUtil.exists("email:verified:" + TEST_EMAIL)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> memberService.signUp(request))
                .isInstanceOf(MemberException.class)
                .hasMessage(MemberErrorCode.EMAIL_NOT_VERIFIED.getMessage());
    }

    @DisplayName("이메일 인증을 완료한 경우 회원가입 시 회원 정보와 프로필이 저장된다.")
    @Test
    void 이메일_인증_완료후_회원가입시_회원정보와_프로필이_저장된다() {
        // given
        SignUpRequest request = new SignUpRequest(TEST_EMAIL, "password123!", Gender.MALE, LocalDate.of(2000, 1, 1));
        given(redisUtil.exists("email:verified:" + TEST_EMAIL)).willReturn(true);
        given(passwordEncoder.encode("password123!")).willReturn("encodedPassword");
        given(memberRepository.saveAndFlush(any(Member.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        memberService.signUp(request);

        // then
        then(memberRepository).should().saveAndFlush(any(Member.class));
        then(redisUtil).should().delete("email:verified:" + TEST_EMAIL);
    }

    @DisplayName("DB 레벨에서 이메일 중복이 감지되면 중복 이메일 예외가 발생한다.")
    @Test
    void DB_레벨_이메일_중복_감지시_중복_이메일_예외가_발생한다() {
        // given
        SignUpRequest request = new SignUpRequest(TEST_EMAIL, "password123!", Gender.MALE, LocalDate.of(2000, 1, 1));
        given(redisUtil.exists("email:verified:" + TEST_EMAIL)).willReturn(true);
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
        given(memberRepository.saveAndFlush(any(Member.class))).willThrow(DataIntegrityViolationException.class);

        // when & then
        assertThatThrownBy(() -> memberService.signUp(request))
                .isInstanceOf(MemberException.class)
                .hasMessage(MemberErrorCode.DUPLICATE_EMAIL.getMessage());
    }

    @DisplayName("회원가입이 완료되면 이메일 인증 완료 내역이 Redis에서 삭제된다.")
    @Test
    void 회원가입_완료시_이메일_인증_완료_내역이_Redis에서_삭제된다() {
        // given
        SignUpRequest request = new SignUpRequest(TEST_EMAIL, "password123!", Gender.FEMALE, LocalDate.of(1999, 5, 20));
        given(redisUtil.exists("email:verified:" + TEST_EMAIL)).willReturn(true);
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
        given(memberRepository.saveAndFlush(any(Member.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        memberService.signUp(request);

        // then
        then(redisUtil).should().delete("email:verified:" + TEST_EMAIL);
    }
}
