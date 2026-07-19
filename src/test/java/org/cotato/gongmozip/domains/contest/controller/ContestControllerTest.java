package org.cotato.gongmozip.domains.contest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.contest.dto.request.ContestRequest.SaveContestRequest;
import org.cotato.gongmozip.domains.contest.dto.response.ContestResponse.*;
import org.cotato.gongmozip.domains.contest.service.ContestService;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.enums.MemberRole;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
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
class ContestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private ContestService contestService;

    @MockitoBean
    private MemberRepository memberRepository;

    @DisplayName("관리자 권한을 가진 사용자는 공모전을 등록할 수 있다.")
    @Test
    void 관리자는_공모전을_등록할_수_있다() throws Exception {
        // given
        Member admin = Member.builder()
                .memberId(1L)
                .email("admin@gongmozip.com")
                .role(MemberRole.ADMIN)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(admin);

        SaveContestRequest request = new SaveContestRequest(
                "2026 공모전",
                "요약",
                "설명",
                "IT_AI_TECH",
                "OPEN",
                "주최",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(10),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null);

        ContestCreateResponse response = new ContestCreateResponse(
                1L, "2026 공모전", "IT_AI_TECH", "OPEN", LocalDateTime.now().plusDays(10), LocalDateTime.now());

        given(contestService.createContest(any(SaveContestRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/contests")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.contestId").value(1L))
                .andExpect(jsonPath("$.data.title").value("2026 공모전"));
    }

    @DisplayName("일반 권한을 가진 사용자가 공모전 등록 시 403 Forbidden 에러가 발생한다.")
    @Test
    void 일반유저는_공모전_등록할_수_없다() throws Exception {
        // given
        Member user = Member.builder()
                .memberId(2L)
                .email("user@gongmozip.com")
                .role(MemberRole.USER)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        SaveContestRequest request = new SaveContestRequest(
                "2026 공모전",
                "요약",
                "설명",
                "IT_AI_TECH",
                "OPEN",
                "주최",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(10),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null);

        // when & then
        mockMvc.perform(post("/api/contests")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @DisplayName("로그인한 사용자는 공모전 목록을 조회할 수 있다.")
    @Test
    void 로그인한_사용자는_공모전_목록을_조회할_수_있다() throws Exception {
        // given
        Member user = Member.builder()
                .memberId(2L)
                .email("user@gongmozip.com")
                .role(MemberRole.USER)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        ContestSummaryResponse contestSummary = new ContestSummaryResponse(
                1L,
                "공모전",
                "IT_AI_TECH",
                "OPEN",
                "주최",
                "http://thumb",
                LocalDateTime.now().plusDays(10),
                10);
        ContestListResponse listResponse = new ContestListResponse(List.of(contestSummary), 0, 20, 1, 1, false);

        given(contestService.getContests(any(), any(), any(), any(), any(), any()))
                .willReturn(listResponse);

        // when & then
        mockMvc.perform(get("/api/contests")
                        .with(user(userDetails))
                        .param("sort", "deadlineAsc")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contests[0].contestId").value(1L))
                .andExpect(jsonPath("$.data.contests[0].title").value("공모전"));
    }

    @DisplayName("비로그인 사용자가 공모전 목록 조회 시 401 Unauthorized 에러가 발생한다.")
    @Test
    void 비로그인_사용자_목록_조회시_401_에러가_발생한다() throws Exception {
        // when & then
        mockMvc.perform(get("/api/contests")
                        .param("sort", "deadlineAsc")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("로그인한 사용자는 공모전 상세 정보를 조회할 수 있다.")
    @Test
    void 로그인한_사용자는_공모전_상세를_조회할_수_있다() throws Exception {
        // given
        Member user = Member.builder()
                .memberId(2L)
                .email("user@gongmozip.com")
                .role(MemberRole.USER)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        ContestDetailResponse detailResponse = new ContestDetailResponse(
                1L,
                "공모전",
                "요약",
                "설명",
                "IT_AI_TECH",
                "OPEN",
                "주최",
                null,
                LocalDateTime.now().plusDays(10),
                null,
                null,
                null,
                null,
                "http://thumb",
                List.of(),
                null,
                false,
                null,
                null,
                10,
                100);

        given(contestService.getContestDetail(1L)).willReturn(detailResponse);

        // when & then
        mockMvc.perform(get("/api/contests/{contestId}", 1L).with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contestId").value(1L))
                .andExpect(jsonPath("$.data.title").value("공모전"));
    }

    @DisplayName("비로그인 사용자가 공모전 상세 조회 시 401 Unauthorized 에러가 발생한다.")
    @Test
    void 비로그인_사용자_상세_조회시_401_에러가_발생한다() throws Exception {
        // when & then
        mockMvc.perform(get("/api/contests/{contestId}", 1L)).andExpect(status().isUnauthorized());
    }

    @DisplayName("로그인한 회원은 공모전을 스크랩할 수 있다.")
    @Test
    void 로그인한_회원은_스크랩할_수_있다() throws Exception {
        // given
        Member user = Member.builder()
                .memberId(3L)
                .email("user@gongmozip.com")
                .role(MemberRole.USER)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        ScrapResponse scrapResponse = new ScrapResponse(1L, true, LocalDateTime.now());

        given(contestService.scrapContest(1L, 3L)).willReturn(scrapResponse);

        // when & then
        mockMvc.perform(post("/api/contests/{contestId}/scraps", 1L).with(user(userDetails)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.contestId").value(1L))
                .andExpect(jsonPath("$.data.isScrapped").value(true));
    }
}
