package org.cotato.gongmozip.domains.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.member.dto.request.MemberAuthRequest.EmailVerifyConfirmRequest;
import org.cotato.gongmozip.domains.member.dto.request.MemberAuthRequest.EmailVerifyRequest;
import org.cotato.gongmozip.domains.member.dto.request.MemberAuthRequest.RegisterRequiredInfoRequest;
import org.cotato.gongmozip.domains.member.dto.request.MemberAuthRequest.SignUpRequest;
import org.cotato.gongmozip.domains.member.dto.response.MemberAuthResponse.SignUpResponse;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.exception.codes.MemberSuccessCode;
import org.cotato.gongmozip.domains.member.service.MemberService;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
}
