package org.cotato.gongmozip.domains.inquiry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
        given(inquiryRepository.findTop100ByEmailOrderByCreatedAtDesc(TEST_EMAIL))
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
        given(inquiryRepository.findTop100ByEmailOrderByCreatedAtDesc(TEST_EMAIL))
                .willReturn(List.of());

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
        given(inquiryRepository.findTop100ByEmailOrderByCreatedAtDesc(TEST_EMAIL))
                .willReturn(List.of(inquiry));
        given(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).willReturn(true);

        // when
        InquiryListResponse response = inquiryService.getInquiries(new InquiryAuthRequest(TEST_EMAIL, TEST_PASSWORD));

        // then
        assertThat(response.inquiries().get(0).contentPreview()).hasSize(50);
    }

    @DisplayName("관리자 목록 조회 시 상태 필터가 없으면 전체 문의를 페이징 조회한다.")
    @Test
    void 관리자_목록_조회시_필터가_없으면_전체_조회한다() {
        // given
        Page<Inquiry> page = new PageImpl<>(List.of(inquiry(1L, ENCODED_PASSWORD)), PageRequest.of(0, 10), 1);
        given(inquiryRepository.findAll(any(Pageable.class))).willReturn(page);

        // when
        AdminInquiryListResponse response = inquiryService.getAdminInquiries(null, 0, 10);

        // then
        then(inquiryRepository).should().findAll(any(Pageable.class));
        assertThat(response.inquiries()).hasSize(1);
        assertThat(response.inquiries().get(0).email()).isEqualTo(TEST_EMAIL);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
    }

    @DisplayName("관리자 목록 조회 시 상태 필터가 있으면 해당 상태의 문의만 조회한다.")
    @Test
    void 관리자_목록_조회시_상태_필터로_조회한다() {
        // given
        Page<Inquiry> page = new PageImpl<>(List.of(inquiry(1L, ENCODED_PASSWORD)), PageRequest.of(0, 10), 1);
        given(inquiryRepository.findAllByStatus(eq(InquiryStatus.PENDING), any(Pageable.class)))
                .willReturn(page);

        // when
        inquiryService.getAdminInquiries("PENDING", 0, 10);

        // then
        then(inquiryRepository).should().findAllByStatus(eq(InquiryStatus.PENDING), any(Pageable.class));
    }

    @DisplayName("관리자 목록 조회 시 유효하지 않은 상태 값이면 잘못된 입력 예외가 발생한다.")
    @Test
    void 관리자_목록_조회시_유효하지_않은_상태값이면_예외가_발생한다() {
        // when & then
        assertThatThrownBy(() -> inquiryService.getAdminInquiries("INVALID", 0, 10))
                .isInstanceOf(InquiryException.class)
                .hasMessage(InquiryErrorCode.INQUIRY_INVALID_INPUT.getMessage());
    }

    @DisplayName("관리자 목록 조회 시 페이지 번호가 음수이거나 크기가 범위를 벗어나면 예외가 발생한다.")
    @Test
    void 관리자_목록_조회시_페이징_값이_유효하지_않으면_예외가_발생한다() {
        // when & then
        assertThatThrownBy(() -> inquiryService.getAdminInquiries(null, -1, 10))
                .isInstanceOf(InquiryException.class)
                .hasMessage(InquiryErrorCode.INQUIRY_INVALID_INPUT.getMessage());
        assertThatThrownBy(() -> inquiryService.getAdminInquiries(null, 0, 0))
                .isInstanceOf(InquiryException.class)
                .hasMessage(InquiryErrorCode.INQUIRY_INVALID_INPUT.getMessage());
        assertThatThrownBy(() -> inquiryService.getAdminInquiries(null, 0, 101))
                .isInstanceOf(InquiryException.class)
                .hasMessage(InquiryErrorCode.INQUIRY_INVALID_INPUT.getMessage());
    }

    @DisplayName("관리자는 자격증명 없이 문의 상세를 조회할 수 있다.")
    @Test
    void 관리자는_자격증명_없이_문의_상세를_조회할_수_있다() {
        // given
        Inquiry inquiry = inquiry(1L, ENCODED_PASSWORD);
        given(inquiryRepository.findById(1L)).willReturn(Optional.of(inquiry));

        // when
        InquiryDetailResponse response = inquiryService.getAdminInquiryDetail(1L);

        // then
        assertThat(response.inquiryId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo(TEST_EMAIL);
    }

    @DisplayName("관리자 상세 조회 시 존재하지 않는 문의면 문의 없음 예외가 발생한다.")
    @Test
    void 관리자_상세_조회시_존재하지_않는_문의면_예외가_발생한다() {
        // given
        given(inquiryRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inquiryService.getAdminInquiryDetail(99L))
                .isInstanceOf(InquiryException.class)
                .hasMessage(InquiryErrorCode.INQUIRY_NOT_FOUND.getMessage());
    }

    @DisplayName("답변 등록 시 문의 상태가 ANSWERED로 변경되고 답변 시각이 설정된다.")
    @Test
    void 답변_등록시_상태가_ANSWERED로_변경된다() {
        // given
        Inquiry inquiry = inquiry(1L, ENCODED_PASSWORD);
        given(inquiryRepository.findById(1L)).willReturn(Optional.of(inquiry));

        // when
        inquiryService.answerInquiry(1L, new AnswerInquiryRequest("답변 내용입니다."));

        // then
        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.ANSWERED);
        assertThat(inquiry.getAnswerContent()).isEqualTo("답변 내용입니다.");
        assertThat(inquiry.getAnsweredAt()).isNotNull();
    }

    @DisplayName("이미 답변된 문의에 재답변하면 답변 내용이 덮어써지고 답변 시각이 갱신된다.")
    @Test
    void 이미_답변된_문의에_재답변하면_답변이_덮어써진다() {
        // given
        Inquiry inquiry = inquiry(1L, ENCODED_PASSWORD);
        inquiry.answer("첫 번째 답변");
        LocalDateTime firstAnsweredAt = inquiry.getAnsweredAt();
        given(inquiryRepository.findById(1L)).willReturn(Optional.of(inquiry));

        // when
        inquiryService.answerInquiry(1L, new AnswerInquiryRequest("수정된 답변"));

        // then
        assertThat(inquiry.getAnswerContent()).isEqualTo("수정된 답변");
        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.ANSWERED);
        assertThat(inquiry.getAnsweredAt()).isAfterOrEqualTo(firstAnsweredAt);
    }

    @DisplayName("존재하지 않는 문의에 답변하면 문의 없음 예외가 발생한다.")
    @Test
    void 존재하지_않는_문의에_답변하면_예외가_발생한다() {
        // given
        given(inquiryRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inquiryService.answerInquiry(99L, new AnswerInquiryRequest("답변")))
                .isInstanceOf(InquiryException.class)
                .hasMessage(InquiryErrorCode.INQUIRY_NOT_FOUND.getMessage());
    }
}
