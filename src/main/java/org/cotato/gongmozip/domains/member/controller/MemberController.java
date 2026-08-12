package org.cotato.gongmozip.domains.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.member.dto.request.MemberAuthRequest.EmailVerifyConfirmRequest;
import org.cotato.gongmozip.domains.member.dto.request.MemberAuthRequest.EmailVerifyRequest;
import org.cotato.gongmozip.domains.member.dto.request.MemberAuthRequest.RegisterRequiredInfoRequest;
import org.cotato.gongmozip.domains.member.dto.request.MemberAuthRequest.SignUpRequest;
import org.cotato.gongmozip.domains.member.dto.request.MemberRequest.WithdrawMemberRequest;
import org.cotato.gongmozip.domains.member.dto.response.MemberAuthResponse.SignUpResponse;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.exception.codes.MemberSuccessCode;
import org.cotato.gongmozip.domains.member.service.MemberService;
import org.cotato.gongmozip.domains.member.service.MemberWithdrawService;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member", description = "회원 관련 API")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberWithdrawService memberWithdrawService;

    @Operation(summary = "이메일 인증코드 발송")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MemberErrorCode.class)
    @PostMapping("/email/verify-request")
    public ResponseEntity<BaseResponse<Void>> sendVerificationCode(@RequestBody @Valid EmailVerifyRequest request) {
        memberService.sendVerificationCode(request);
        return BaseResponseFormatter.success(MemberSuccessCode.EMAIL_VERIFY_CODE_SENT);
    }

    @Operation(summary = "이메일 인증코드 확인")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MemberErrorCode.class)
    @PostMapping("/email/verify")
    public ResponseEntity<BaseResponse<Void>> confirmVerificationCode(
            @RequestBody @Valid EmailVerifyConfirmRequest request) {
        memberService.confirmVerificationCode(request);
        return BaseResponseFormatter.success(MemberSuccessCode.EMAIL_VERIFY_SUCCESS);
    }

    @Operation(summary = "이메일 회원가입")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MemberErrorCode.class)
    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<SignUpResponse>> signUp(@RequestBody @Valid SignUpRequest request) {
        SignUpResponse response = memberService.signUp(request);
        return BaseResponseFormatter.success(MemberSuccessCode.SIGN_UP_SUCCESS, response);
    }

    @Operation(summary = "소셜 로그인 후 필수 정보(성별/생년월일) 등록")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MemberErrorCode.class)
    @PatchMapping("/me/required-info")
    public ResponseEntity<BaseResponse<Void>> registerRequiredInfo(
            @RequestBody @Valid RegisterRequiredInfoRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        memberService.registerRequiredInfo(request, userDetails.getMemberId());
        return BaseResponseFormatter.success(MemberSuccessCode.REQUIRED_INFO_REGISTERED);
    }

    @Operation(summary = "내 기본 정보 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MemberErrorCode.class)
    @org.springframework.web.bind.annotation.GetMapping("/me")
    public ResponseEntity<
                    BaseResponse<org.cotato.gongmozip.domains.member.dto.response.MemberResponse.MemberMeResponse>>
            getMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        org.cotato.gongmozip.domains.member.dto.response.MemberResponse.MemberMeResponse response =
                memberService.getMemberMe(userDetails.getMemberId());
        return BaseResponseFormatter.success(MemberSuccessCode.MEMBER_DETAIL_RETRIEVED, response);
    }

    @Operation(summary = "내 기본 정보 수정")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MemberErrorCode.class)
    @org.springframework.web.bind.annotation.PatchMapping("/me")
    public ResponseEntity<BaseResponse<Void>> updateMyInfo(
            @RequestBody @Valid
                    org.cotato.gongmozip.domains.member.dto.request.MemberRequest.UpdateMemberMeRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        memberService.updateMemberMe(userDetails.getMemberId(), request);
        return BaseResponseFormatter.success(MemberSuccessCode.MEMBER_DETAIL_UPDATED);
    }

    @Operation(summary = "마케팅 수신동의 여부 수정")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MemberErrorCode.class)
    @org.springframework.web.bind.annotation.PatchMapping("/me/marketing-consents")
    public ResponseEntity<BaseResponse<Void>> updateMarketingConsent(
            @RequestBody @Valid
                    org.cotato.gongmozip.domains.member.dto.request.MemberRequest.UpdateMarketingConsentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        memberService.updateMarketingConsent(userDetails.getMemberId(), request);
        return BaseResponseFormatter.success(MemberSuccessCode.MARKETING_CONSENT_UPDATED);
    }

    @Operation(summary = "프로필 사진 업로드용 Presigned URL 발급")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MemberErrorCode.class)
    @PostMapping("/me/profile-image/presigned-url")
    public ResponseEntity<BaseResponse<org.cotato.gongmozip.domains.upload.dto.response.GetPresignedUrlResponse>>
            getProfileImagePresignedUrl(
                    @RequestBody @Valid
                            org.cotato.gongmozip.domains.member.dto.request.MemberRequest
                                            .GetProfileImagePresignedUrlRequest
                                    request,
                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        org.cotato.gongmozip.domains.upload.dto.response.GetPresignedUrlResponse response =
                memberService.getProfileImagePresignedUrl(request);
        return BaseResponseFormatter.success(MemberSuccessCode.PROFILE_IMAGE_PRESIGNED_URL_GENERATED, response);
    }

    @Operation(summary = "프로필 사진 업데이트")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MemberErrorCode.class)
    @PatchMapping("/me/profile-image")
    public ResponseEntity<BaseResponse<Void>> updateProfileImage(
            @RequestBody @Valid
                    org.cotato.gongmozip.domains.member.dto.request.MemberRequest.UpdateProfileImageRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        memberService.updateProfileImage(userDetails.getMemberId(), request);
        return BaseResponseFormatter.success(MemberSuccessCode.PROFILE_IMAGE_UPDATED);
    }

    @Operation(summary = "회원 탈퇴", description = "이메일 가입 회원은 비밀번호 검증 후 탈퇴 처리됩니다. 탈퇴 후 14일간 같은 이메일로 재가입할 수 없습니다.")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = MemberErrorCode.class)
    @DeleteMapping("/me")
    public ResponseEntity<BaseResponse<Void>> withdraw(
            @RequestBody @Valid WithdrawMemberRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest) {
        memberWithdrawService.withdraw(userDetails.getMemberId(), extractToken(httpRequest), request);
        return BaseResponseFormatter.success(MemberSuccessCode.MEMBER_WITHDRAWN);
    }

    // Authorization 헤더에서 access 토큰 추출 (탈퇴 시 블랙리스트 처리용)
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
