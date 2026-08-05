package org.cotato.gongmozip.domains.inquiry.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.inquiry.enums.InquiryStatus;

public class InquiryResponse {

    // 문의 내역 목록의 개별 항목
    public record InquirySummaryResponse(
            Long inquiryId, InquiryStatus status, String title, String contentPreview, LocalDateTime createdAt) {}

    // 문의 내역 목록
    public record InquiryListResponse(List<InquirySummaryResponse> inquiries, int totalCount) {}

    // 문의 상세 (답변 전이면 answerContent/answeredAt은 null)
    public record InquiryDetailResponse(
            Long inquiryId,
            InquiryStatus status,
            String title,
            String content,
            String email,
            LocalDateTime createdAt,
            String answerContent,
            LocalDateTime answeredAt) {}
}
