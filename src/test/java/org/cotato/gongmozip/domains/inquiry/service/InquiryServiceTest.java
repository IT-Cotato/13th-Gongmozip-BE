package org.cotato.gongmozip.domains.inquiry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.inquiry.dto.request.InquiryRequest.CreateInquiryRequest;
import org.cotato.gongmozip.domains.inquiry.dto.request.InquiryRequest.InquiryAuthRequest;
import org.cotato.gongmozip.domains.inquiry.dto.response.InquiryResponse.InquiryDetailResponse;
import org.cotato.gongmozip.domains.inquiry.dto.response.InquiryResponse.InquiryListResponse;
import org.cotato.gongmozip.domains.inquiry.entity.Inquiry;
import org.cotato.gongmozip.domains.inquiry.enums.InquiryStatus;
import org.cotato.gongmozip.domains.inquiry.exception.InquiryException;
import org.cotato.gongmozip.domains.inquiry.exception.codes.InquiryErrorCode;
import org.cotato.gongmozip.domains.inquiry.repository.InquiryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private InquiryService inquiryService;

    private static final String TEST_EMAIL = "test@gongmozip.com";
    private static final String TEST_PASSWORD = "1234";
    private static final String ENCODED_PASSWORD = "$2a$10$encodedPassword";

    private Inquiry inquiry(Long id, String encodedPassword) {
        return Inquiry.builder()
                .inquiryId(id)
                .email(TEST_EMAIL)
                .password(encodedPassword)
                .title("문의 제목")
                .content("문의 내용입니다.")
                .build();
    }

    @DisplayName("문의 등록 시 비밀번호가 인코딩되어 저장된다.")
    @Test
    void 문의_등록시_비밀번호가_인코딩되어_저장된다() {
        // given
        CreateInquiryRequest request = new CreateInquiryRequest(TEST_EMAIL, TEST_PASSWORD, "문의 제목", "문의 내용입니다.");
        given(passwordEncoder.encode(TEST_PASSWORD)).willReturn(ENCODED_PASSWORD);

        // when
        inquiryService.createInquiry(request);

        // then
        ArgumentCaptor<Inquiry> captor = ArgumentCaptor.forClass(Inquiry.class);
        then(inquiryRepository).should().save(captor.capture());
        Inquiry saved = captor.getValue();
        assertThat(saved.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(saved.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(saved.getStatus()).isEqualTo(InquiryStatus.PENDING);
    }

    @DisplayName("같은 이메일이라도 문의 비밀번호가 일치하는 문의만 반환한다.")
    @Test
    void 이메일과_비밀번호_조합이_일치하는_문의만_반환한다() {
        // given
        Inquiry matched = inquiry(1L, ENCODED_PASSWORD);
        Inquiry notMatched = inquiry(2L, "$2a$10$otherPassword");
        given(inquiryRepository.findAllByEmailOrderByCreatedAtDesc(TEST_EMAIL))
                .willReturn(List.of(matched, notMatched));
        given(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
        given(passwordEncoder.matches(TEST_PASSWORD, "$2a$10$otherPassword")).willReturn(false);

        // when
        InquiryListResponse response = inquiryService.getInquiries(new InquiryAuthRequest(TEST_EMAIL, TEST_PASSWORD));

        // then
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.inquiries().get(0).inquiryId()).isEqualTo(1L);
    }

    @DisplayName("조합이 일치하는 문의가 없으면 문의 없음 예외가 발생한다.")
    @Test
    void 일치하는_문의가_없으면_문의_없음_예외가_발생한다() {
        // given
        given(inquiryRepository.findAllByEmailOrderByCreatedAtDesc(TEST_EMAIL)).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> inquiryService.getInquiries(new InquiryAuthRequest(TEST_EMAIL, TEST_PASSWORD)))
                .isInstanceOf(InquiryException.class)
                .hasMessage(InquiryErrorCode.INQUIRY_NOT_FOUND.getMessage());
    }

    @DisplayName("존재하지 않는 문의 ID로 상세 조회하면 문의 없음 예외가 발생한다.")
    @Test
    void 존재하지_않는_문의_ID로_상세_조회시_문의_없음_예외가_발생한다() {
        // given
        given(inquiryRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
                        () -> inquiryService.getInquiryDetail(99L, new InquiryAuthRequest(TEST_EMAIL, TEST_PASSWORD)))
                .isInstanceOf(InquiryException.class)
                .hasMessage(InquiryErrorCode.INQUIRY_NOT_FOUND.getMessage());
    }

    @DisplayName("상세 조회 시 자격증명이 일치하지 않으면 동일한 문의 없음 예외가 발생한다.")
    @Test
    void 상세_조회시_자격증명_불일치면_문의_없음_예외가_발생한다() {
        // given
        Inquiry inquiry = inquiry(1L, ENCODED_PASSWORD);
        given(inquiryRepository.findById(1L)).willReturn(Optional.of(inquiry));
        given(passwordEncoder.matches("9999", ENCODED_PASSWORD)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> inquiryService.getInquiryDetail(1L, new InquiryAuthRequest(TEST_EMAIL, "9999")))
                .isInstanceOf(InquiryException.class)
                .hasMessage(InquiryErrorCode.INQUIRY_NOT_FOUND.getMessage());
    }

    @DisplayName("답변 완료된 문의 상세 조회 시 답변 내용이 포함된다.")
    @Test
    void 답변_완료된_문의_상세_조회시_답변_내용이_포함된다() {
        // given
        Inquiry inquiry = inquiry(1L, ENCODED_PASSWORD);
        inquiry.answer("고객님의 문의에 대한 답변입니다.");
        given(inquiryRepository.findById(1L)).willReturn(Optional.of(inquiry));
        given(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).willReturn(true);

        // when
        InquiryDetailResponse response =
                inquiryService.getInquiryDetail(1L, new InquiryAuthRequest(TEST_EMAIL, TEST_PASSWORD));

        // then
        assertThat(response.status()).isEqualTo(InquiryStatus.ANSWERED);
        assertThat(response.answerContent()).isEqualTo("고객님의 문의에 대한 답변입니다.");
        assertThat(response.answeredAt()).isNotNull();
    }

    @DisplayName("문의 내용이 50자를 초과하면 목록에서 50자로 잘린 미리보기를 제공한다.")
    @Test
    void 문의_내용이_50자를_초과하면_미리보기로_잘린다() {
        // given
        String longContent = "가".repeat(80);
        Inquiry inquiry = Inquiry.builder()
                .inquiryId(1L)
                .email(TEST_EMAIL)
                .password(ENCODED_PASSWORD)
                .title("문의 제목")
                .content(longContent)
                .build();
        given(inquiryRepository.findAllByEmailOrderByCreatedAtDesc(TEST_EMAIL)).willReturn(List.of(inquiry));
        given(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).willReturn(true);

        // when
        InquiryListResponse response = inquiryService.getInquiries(new InquiryAuthRequest(TEST_EMAIL, TEST_PASSWORD));

        // then
        assertThat(response.inquiries().get(0).contentPreview()).hasSize(50);
    }
}
