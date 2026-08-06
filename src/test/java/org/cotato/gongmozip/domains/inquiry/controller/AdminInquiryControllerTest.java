package org.cotato.gongmozip.domains.inquiry.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.inquiry.dto.request.InquiryRequest.AnswerInquiryRequest;
import org.cotato.gongmozip.domains.inquiry.dto.response.InquiryResponse.AdminInquiryListResponse;
import org.cotato.gongmozip.domains.inquiry.dto.response.InquiryResponse.AdminInquirySummaryResponse;
import org.cotato.gongmozip.domains.inquiry.dto.response.InquiryResponse.InquiryDetailResponse;
import org.cotato.gongmozip.domains.inquiry.enums.InquiryStatus;
import org.cotato.gongmozip.domains.inquiry.exception.InquiryException;
import org.cotato.gongmozip.domains.inquiry.exception.codes.InquiryErrorCode;
import org.cotato.gongmozip.domains.inquiry.service.InquiryService;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.enums.MemberRole;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
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
class AdminInquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private InquiryService inquiryService;

    private CustomUserDetails adminDetails() {
        Member admin = Member.builder()
                .memberId(1L)
                .email("admin@gongmozip.com")
                .role(MemberRole.ADMIN)
                .build();
        return new CustomUserDetails(admin);
    }

    private CustomUserDetails userDetails() {
        Member user = Member.builder()
                .memberId(2L)
                .email("user@gongmozip.com")
                .role(MemberRole.USER)
                .build();
        return new CustomUserDetails(user);
    }

    @DisplayName("관리자는 문의 목록을 조회할 수 있다.")
    @Test
    void 관리자는_문의_목록을_조회할_수_있다() throws Exception {
        // given
        AdminInquiryListResponse response = new AdminInquiryListResponse(
                List.of(new AdminInquirySummaryResponse(
                        1L, InquiryStatus.PENDING, "문의 제목", "문의 내용", "guest@test.com", LocalDateTime.now())),
                0,
                10,
                1,
                1,
                false);
        given(inquiryService.getAdminInquiries(any(), any(), any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/admin/inquiries").with(user(adminDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("INQUIRY_200_3"))
                .andExpect(jsonPath("$.data.inquiries[0].email").value("guest@test.com"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @DisplayName("관리자는 상태 필터로 문의 목록을 조회할 수 있다.")
    @Test
    void 관리자는_상태_필터로_문의_목록을_조회할_수_있다() throws Exception {
        // given
        AdminInquiryListResponse response = new AdminInquiryListResponse(List.of(), 0, 10, 0, 0, false);
        given(inquiryService.getAdminInquiries(eq("PENDING"), any(), any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/admin/inquiries").param("status", "PENDING").with(user(adminDetails())))
                .andExpect(status().isOk());

        verify(inquiryService).getAdminInquiries(eq("PENDING"), any(), any());
    }

    @DisplayName("유효하지 않은 상태 값으로 목록 조회 시 400 에러가 발생한다.")
    @Test
    void 유효하지_않은_상태값으로_목록_조회시_400_에러가_발생한다() throws Exception {
        // given
        given(inquiryService.getAdminInquiries(eq("INVALID"), any(), any()))
                .willThrow(new InquiryException(InquiryErrorCode.INQUIRY_INVALID_INPUT));

        // when & then
        mockMvc.perform(get("/api/admin/inquiries").param("status", "INVALID").with(user(adminDetails())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INQUIRY_400_1"));
    }

    @DisplayName("관리자는 문의 상세를 조회할 수 있다.")
    @Test
    void 관리자는_문의_상세를_조회할_수_있다() throws Exception {
        // given
        InquiryDetailResponse response = new InquiryDetailResponse(
                1L,
                InquiryStatus.ANSWERED,
                "문의 제목",
                "문의 내용",
                "guest@test.com",
                LocalDateTime.now(),
                "답변 내용",
                LocalDateTime.now());
        given(inquiryService.getAdminInquiryDetail(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/admin/inquiries/1").with(user(adminDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("INQUIRY_200_4"))
                .andExpect(jsonPath("$.data.answerContent").value("답변 내용"));
    }

    @DisplayName("관리자는 문의에 답변을 등록할 수 있다.")
    @Test
    void 관리자는_문의에_답변을_등록할_수_있다() throws Exception {
        // given
        AnswerInquiryRequest request = new AnswerInquiryRequest("답변 내용입니다.");

        // when & then
        mockMvc.perform(patch("/api/admin/inquiries/1/answer")
                        .with(user(adminDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("INQUIRY_200_5"));

        verify(inquiryService).answerInquiry(eq(1L), any(AnswerInquiryRequest.class));
    }

    @DisplayName("답변 내용이 비어 있으면 400 에러가 발생한다.")
    @Test
    void 답변_내용이_비어_있으면_400_에러가_발생한다() throws Exception {
        // given
        AnswerInquiryRequest request = new AnswerInquiryRequest(" ");

        // when & then
        mockMvc.perform(patch("/api/admin/inquiries/1/answer")
                        .with(user(adminDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("답변 내용이 1000자를 초과하면 400 에러가 발생한다.")
    @Test
    void 답변_내용이_1000자를_초과하면_400_에러가_발생한다() throws Exception {
        // given
        AnswerInquiryRequest request = new AnswerInquiryRequest("가".repeat(1001));

        // when & then
        mockMvc.perform(patch("/api/admin/inquiries/1/answer")
                        .with(user(adminDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("존재하지 않는 문의에 답변하면 404 에러가 발생한다.")
    @Test
    void 존재하지_않는_문의에_답변하면_404_에러가_발생한다() throws Exception {
        // given
        willThrow(new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND))
                .given(inquiryService)
                .answerInquiry(eq(99L), any(AnswerInquiryRequest.class));
        AnswerInquiryRequest request = new AnswerInquiryRequest("답변 내용입니다.");

        // when & then
        mockMvc.perform(patch("/api/admin/inquiries/99/answer")
                        .with(user(adminDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INQUIRY_404_1"));
    }

    @DisplayName("일반 유저가 관리자 문의 API 요청 시 403 에러가 발생한다.")
    @Test
    void 일반_유저는_관리자_문의_API에_접근할_수_없다() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/inquiries").with(user(userDetails()))).andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/inquiries/1").with(user(userDetails()))).andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/inquiries/1/answer")
                        .with(user(userDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnswerInquiryRequest("답변"))))
                .andExpect(status().isForbidden());
    }

    @DisplayName("비로그인 사용자가 관리자 문의 API 요청 시 401 에러가 발생한다.")
    @Test
    void 비로그인_사용자는_관리자_문의_API에_접근할_수_없다() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/inquiries")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/inquiries/1")).andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/admin/inquiries/1/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnswerInquiryRequest("답변"))))
                .andExpect(status().isUnauthorized());
    }
}
