package org.cotato.gongmozip.domains.member.service;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.auth.converter.AuthAccountConverter;
import org.cotato.gongmozip.domains.auth.repository.AuthAccountRepository;
import org.cotato.gongmozip.domains.member.converter.MemberConverter;
import org.cotato.gongmozip.domains.member.dto.request.MemberAuthRequest.EmailVerifyConfirmRequest;
import org.cotato.gongmozip.domains.member.dto.request.MemberAuthRequest.EmailVerifyRequest;
import org.cotato.gongmozip.domains.member.dto.request.MemberAuthRequest.RegisterRequiredInfoRequest;
import org.cotato.gongmozip.domains.member.dto.request.MemberAuthRequest.SignUpRequest;
import org.cotato.gongmozip.domains.member.dto.response.MemberAuthResponse.SignUpResponse;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.global.exception.CustomException;
import org.cotato.gongmozip.global.redis.RedisUtil;
import org.cotato.gongmozip.global.verification.EmailVerificationResult;
import org.cotato.gongmozip.global.verification.EmailVerificationService;
import org.cotato.gongmozip.global.verification.EmailVerificationService.Purpose;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private static final String VERIFIED_PREFIX = "email:verified:";
    private static final long VERIFIED_TTL = 30;

    private final MemberRepository memberRepository;
    private final AuthAccountRepository authAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisUtil redisUtil;
    private final EmailVerificationService emailVerificationService;
    private final org.cotato.gongmozip.domains.upload.service.S3Service s3Service;

    // 회원가입 인증 코드 전송 메서드
    public void sendVerificationCode(EmailVerifyRequest request) {

        // 이메일이 DB에 존재하는 경우(중복 가입 방지)
        if (memberRepository.existsByEmail(request.email())) {
            throw new CustomException(MemberErrorCode.DUPLICATE_EMAIL);
        }

        // 공통 이메일 인증 서비스를 통해 인증 코드 전송
        try {
            emailVerificationService.sendCode(Purpose.SIGN_UP, request.email(), "[공모집] 이메일 인증코드");
        } catch (MailException e) {
            throw new MemberException(MemberErrorCode.EMAIL_SEND_FAILED);
        }
    }

    // 회원가입 인증 코드 확인 메서드
    public void confirmVerificationCode(EmailVerifyConfirmRequest request) {
        // 공통 이메일 인증 결과를 member 도메인 예외로 변환
        EmailVerificationResult result =
                emailVerificationService.verifyCode(Purpose.SIGN_UP, request.email(), request.code());
        switch (result) {
            case INVALID_CODE -> throw new MemberException(MemberErrorCode.INVALID_VERIFY_CODE);
            case EXPIRED_CODE -> throw new MemberException(MemberErrorCode.EXPIRED_VERIFY_CODE);
            case CODE_NOT_ISSUED -> throw new MemberException(MemberErrorCode.VERIFY_CODE_NOT_ISSUED);
            case TOO_MANY_ATTEMPTS -> throw new MemberException(MemberErrorCode.TOO_MANY_VERIFY_ATTEMPTS);
            case VERIFIED -> {}
        }

        // 인증 완료 상태는 30분 동안 유효
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
            // 비밀번호를 암호화하여 회원 저장
            Member member = MemberConverter.toMember(request, passwordEncoder.encode(request.password()));
            memberRepository.saveAndFlush(member);

            // 이메일 로그인 계정 정보 저장
            authAccountRepository.save(AuthAccountConverter.toEmailAuthAccount(member));

            // 사용한 이메일 인증 완료 상태 삭제
            redisUtil.delete(VERIFIED_PREFIX + request.email());

            return MemberConverter.toSignUpResponse(member);
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 이메일로 가입한 경우
            throw new MemberException(MemberErrorCode.DUPLICATE_EMAIL);
        }
    }

    @Transactional
    // 소셜 회원 필수 정보 등록 메서드
    public void registerRequiredInfo(RegisterRequiredInfoRequest request, Long memberId) {
        // 회원이 존재하지 않는 경우
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 성별과 생년월일 등록
        member.registerRequiredInfo(request.gender(), request.birthDate());
    }

    @Transactional(readOnly = true)
    public org.cotato.gongmozip.domains.member.dto.response.MemberResponse.MemberMeResponse getMemberMe(Long memberId) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        return MemberConverter.toMemberMeResponse(member);
    }

    @Transactional
    public void updateMemberMe(
            Long memberId,
            org.cotato.gongmozip.domains.member.dto.request.MemberRequest.UpdateMemberMeRequest request) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        member.updateInfo(request.name(), request.gender(), request.birthDate());
    }

    @Transactional
    public void updateMarketingConsent(
            Long memberId,
            org.cotato.gongmozip.domains.member.dto.request.MemberRequest.UpdateMarketingConsentRequest request) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        member.updateMarketingConsents(request.marketingConsentEmail(), request.marketingConsentSms());
    }

    public org.cotato.gongmozip.domains.upload.dto.response.GetPresignedUrlResponse getProfileImagePresignedUrl(
            org.cotato.gongmozip.domains.member.dto.request.MemberRequest.GetProfileImagePresignedUrlRequest request) {
        return s3Service.getProfileImagePresignedUrl(request.fileName(), request.contentType());
    }

    @Transactional
    public void updateProfileImage(
            Long memberId,
            org.cotato.gongmozip.domains.member.dto.request.MemberRequest.UpdateProfileImageRequest request) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        String oldImageUrl = member.getProfileImageUrl();
        if (oldImageUrl != null && !oldImageUrl.equals(request.profileImageUrl())) {
            s3Service.deleteFile(oldImageUrl);
        }

        member.updateProfileImage(request.profileImageUrl());
    }
}
