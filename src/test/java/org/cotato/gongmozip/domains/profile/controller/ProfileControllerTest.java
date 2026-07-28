package org.cotato.gongmozip.domains.profile.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.profile.dto.response.ProfileResponse.ProjectAiSummaryResponse;
import org.cotato.gongmozip.domains.profile.dto.response.ProfileResponse.PublicProfileResponse;
import org.cotato.gongmozip.domains.profile.service.ProfileService;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private ProfileService profileService;

    @Test
    void 인증된_사용자가_공개_프로필을_조회할_수_있다() throws Exception {
        Member member =
                Member.builder().memberId(1L).email("user@gongmozip.com").build();
        CustomUserDetails userDetails = new CustomUserDetails(member);

        given(profileService.getPublicProfile(1L))
                .willReturn(new PublicProfileResponse(
                        1L, "러너", null, "서울 소재 대학교", "학교", 3, "컴퓨터공학", null, List.of(), List.of(), List.of()));

        mockMvc.perform(get("/api/public/profiles/{profileId}", 1L).with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileId").value(1L))
                .andExpect(jsonPath("$.data.nickname").value("러너"));
    }

    @Test
    void AI_요약_생성_요청_시_202_ACCEPTED_를_반환한다() throws Exception {
        Member member =
                Member.builder().memberId(1L).email("user@gongmozip.com").build();
        CustomUserDetails userDetails = new CustomUserDetails(member);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        mockMvc.perform(post("/api/profiles/{profileId}/projects/{projectId}/ai-summary", 1L, 10L)
                        .with(user(userDetails)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value("PROFILE_202_1"));
    }

    @Test
    void AI_요약_조회_시_200_OK_및_요약정보를_반환한다() throws Exception {
        Member member =
                Member.builder().memberId(1L).email("user@gongmozip.com").build();
        CustomUserDetails userDetails = new CustomUserDetails(member);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        ProjectAiSummaryResponse response = new ProjectAiSummaryResponse("프로젝트 요약", "COMPLETED", LocalDateTime.now());
        given(profileService.getProjectAiSummary(1L, 10L, member)).willReturn(response);

        mockMvc.perform(get("/api/profiles/{profileId}/projects/{projectId}/ai-summary", 1L, 10L)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PROFILE_200_23"))
                .andExpect(jsonPath("$.data.summary").value("프로젝트 요약"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void AI_요약_재생성_요청_시_202_ACCEPTED_를_반환한다() throws Exception {
        Member member =
                Member.builder().memberId(1L).email("user@gongmozip.com").build();
        CustomUserDetails userDetails = new CustomUserDetails(member);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        mockMvc.perform(post("/api/profiles/{profileId}/projects/{projectId}/ai-summary/regenerate", 1L, 10L)
                        .with(user(userDetails)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value("PROFILE_202_2"));
    }
}
