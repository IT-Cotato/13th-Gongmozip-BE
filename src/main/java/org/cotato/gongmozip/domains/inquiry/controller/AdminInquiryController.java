package org.cotato.gongmozip.domains.inquiry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.inquiry.dto.request.InquiryRequest.AnswerInquiryRequest;
import org.cotato.gongmozip.domains.inquiry.dto.response.InquiryResponse.AdminInquiryListResponse;
import org.cotato.gongmozip.domains.inquiry.dto.response.InquiryResponse.InquiryDetailResponse;
import org.cotato.gongmozip.domains.inquiry.exception.codes.InquiryErrorCode;
import org.cotato.gongmozip.domains.inquiry.exception.codes.InquirySuccessCode;
import org.cotato.gongmozip.domains.inquiry.service.InquiryService;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Inquiry", description = "관리자 문의 관리 API")
@RestController
@RequestMapping("/api/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController {

    private final InquiryService inquiryService;

    @Operation(summary = "관리자 문의 목록 조회", description = "전체 문의 목록을 조회합니다. status(PENDING/ANSWERED)로 필터링할 수 있습니다.")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = InquiryErrorCode.class)
    @GetMapping
    public ResponseEntity<BaseResponse<AdminInquiryListResponse>> getInquiries(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(name = "size", required = false, defaultValue = "10") Integer size) {
        AdminInquiryListResponse response = inquiryService.getAdminInquiries(status, page, size);
        return BaseResponseFormatter.success(InquirySuccessCode.ADMIN_INQUIRY_LIST_RETRIEVED, response);
    }

    @Operation(summary = "관리자 문의 상세 조회", description = "문의 상세(답변 포함)를 조회합니다.")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = InquiryErrorCode.class)
    @GetMapping("/{inquiryId}")
    public ResponseEntity<BaseResponse<InquiryDetailResponse>> getInquiryDetail(@PathVariable Long inquiryId) {
        InquiryDetailResponse response = inquiryService.getAdminInquiryDetail(inquiryId);
        return BaseResponseFormatter.success(InquirySuccessCode.ADMIN_INQUIRY_DETAIL_RETRIEVED, response);
    }

    @Operation(summary = "문의 답변 등록/수정", description = "문의에 답변을 등록합니다. 이미 답변된 문의는 답변 내용을 덮어씁니다.")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = InquiryErrorCode.class)
    @PatchMapping("/{inquiryId}/answer")
    public ResponseEntity<BaseResponse<Void>> answerInquiry(
            @PathVariable Long inquiryId, @RequestBody @Valid AnswerInquiryRequest request) {
        inquiryService.answerInquiry(inquiryId, request);
        return BaseResponseFormatter.success(InquirySuccessCode.INQUIRY_ANSWERED);
    }
}
