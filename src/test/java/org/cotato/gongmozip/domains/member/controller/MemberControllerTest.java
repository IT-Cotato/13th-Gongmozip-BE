package org.cotato.gongmozip.domains.member.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.cotato.gongmozip.domains.member.dto.request.MemberRequest.UpdateMarketingConsentRequest;
import org.cotato.gongmozip.domains.member.dto.request.MemberRequest.UpdateMemberMeRequest;
import org.cotato.gongmozip.domains.member.dto.response.MemberResponse.MemberMeResponse;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.enums.Gender;
import org.cotato.gongmozip.domains.member.enums.MemberRole;
import org.cotato.gongmozip.domains.member.service.MemberService;
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
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private org.cotato.gongmozip.domains.member.service.MemberWithdrawService memberWithdrawService;

    private Member member() {
        return Member.builder()
                .memberId(1L)
                .email("test@gongmozip.com")
                .role(MemberRole.USER)
                .build();
    }

    @Test
    @DisplayName("로그인한 회원은 자신의 상세 정보를 조회할 수 있다")
    void getMyInfoReturnsMemberDetails() throws Exception {
        Member member = member();
        MemberMeResponse response = new MemberMeResponse(
                1L,
                "test@gongmozip.com",
                "홍길동",
                Gender.MALE,
                LocalDate.of(2000, 1, 1),
                "GOOGLE",
                true,
                true,
                false,
                "http://profile-image.com/myphoto.png");
        given(memberService.getMemberMe(1L)).willReturn(response);

        mockMvc.perform(get("/api/members/me").with(user(new CustomUserDetails(member))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MEMBER_200_4"))
                .andExpect(jsonPath("$.data.email").value("test@gongmozip.com"))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.snsLinked").value(true))
                .andExpect(jsonPath("$.data.profileImageUrl").value("http://profile-image.com/myphoto.png"));
    }

    @Test
    @DisplayName("로그인한 회원은 자신의 상세 정보를 수정할 수 있다")
    void updateMyInfoModifiesDetails() throws Exception {
        Member member = member();
        UpdateMemberMeRequest request = new UpdateMemberMeRequest("수정된이름", Gender.FEMALE, LocalDate.of(1995, 5, 5));

        mockMvc.perform(patch("/api/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(new CustomUserDetails(member))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MEMBER_200_5"));
    }

    @Test
    @DisplayName("로그인한 회원은 마케팅 수신동의 여부를 수정할 수 있다")
    void updateMarketingConsentModifiesConsent() throws Exception {
        Member member = member();
        UpdateMarketingConsentRequest request = new UpdateMarketingConsentRequest(true, true);

        mockMvc.perform(patch("/api/members/me/marketing-consents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(new CustomUserDetails(member))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MEMBER_200_6"));
    }

    @Test
    @DisplayName("로그인한 회원은 프로필 사진 업로드용 Presigned URL을 발급받을 수 있다")
    void getProfileImagePresignedUrlSuccess() throws Exception {
        Member member = member();
        org.cotato.gongmozip.domains.member.dto.request.MemberRequest.GetProfileImagePresignedUrlRequest request =
                new org.cotato.gongmozip.domains.member.dto.request.MemberRequest.GetProfileImagePresignedUrlRequest(
                        "my_photo.png", "image/png");
        org.cotato.gongmozip.domains.upload.dto.response.GetPresignedUrlResponse response =
                new org.cotato.gongmozip.domains.upload.dto.response.GetPresignedUrlResponse(
                        "http://presigned-url", "http://image-url", "image/png");
        given(memberService.getProfileImagePresignedUrl(request)).willReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/api/members/me/profile-image/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(new CustomUserDetails(member))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MEMBER_200_7"))
                .andExpect(jsonPath("$.data.uploadUrl").value("http://presigned-url"))
                .andExpect(jsonPath("$.data.imageUrl").value("http://image-url"));
    }

    @Test
    @DisplayName("로그인한 회원은 프로필 사진을 업데이트할 수 있다")
    void updateProfileImageSuccess() throws Exception {
        Member member = member();
        org.cotato.gongmozip.domains.member.dto.request.MemberRequest.UpdateProfileImageRequest request =
                new org.cotato.gongmozip.domains.member.dto.request.MemberRequest.UpdateProfileImageRequest(
                        "http://image-url");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                                "/api/members/me/profile-image")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(new CustomUserDetails(member))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MEMBER_200_8"));
    }

    @Test
    @DisplayName("로그인한 회원은 탈퇴 사유와 비밀번호로 회원 탈퇴할 수 있다")
    void withdrawSuccess() throws Exception {
        Member member = member();
        org.cotato.gongmozip.domains.member.dto.request.MemberRequest.WithdrawMemberRequest request =
                new org.cotato.gongmozip.domains.member.dto.request.MemberRequest.WithdrawMemberRequest(
                        "password123!",
                        org.cotato.gongmozip.domains.member.enums.WithdrawalReasonType.MATCHING_DISSATISFIED,
                        null);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(new CustomUserDetails(member))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MEMBER_200_9"));
    }

    @Test
    @DisplayName("탈퇴 사유 없이 회원 탈퇴를 요청하면 실패한다")
    void withdrawFailsWithoutReason() throws Exception {
        Member member = member();
        org.cotato.gongmozip.domains.member.dto.request.MemberRequest.WithdrawMemberRequest request =
                new org.cotato.gongmozip.domains.member.dto.request.MemberRequest.WithdrawMemberRequest(
                        "password123!", null, null);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(new CustomUserDetails(member))))
                .andExpect(status().isBadRequest());
    }
}
