package org.cotato.gongmozip.global.verification;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailVerificationService {

    @Getter
    @RequiredArgsConstructor
    // 인증 목적별 redis key prefix
    public enum Purpose {
        SIGN_UP("email:verify"),
        PASSWORD_RESET("password-reset:code");

        private final String redisPrefix;
    }

    private static final long CODE_TTL_MINUTES = 5;
    private static final long CODE_ISSUED_TTL_MINUTES = 10;
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${spring.mail.username}")
    private String mailUsername;

    private final JavaMailSender mailSender;
    private final RedisUtil redisUtil;

    // 이메일 인증 코드 전송 메서드
    public void sendCode(Purpose purpose, String email, String subject) {
        // 인증 목적과 이메일을 조합하여 redis key 생성
        String codeKey = codeKey(purpose, email);
        String issuedKey = issuedKey(purpose, email);
        String failKey = failKey(purpose, email);

        // 6자리 인증 코드와 발급 이력을 redis에 저장
        String code = generateCode();
        redisUtil.set(codeKey, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
        redisUtil.set(issuedKey, "true", CODE_ISSUED_TTL_MINUTES, TimeUnit.MINUTES);

        // 새 인증 코드 발급 시 기존 실패 횟수 초기화
        redisUtil.delete(failKey);

        // 인증 코드 이메일 전송
        try {
            sendEmail(email, subject, code);
        } catch (MailException e) {
            // 이메일 전송 실패 시 저장한 코드와 발급 이력 삭제
            redisUtil.delete(codeKey);
            redisUtil.delete(issuedKey);
            throw e;
        }
    }

    // 이메일 인증 코드 확인 메서드
    public EmailVerificationResult verifyCode(Purpose purpose, String email, String code) {
        // 인증 실패 횟수가 최대 횟수에 도달한 경우 정답 여부와 관계없이 인증 차단
        String failKey = failKey(purpose, email);
        String storedFailCount = redisUtil.get(failKey);
        if (storedFailCount != null && Long.parseLong(storedFailCount) >= MAX_VERIFY_ATTEMPTS) {
            return EmailVerificationResult.TOO_MANY_ATTEMPTS;
        }

        // redis에 저장된 인증 코드 조회
        String codeKey = codeKey(purpose, email);
        String storedCode = redisUtil.get(codeKey);

        // 코드가 없으면 발급 이력을 통해 만료와 미발급 구분
        if (storedCode == null) {
            return redisUtil.exists(issuedKey(purpose, email))
                    ? EmailVerificationResult.EXPIRED_CODE
                    : EmailVerificationResult.CODE_NOT_ISSUED;
        }

        // 코드가 일치하지 않으면 실패 횟수를 원자적으로 증가 후 반환값으로 잠금 여부 판단
        if (!storedCode.equals(code)) {
            long failCount = redisUtil.incrementWithTtl(failKey(purpose, email), CODE_TTL_MINUTES, TimeUnit.MINUTES);
            return failCount >= MAX_VERIFY_ATTEMPTS
                    ? EmailVerificationResult.TOO_MANY_ATTEMPTS
                    : EmailVerificationResult.INVALID_CODE;
        }

        // 인증 성공 시 코드, 발급 이력, 실패 횟수 삭제
        redisUtil.delete(codeKey);
        redisUtil.delete(issuedKey(purpose, email));
        redisUtil.delete(failKey);
        return EmailVerificationResult.VERIFIED;
    }

    // 6자리 인증 코드 생성 메서드
    private String generateCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    // 인증 코드 이메일 전송 메서드
    private void sendEmail(String email, String subject, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailUsername);
        message.setTo(email);
        message.setSubject(subject);
        message.setText("인증코드: " + code + "\n\n인증코드는 " + CODE_TTL_MINUTES + "분간 유효합니다.");
        mailSender.send(message);
    }

    // 인증 코드 redis key 생성 메서드
    private String codeKey(Purpose purpose, String email) {
        return purpose.getRedisPrefix() + ":" + email;
    }

    // 인증 코드 발급 이력 redis key 생성 메서드
    private String issuedKey(Purpose purpose, String email) {
        return purpose.getRedisPrefix() + ":issued:" + email;
    }

    // 인증 실패 횟수 redis key 생성 메서드
    private String failKey(Purpose purpose, String email) {
        return purpose.getRedisPrefix() + ":fail:" + email;
    }
}
