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

    @DisplayName("인증되지 않은 사용자가 AI 매칭 설명을 요청하면 401 Unauthorized를 반환한다")
    @Test
    void 인증되지_않은_사용자가_AI_매칭_설명을_요청하면_401_Unauthorized를_반환한다() throws Exception {
        mockMvc.perform(get("/api/matching-explanations")).andExpect(status().isUnauthorized());
    }

    @DisplayName("매칭 결과에 권한이 없는 사용자가 추천 사유 조회를 요청하면 403 Forbidden을 반환한다")
    @Test
    void 매칭_결과에_권한이_없는_사용자가_추천_사유_조회를_요청하면_403_Forbidden을_반환한다() throws Exception {
        // given
        given(memberRepository.findById(any())).willReturn(Optional.of(member));
        given(matchingService.getMatchingReason(any(), any()))
                .willThrow(new org.cotato.gongmozip.domains.matching.exception.MatchingException(
                        org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode
                                .MATCHING_GROUP_ACCESS_DENIED));

        // when & then
        mockMvc.perform(get("/api/ai/matching-results/10/reason").with(user(userDetails)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("MATCHING_403_3"));
    }

    @DisplayName("추천 사유가 이미 생성 중일 때 생성 요청하면 409 Conflict를 반환한다")
    @Test
    void 추천_사유가_이미_생성_중일_때_생성_요청하면_409_Conflict를_반환한다() throws Exception {
        // given
        given(memberRepository.findById(any())).willReturn(Optional.of(member));
        given(matchingService.createMatchingReason(any(), any()))
                .willThrow(new org.cotato.gongmozip.domains.matching.exception.MatchingException(
                        org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode
                                .MATCHING_REASON_IN_PROGRESS));

        // when & then
        mockMvc.perform(post("/api/ai/matching-results/10/reason")
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("MATCHING_409_3"));
    }

    @DisplayName("존재하지 않는 매칭 그룹에 대한 추천 사유를 조회 요청하면 404 Not Found를 반환한다")
    @Test
    void 존재하지_않는_매칭_그룹에_대한_추천_사유를_조회_요청하면_404_Not_Found를_반환한다() throws Exception {
        // given
        given(memberRepository.findById(any())).willReturn(Optional.of(member));
        given(matchingService.getMatchingReason(any(), any()))
                .willThrow(new org.cotato.gongmozip.domains.matching.exception.MatchingException(
                        org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode
                                .MATCHING_GROUP_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/ai/matching-results/999/reason").with(user(userDetails)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("MATCHING_404_2"));
    }
}
