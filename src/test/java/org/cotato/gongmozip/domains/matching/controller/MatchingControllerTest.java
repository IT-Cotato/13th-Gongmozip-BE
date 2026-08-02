package org.cotato.gongmozip.domains.matching.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.LeaderRecommendationCreateResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.LeaderRecommendationDetailResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.MatchingExplanationResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.MatchingReasonCreateResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.MatchingReasonDetailResponse;
import org.cotato.gongmozip.domains.matching.service.MatchingService;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
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
class MatchingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private MatchingService matchingService;

    @MockitoBean
    private MemberRepository memberRepository;

    private Member member;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        member = Member.builder().memberId(1L).email("test@gongmozip.com").build();
        userDetails = new CustomUserDetails(member);
    }

    @DisplayName("AI 분석 매칭 설명 조회 요청 시 성공한다")
    @Test
    void AI_분석_매칭_설명_조회_요청_시_성공한다() throws Exception {
        // given
        MatchingExplanationResponse response =
                new MatchingExplanationResponse("제목", "요약", new ArrayList<>(), "참고사항", LocalDateTime.now());
        given(matchingService.getMatchingExplanation()).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/matching-explanations").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("MATCHING_200_4"))
                .andExpect(jsonPath("$.data.title").value("제목"));
    }

    @DisplayName("매칭 추천 사유 생성 요청 시 202 Accepted를 반환한다")
    @Test
    void 매칭_추천_사유_생성_요청_시_202_Accepted를_반환한다() throws Exception {
        // given
        MatchingReasonCreateResponse response =
                new MatchingReasonCreateResponse(40L, 10L, "PENDING", LocalDateTime.now());
        given(memberRepository.findById(any())).willReturn(Optional.of(member));
        given(matchingService.createMatchingReason(any(), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/ai/matching-results/10/reason")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value(202))
                .andExpect(jsonPath("$.code").value("MATCHING_202_1"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @DisplayName("매칭 추천 사유 조회 요청 시 성공한다")
    @Test
    void 매칭_추천_사유_조회_요청_시_성공한다() throws Exception {
        // given
        MatchingReasonDetailResponse response = new MatchingReasonDetailResponse(
                40L,
                10L,
                "COMPLETED",
                "헤드라인",
                "요약",
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                80,
                90,
                85,
                75,
                null,
                LocalDateTime.now(),
                LocalDateTime.now());
        given(memberRepository.findById(any())).willReturn(Optional.of(member));
        given(matchingService.getMatchingReason(any(), any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/ai/matching-results/10/reason").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("MATCHING_200_5"))
                .andExpect(jsonPath("$.data.headline").value("헤드라인"));
    }

    @DisplayName("AI 팀장 추천 생성 요청 시 202 Accepted를 반환한다")
    @Test
    void AI_팀장_추천_생성_요청_시_202_Accepted를_반환한다() throws Exception {
        // given
        LeaderRecommendationCreateResponse response =
                new LeaderRecommendationCreateResponse(50L, 30L, "PENDING", LocalDateTime.now());
        given(memberRepository.findById(any())).willReturn(Optional.of(member));
        given(matchingService.createLeaderRecommendation(any(), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/ai/teams/30/leader-recommendation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value(202))
                .andExpect(jsonPath("$.code").value("MATCHING_202_2"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @DisplayName("AI 팀장 추천 결과 조회 요청 시 성공한다")
    @Test
    void AI_팀장_추천_결과_조회_요청_시_성공한다() throws Exception {
        // given
        LeaderRecommendationDetailResponse response = new LeaderRecommendationDetailResponse(
                50L,
                30L,
                "COMPLETED",
                1L,
                "닉네임",
                "이유",
                new ArrayList<>(),
                "요약",
                "주의사항",
                null,
                LocalDateTime.now(),
                LocalDateTime.now());
        given(memberRepository.findById(any())).willReturn(Optional.of(member));
        given(matchingService.getLeaderRecommendation(any(), any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/ai/teams/30/leader-recommendation").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("MATCHING_200_6"))
                .andExpect(jsonPath("$.data.recommendedMemberNickname").value("닉네임"));
    }
}
