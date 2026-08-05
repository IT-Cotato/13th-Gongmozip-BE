package org.cotato.gongmozip.domains.contest.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.cotato.gongmozip.domains.contest.dto.response.ContestResponse.ContestSummaryResponse;
import org.cotato.gongmozip.domains.contest.dto.response.RecommendationResponse.RecommendationReasonResponse;
import org.cotato.gongmozip.domains.contest.service.ContestRecommendationService;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.enums.MemberRole;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ContestRecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContestRecommendationService recommendationService;

    private Member member() {
        return Member.builder()
                .memberId(1L)
                .email("test@gongmozip.com")
                .role(MemberRole.USER)
                .build();
    }

    @Test
    @DisplayName("로그인한 회원은 홈 추천 공모전 목록을 조회할 수 있다")
    void getHomeRecommendationsReturnsList() throws Exception {
        Member member = member();
        ContestSummaryResponse summary = new ContestSummaryResponse(
                1L, "추천 공모전", "IT", "OPEN", "주최사", "thumb.png", java.time.LocalDateTime.now(), 5);
        given(recommendationService.getHomeRecommendations(1L)).willReturn(List.of(summary));

        mockMvc.perform(get("/api/recommendations/contests").with(user(new CustomUserDetails(member))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CONTEST_200_8"))
                .andExpect(jsonPath("$.data[0].contestId").value(1L))
                .andExpect(jsonPath("$.data[0].title").value("추천 공모전"));
    }

    @Test
    @DisplayName("로그인한 회원은 프로필별 추천 공모전 목록을 조회할 수 있다")
    void getProfileRecommendationsReturnsList() throws Exception {
        Member member = member();
        ContestSummaryResponse summary = new ContestSummaryResponse(
                2L, "프로필 추천 공모전", "IT", "OPEN", "주최사", "thumb.png", java.time.LocalDateTime.now(), 5);
        given(recommendationService.getProfileRecommendations(10L, 1L)).willReturn(List.of(summary));

        mockMvc.perform(get("/api/profiles/{profileId}/contest-recommendations", 10L)
                        .with(user(new CustomUserDetails(member))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CONTEST_200_8"))
                .andExpect(jsonPath("$.data[0].contestId").value(2L));
    }

    @Test
    @DisplayName("로그인한 회원은 추천 공모전 사유를 조회할 수 있다")
    void getRecommendationReasonReturnsText() throws Exception {
        Member member = member();
        RecommendationReasonResponse response = new RecommendationReasonResponse("추천 이유 설명");
        given(recommendationService.getRecommendationReason(100L, 1L)).willReturn(response);

        mockMvc.perform(get("/api/recommendations/contests/{contestId}/reason", 100L)
                        .with(user(new CustomUserDetails(member))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CONTEST_200_9"))
                .andExpect(jsonPath("$.data.reason").value("추천 이유 설명"));
    }
}
