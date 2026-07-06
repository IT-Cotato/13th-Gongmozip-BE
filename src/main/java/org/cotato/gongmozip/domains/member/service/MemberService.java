package org.cotato.gongmozip.domains.member.service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.member.converter.MemberConverter;
import org.cotato.gongmozip.domains.member.dto.request.MemberAuthRequest.EmailVerifyConfirmRequest;
import org.cotato.gongmozip.domains.member.dto.request.MemberAuthRequest.EmailVerifyRequest;
import org.cotato.gongmozip.domains.member.dto.request.MemberAuthRequest.SignUpRequest;
import org.cotato.gongmozip.domains.member.dto.response.MemberAuthResponse.SignUpResponse;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.entity.MemberProfile;
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberProfileRepository;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.global.exception.CustomException;
import org.cotato.gongmozip.global.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private static final String VERIFY_CODE_PREFIX = "email:verify:";
    private static final String VERIFY_CODE_ISSUED_PREFIX = "email:verify:issued:";
    private static final String VERIFIED_PREFIX = "email:verified:";
    private static final String VERIFY_FAIL_PREFIX = "email:verify:fail:";
    private static final long VERIFY_CODE_TTL = 5;
    private static final long VERIFY_CODE_ISSUED_TTL = 10;
    private static final long VERIFIED_TTL = 30;
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    @Value("${spring.mail.username}")
    private String mailUsername;

    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final RedisUtil redisUtil;

    // 인증 코드 전송 메서드
    public void sendVerificationCode(EmailVerifyRequest request) {

        // 이메일이 DB에 존재하는 경우(중복 가입 방지)
        if (memberRepository.existsByEmail(request.email())) {
            throw new CustomException(MemberErrorCode.DUPLICATE_EMAIL);
        }

        // 코드 생성 및 redis에 저장(TTL: 5), 새 코드 발급 시 실패 카운터 초기화
        String code = generateCode();
        redisUtil.set(VERIFY_CODE_PREFIX + request.email(), code, VERIFY_CODE_TTL, TimeUnit.MINUTES);
        redisUtil.set(VERIFY_CODE_ISSUED_PREFIX + request.email(), "true", VERIFY_CODE_ISSUED_TTL, TimeUnit.MINUTES);
        redisUtil.delete(VERIFY_FAIL_PREFIX + request.email());

        // 이메일 전송
        sendEmail(request.email(), code);
    }

    // 이메일 전송 코드 확인 메서드
    public void confirmVerificationCode(EmailVerifyConfirmRequest request) {
        String failKey = VERIFY_FAIL_PREFIX + request.email();
        String failCount = redisUtil.get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= MAX_VERIFY_ATTEMPTS) {
            throw new MemberException(MemberErrorCode.TOO_MANY_VERIFY_ATTEMPTS);
        }

        String stored = redisUtil.get(VERIFY_CODE_PREFIX + request.email());

        // redis에 이메일 인증 내역이 없는 경우
        if (stored == null) {
            // 인증 시간 만료
            if (redisUtil.exists(VERIFY_CODE_ISSUED_PREFIX + request.email())) {
                throw new MemberException(MemberErrorCode.EXPIRED_VERIFY_CODE);
            }
            // 발급한 적이 없는 경우
            throw new MemberException(MemberErrorCode.VERIFY_CODE_NOT_ISSUED);
        }
        // 같지 않은 경우
        if (!stored.equals(request.code())) {
            redisUtil.incrementWithTtl(failKey, VERIFY_CODE_TTL, TimeUnit.MINUTES);
            throw new MemberException(MemberErrorCode.INVALID_VERIFY_CODE);
        }

        redisUtil.delete(VERIFY_CODE_PREFIX + request.email());
        redisUtil.delete(failKey);
        // 인증 30분 동안 유효
        redisUtil.set(VERIFIED_PREFIX + request.email(), "true", VERIFIED_TTL, TimeUnit.MINUTES);
    }

    @Transactional
    // 회원 가입 메서드
    public SignUpResponse signUp(SignUpRequest request) {
        // 이메일 인증이 진행되지 않은 경우
        if (!redisUtil.exists(VERIFIED_PREFIX + request.email())) {
            throw new MemberException(MemberErrorCode.EMAIL_NOT_VERIFIED);
        }

        try {
            Member member = MemberConverter.toMember(request, passwordEncoder.encode(request.password()));
            memberRepository.saveAndFlush(member);

            MemberProfile memberProfile = MemberConverter.toMemberProfile(request, member);
            memberProfileRepository.save(memberProfile);

            redisUtil.delete(VERIFIED_PREFIX + request.email());

            return MemberConverter.toSignUpResponse(member);
        } catch (DataIntegrityViolationException e) {
            throw new MemberException(MemberErrorCode.DUPLICATE_EMAIL);
        }
    }

    // 인증 코드 생성 메서드
    private String generateCode() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }

    // 이메일 전송 메서드
    private void sendEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailUsername);
        message.setTo(to);
        message.setSubject("[공모집] 이메일 인증코드");
        message.setText("인증코드: " + code + "\n\n인증코드는 5분간 유효합니다.");
        mailSender.send(message);
    }
}
