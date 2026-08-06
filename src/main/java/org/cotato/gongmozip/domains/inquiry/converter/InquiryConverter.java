package org.cotato.gongmozip.domains.inquiry.converter;

import java.util.List;
import org.cotato.gongmozip.domains.inquiry.dto.request.InquiryRequest.CreateInquiryRequest;
import org.cotato.gongmozip.domains.inquiry.dto.response.InquiryResponse.AdminInquiryListResponse;
import org.cotato.gongmozip.domains.inquiry.dto.response.InquiryResponse.AdminInquirySummaryResponse;
import org.cotato.gongmozip.domains.inquiry.dto.response.InquiryResponse.InquiryDetailResponse;
import org.cotato.gongmozip.domains.inquiry.dto.response.InquiryResponse.InquiryListResponse;
import org.cotato.gongmozip.domains.inquiry.dto.response.InquiryResponse.InquirySummaryResponse;
import org.cotato.gongmozip.domains.inquiry.entity.Inquiry;
import org.springframework.data.domain.Page;

public class InquiryConverter {

    private static final int CONTENT_PREVIEW_LENGTH = 50;

    // dto -> entity
    public static Inquiry toInquiry(CreateInquiryRequest request, String encodedPassword) {
        return Inquiry.builder()
                .email(request.email())
                .password(encodedPassword)
                .title(request.title())
                .content(request.content())
                .build();
    }

    // entity -> dto
    public static InquirySummaryResponse toSummaryResponse(Inquiry inquiry) {
        return new InquirySummaryResponse(
                inquiry.getInquiryId(),
                inquiry.getStatus(),
                inquiry.getTitle(),
                truncateContent(inquiry.getContent()),
                inquiry.getCreatedAt());
    }

    public static InquiryListResponse toListResponse(List<Inquiry> inquiries) {
        List<InquirySummaryResponse> summaries =
                inquiries.stream().map(InquiryConverter::toSummaryResponse).toList();
        return new InquiryListResponse(summaries, summaries.size());
    }

    public static InquiryDetailResponse toDetailResponse(Inquiry inquiry) {
        return new InquiryDetailResponse(
                inquiry.getInquiryId(),
                inquiry.getStatus(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getEmail(),
                inquiry.getCreatedAt(),
                inquiry.getAnswerContent(),
                inquiry.getAnsweredAt());
    }

    public static AdminInquirySummaryResponse toAdminSummaryResponse(Inquiry inquiry) {
        return new AdminInquirySummaryResponse(
                inquiry.getInquiryId(),
                inquiry.getStatus(),
                inquiry.getTitle(),
                truncateContent(inquiry.getContent()),
                inquiry.getEmail(),
                inquiry.getCreatedAt());
    }

    public static AdminInquiryListResponse toAdminListResponse(Page<Inquiry> inquiryPage) {
        List<AdminInquirySummaryResponse> summaries = inquiryPage.getContent().stream()
                .map(InquiryConverter::toAdminSummaryResponse)
                .toList();
        return new AdminInquiryListResponse(
                summaries,
                inquiryPage.getNumber(),
                inquiryPage.getSize(),
                inquiryPage.getTotalElements(),
                inquiryPage.getTotalPages(),
                inquiryPage.hasNext());
    }

    private static String truncateContent(String content) {
        if (content.length() <= CONTENT_PREVIEW_LENGTH) {
            return content;
        }
        return content.substring(0, CONTENT_PREVIEW_LENGTH);
    }
}
