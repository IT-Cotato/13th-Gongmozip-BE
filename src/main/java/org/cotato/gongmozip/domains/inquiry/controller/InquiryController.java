package org.cotato.gongmozip.domains.inquiry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.inquiry.dto.request.InquiryRequest.CreateInquiryRequest;
import org.cotato.gongmozip.domains.inquiry.dto.request.InquiryRequest.InquiryAuthRequest;
import org.cotato.gongmozip.domains.inquiry.dto.response.InquiryResponse.InquiryDetailResponse;
import org.cotato.gongmozip.domains.inquiry.dto.response.InquiryResponse.InquiryListResponse;
import org.cotato.gongmozip.domains.inquiry.exception.codes.InquiryErrorCode;
import org.cotato.gongmozip.domains.inquiry.exception.codes.InquirySuccessCode;
import org.cotato.gongmozip.domains.inquiry.service.InquiryService;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Inquiry", description = "문의하기 API (비회원용)")
@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @Operation(summary = "문의 등록", description = "이메일과 문의 비밀번호(숫자 4자리)로 문의를 등록합니다.")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = InquiryErrorCode.class)
    @PostMapping
    public ResponseEntity<BaseResponse<Void>> createInquiry(@RequestBody @Valid CreateInquiryRequest request) {
        inquiryService.createInquiry(request);
        return BaseResponseFormatter.success(InquirySuccessCode.INQUIRY_CREATED);
    }

    @Operation(summary = "문의 내역 조회", description = "문의 작성 시 입력한 이메일과 문의 비밀번호가 모두 일치하는 문의 목록을 조회합니다.")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = InquiryErrorCode.class)
    @PostMapping("/list")
    public ResponseEntity<BaseResponse<InquiryListResponse>> getInquiries(
            @RequestBody @Valid InquiryAuthRequest request) {
        InquiryListResponse response = inquiryService.getInquiries(request);
        return BaseResponseFormatter.success(InquirySuccessCode.INQUIRY_LIST_RETRIEVED, response);
    }

    @Operation(summary = "문의 상세 조회", description = "이메일과 문의 비밀번호 검증 후 문의 상세(답변 포함)를 조회합니다.")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = InquiryErrorCode.class)
    @PostMapping("/{inquiryId}")
    public ResponseEntity<BaseResponse<InquiryDetailResponse>> getInquiryDetail(
            @PathVariable Long inquiryId, @RequestBody @Valid InquiryAuthRequest request) {
        InquiryDetailResponse response = inquiryService.getInquiryDetail(inquiryId, request);
        return BaseResponseFormatter.success(InquirySuccessCode.INQUIRY_DETAIL_RETRIEVED, response);
    }
}
