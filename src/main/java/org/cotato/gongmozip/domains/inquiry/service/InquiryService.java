package org.cotato.gongmozip.domains.inquiry.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.inquiry.converter.InquiryConverter;
import org.cotato.gongmozip.domains.inquiry.dto.request.InquiryRequest.AnswerInquiryRequest;
import org.cotato.gongmozip.domains.inquiry.dto.request.InquiryRequest.CreateInquiryRequest;
import org.cotato.gongmozip.domains.inquiry.dto.request.InquiryRequest.InquiryAuthRequest;
import org.cotato.gongmozip.domains.inquiry.dto.response.InquiryResponse.AdminInquiryListResponse;
import org.cotato.gongmozip.domains.inquiry.dto.response.InquiryResponse.InquiryDetailResponse;
import org.cotato.gongmozip.domains.inquiry.dto.response.InquiryResponse.InquiryListResponse;
import org.cotato.gongmozip.domains.inquiry.entity.Inquiry;
import org.cotato.gongmozip.domains.inquiry.enums.InquiryStatus;
import org.cotato.gongmozip.domains.inquiry.exception.InquiryException;
import org.cotato.gongmozip.domains.inquiry.exception.codes.InquiryErrorCode;
import org.cotato.gongmozip.domains.inquiry.repository.InquiryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_PAGE_SIZE = 100;

    @Transactional
    public void createInquiry(CreateInquiryRequest request) {
        String encodedPassword = passwordEncoder.encode(request.password());
        inquiryRepository.save(InquiryConverter.toInquiry(request, encodedPassword));
    }

    public InquiryListResponse getInquiries(InquiryAuthRequest request) {
        // BCrypt 해시는 equality 조회가 불가하므로 이메일로 조회 후 비밀번호를 행 단위로 대조한다
        List<Inquiry> matched = inquiryRepository.findTop100ByEmailOrderByCreatedAtDesc(request.email()).stream()
                .filter(inquiry -> passwordEncoder.matches(request.password(), inquiry.getPassword()))
                .toList();

        if (matched.isEmpty()) {
            throw new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND);
        }
        return InquiryConverter.toListResponse(matched);
    }

    public InquiryDetailResponse getInquiryDetail(Long inquiryId, InquiryAuthRequest request) {
        // 존재하지 않는 문의와 자격증명 불일치를 같은 에러로 응답해 문의 존재 여부를 노출하지 않는다
        Inquiry inquiry = inquiryRepository
                .findById(inquiryId)
                .orElseThrow(() -> new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND));

        if (!inquiry.getEmail().equals(request.email())
                || !passwordEncoder.matches(request.password(), inquiry.getPassword())) {
            throw new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND);
        }
        return InquiryConverter.toDetailResponse(inquiry);
    }

    public AdminInquiryListResponse getAdminInquiries(String status, Integer page, Integer size) {
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 10;

        if (pageNum < 0 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new InquiryException(InquiryErrorCode.INQUIRY_INVALID_INPUT);
        }

        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Inquiry> inquiryPage;
        if (status == null || status.isBlank()) {
            inquiryPage = inquiryRepository.findAll(pageable);
        } else {
            inquiryPage = inquiryRepository.findAllByStatus(parseStatus(status), pageable);
        }
        return InquiryConverter.toAdminListResponse(inquiryPage);
    }

    public InquiryDetailResponse getAdminInquiryDetail(Long inquiryId) {
        Inquiry inquiry = inquiryRepository
                .findById(inquiryId)
                .orElseThrow(() -> new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND));
        return InquiryConverter.toDetailResponse(inquiry);
    }

    @Transactional
    public void answerInquiry(Long inquiryId, AnswerInquiryRequest request) {
        Inquiry inquiry = inquiryRepository
                .findById(inquiryId)
                .orElseThrow(() -> new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND));
        inquiry.answer(request.answerContent());
    }

    private InquiryStatus parseStatus(String status) {
        try {
            return InquiryStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InquiryException(InquiryErrorCode.INQUIRY_INVALID_INPUT);
        }
    }
}
