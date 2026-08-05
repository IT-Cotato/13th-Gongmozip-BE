package org.cotato.gongmozip.domains.inquiry.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.inquiry.dto.request.InquiryRequest.CreateInquiryRequest;
import org.cotato.gongmozip.domains.inquiry.dto.request.InquiryRequest.InquiryAuthRequest;
import org.cotato.gongmozip.domains.inquiry.dto.response.InquiryResponse.InquiryDetailResponse;
import org.cotato.gongmozip.domains.inquiry.dto.response.InquiryResponse.InquiryListResponse;
import org.cotato.gongmozip.domains.inquiry.dto.response.InquiryResponse.InquirySummaryResponse;
import org.cotato.gongmozip.domains.inquiry.enums.InquiryStatus;
import org.cotato.gongmozip.domains.inquiry.exception.InquiryException;
import org.cotato.gongmozip.domains.inquiry.exception.codes.InquiryErrorCode;
import org.cotato.gongmozip.domains.inquiry.service.InquiryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private InquiryService inquiryService;

    private static final String TEST_EMAIL = "test@gongmozip.com";
    private static final String TEST_PASSWORD = "1234";

    @Test
    @DisplayName("비회원은 이메일과 문의 비밀번호로 문의를 등록할 수 있다")
    void createInquirySucceedsWithoutAuthentication() throws Exception {
        CreateInquiryRequest request = new CreateInquiryRequest(TEST_EMAIL, TEST_PASSWORD, "문의 제목", "문의 내용입니다.");

        mockMvc.perform(post("/api/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("INQUIRY_201_1"));
    }

    @Test
    @DisplayName("문의 제목이 20자를 초과하면 400 응답을 반환한다")
    void createInquiryFailsWhenTitleExceedsMaxLength() throws Exception {
        CreateInquiryRequest request =
                new CreateInquiryRequest(TEST_EMAIL, TEST_PASSWORD, "가나다라마바사아자차카타파하가나다라마바사", "문의 내용입니다.");

        mockMvc.perform(post("/api/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("문의 비밀번호가 숫자 4자리가 아니면 400 응답을 반환한다")
    void createInquiryFailsWhenPasswordIsNotFourDigits() throws Exception {
        CreateInquiryRequest request = new CreateInquiryRequest(TEST_EMAIL, "12a4", "문의 제목", "문의 내용입니다.");

        mockMvc.perform(post("/api/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이메일 형식이 올바르지 않으면 400 응답을 반환한다")
    void createInquiryFailsWhenEmailIsInvalid() throws Exception {
        CreateInquiryRequest request = new CreateInquiryRequest("invalid-email", TEST_PASSWORD, "문의 제목", "문의 내용입니다.");

        mockMvc.perform(post("/api/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이메일과 문의 비밀번호가 일치하는 문의 내역을 조회할 수 있다")
    void getInquiriesReturnsMatchedInquiries() throws Exception {
        InquiryAuthRequest request = new InquiryAuthRequest(TEST_EMAIL, TEST_PASSWORD);
        InquiryListResponse response = new InquiryListResponse(
                List.of(new InquirySummaryResponse(
                        1L, InquiryStatus.PENDING, "문의 제목", "문의 내용 미리보기", LocalDateTime.now())),
                1);
        given(inquiryService.getInquiries(any(InquiryAuthRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/inquiries/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("INQUIRY_200_1"))
                .andExpect(jsonPath("$.data.inquiries[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalCount").value(1));
    }

    @Test
    @DisplayName("답변 완료된 문의는 상세 조회 시 답변 내용이 포함된다")
    void getInquiryDetailReturnsAnswerWhenAnswered() throws Exception {
        InquiryAuthRequest request = new InquiryAuthRequest(TEST_EMAIL, TEST_PASSWORD);
        InquiryDetailResponse response = new InquiryDetailResponse(
                1L,
                InquiryStatus.ANSWERED,
                "문의 제목",
                "문의 내용입니다.",
                TEST_EMAIL,
                LocalDateTime.now(),
                "고객님의 문의에 대한 답변입니다.",
                LocalDateTime.now());
        given(inquiryService.getInquiryDetail(eq(1L), any(InquiryAuthRequest.class)))
                .willReturn(response);

        mockMvc.perform(post("/api/inquiries/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("INQUIRY_200_2"))
                .andExpect(jsonPath("$.data.status").value("ANSWERED"))
                .andExpect(jsonPath("$.data.answerContent").value("고객님의 문의에 대한 답변입니다."));
    }

    @Test
    @DisplayName("자격증명이 일치하는 문의가 없으면 404 응답을 반환한다")
    void getInquiriesFailsWhenNoInquiryMatches() throws Exception {
        InquiryAuthRequest request = new InquiryAuthRequest(TEST_EMAIL, "9999");
        given(inquiryService.getInquiries(any(InquiryAuthRequest.class)))
                .willThrow(new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND));

        mockMvc.perform(post("/api/inquiries/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INQUIRY_404_1"));
    }
}
