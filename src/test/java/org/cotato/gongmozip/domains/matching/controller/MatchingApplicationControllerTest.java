package org.cotato.gongmozip.domains.matching.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.cotato.gongmozip.domains.matching.dto.request.MatchingApplicationRequest.ApplyRequest;
import org.cotato.gongmozip.domains.matching.dto.response.MatchingApplicationResponse.ApplicationResponse;
import org.cotato.gongmozip.domains.matching.dto.response.MatchingApplicationResponse.EligibilityResponse;
import org.cotato.gongmozip.domains.matching.dto.response.MatchingApplicationResponse.TodayApplicationResponse;
import org.cotato.gongmozip.domains.matching.dto.response.MatchingApplicationResponse.WithdrawalResponse;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.matching.enums.WithdrawalType;
import org.cotato.gongmozip.domains.matching.exception.MatchingException;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.matching.service.MatchingApplicationService;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
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
class MatchingApplicationControllerTest {

    private static final LocalDate APPLICATION_DATE = LocalDate.of(2026, 7, 31);

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private MatchingApplicationService matchingApplicationService;

    @MockitoBean
    private MemberRepository memberRepository;

    private final Member member =
            Member.builder().memberId(1L).email("member@example.com").build();
    private final CustomUserDetails userDetails = new CustomUserDetails(member);

    @DisplayName("인증된 회원은 매칭 신청 자격과 오늘의 참여 인원을 조회한다.")
    @Test
    void getEligibility() throws Exception {
        given(matchingApplicationService.getEligibility(1L))
                .willReturn(new EligibilityResponse(
                        true, List.of(), true, true, false, null, APPLICATION_DATE.atTime(14, 0), 12));

        mockMvc.perform(get("/api/matching/applications/eligibility").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MATCHING_200_1"))
                .andExpect(jsonPath("$.data.eligible").value(true))
                .andExpect(jsonPath("$.data.participantCount").value(12));
    }

    @DisplayName("오늘 신청이 없으면 신청 정보 필드를 null로 포함한다.")
    @Test
    void getTodayApplicationReturnsNullFieldsWhenNotApplied() throws Exception {
        given(matchingApplicationService.getTodayApplication(1L))
                .willReturn(
                        new TodayApplicationResponse(false, null, "NONE", null, null, null, null, null, null, null));

        mockMvc.perform(get("/api/matching/applications/me/today").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appliedToday").value(false))
                .andExpect(jsonPath("$.data.status").value("NONE"))
                .andExpect(jsonPath("$.data.applicationId").value(nullValue()))
                .andExpect(jsonPath("$.data.applicationDate").value(nullValue()))
                .andExpect(jsonPath("$.data.contestCategory").value(nullValue()))
                .andExpect(jsonPath("$.data.leaderPreference").value(nullValue()))
                .andExpect(jsonPath("$.data.skillScore").value(nullValue()))
                .andExpect(jsonPath("$.data.skillGroup").value(nullValue()))
                .andExpect(jsonPath("$.data.collaborationDistance").value(nullValue()))
                .andExpect(jsonPath("$.data.withdrawal").value(nullValue()));
    }

    @DisplayName("프로필, 카테고리, 팀장 선호, 주의사항을 제출하면 매칭풀에 입장한다.")
    @Test
    void apply() throws Exception {
        ApplyRequest request = new ApplyRequest(10L, InterestCategory.IT_AI_TECH, LeaderPreference.WANTS, true);
        given(matchingApplicationService.apply(eq(1L), any(ApplyRequest.class)))
                .willReturn(new ApplicationResponse(
                        100L,
                        "WAITING",
                        APPLICATION_DATE,
                        InterestCategory.IT_AI_TECH,
                        LeaderPreference.WANTS,
                        new BigDecimal("52.00"),
                        2,
                        100,
                        APPLICATION_DATE.atTime(14, 0)));

        mockMvc.perform(post("/api/matching/applications")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("MATCHING_201_1"))
                .andExpect(jsonPath("$.data.status").value("WAITING"))
                .andExpect(jsonPath("$.data.collaborationDistance").value(100));
    }

    @DisplayName("주의사항에 동의하지 않으면 매칭을 신청할 수 없다.")
    @Test
    void applyRejectsUnconfirmedNotice() throws Exception {
        ApplyRequest request = new ApplyRequest(10L, InterestCategory.IT_AI_TECH, LeaderPreference.NEUTRAL, false);

        mockMvc.perform(post("/api/matching/applications")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_1"));
    }

    @DisplayName("프로필 학점이 잘못되면 구조화된 매칭 400 응답을 반환한다.")
    @Test
    void applyRejectsInvalidProfileGpa() throws Exception {
        ApplyRequest request = new ApplyRequest(10L, InterestCategory.IT_AI_TECH, LeaderPreference.NEUTRAL, true);
        given(matchingApplicationService.apply(eq(1L), any(ApplyRequest.class)))
                .willThrow(new MatchingException(MatchingErrorCode.INVALID_PROFILE_GPA));

        mockMvc.perform(post("/api/matching/applications")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("MATCHING_400_5"))
                .andExpect(jsonPath("$.message").value("프로필의 학점 입력값이 올바르지 않습니다."));
    }

    @DisplayName("프론트 분기 없이 하나의 철회 API가 백엔드 판정 결과를 반환한다.")
    @Test
    void withdraw() throws Exception {
        given(matchingApplicationService.withdraw(1L, 100L))
                .willReturn(new WithdrawalResponse(100L, "PASSED", WithdrawalType.PENALIZED_PASS, 5, 95));

        mockMvc.perform(post("/api/matching/applications/100/withdraw").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MATCHING_200_3"))
                .andExpect(jsonPath("$.data.withdrawalType").value("PENALIZED_PASS"))
                .andExpect(jsonPath("$.data.collaborationPenalty").value(5));
    }

    @DisplayName("비인증 사용자는 매칭 신청 API를 사용할 수 없다.")
    @Test
    void applyRejectsUnauthenticatedUser() throws Exception {
        mockMvc.perform(post("/api/matching/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
