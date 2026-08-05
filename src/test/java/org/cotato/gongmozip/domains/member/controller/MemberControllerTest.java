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
                1L, "test@gongmozip.com", "홍길동", Gender.MALE, LocalDate.of(2000, 1, 1), "GOOGLE", true, true, false);
        given(memberService.getMemberMe(1L)).willReturn(response);

        mockMvc.perform(get("/api/members/me").with(user(new CustomUserDetails(member))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MEMBER_200_4"))
                .andExpect(jsonPath("$.data.email").value("test@gongmozip.com"))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.snsLinked").value(true));
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
}
