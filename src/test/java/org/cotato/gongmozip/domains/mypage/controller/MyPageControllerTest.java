package org.cotato.gongmozip.domains.mypage.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.enums.MemberRole;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.mypage.dto.response.MyPageResponse.*;
import org.cotato.gongmozip.domains.mypage.service.MyPageService;
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
class MyPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MyPageService myPageService;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    @DisplayName("로그인한 사용자는 마이페이지 메인 정보를 조회할 수 있다.")
    void getMyPageMain_authenticated() throws Exception {
        // given
        Member userMember = Member.builder()
                .memberId(1L)
                .email("user@gongmozip.com")
                .role(MemberRole.USER)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(userMember);

        MainProfileSummary mainProfile = new MainProfileSummary(10L, "채영", "숙명여자대학교", "컴퓨터과학전공", 3);
        CollaborationDistanceSummary distance = new CollaborationDistanceSummary(100, 500, 20);
        MyPageMainResponse response = new MyPageMainResponse(null, distance, mainProfile, 0, 0, 0, 3);

        given(myPageService.getMyPageMain(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/mypage").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mainProfile.nickname").value("채영"))
                .andExpect(jsonPath("$.data.collaborationDistance.current").value(100))
                .andExpect(jsonPath("$.data.scrapContestCount").value(3));
    }

    @Test
    @DisplayName("비로그인 사용자가 마이페이지 메인 조회 시 401 Unauthorized 에러가 발생한다.")
    void getMyPageMain_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/mypage")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그인한 사용자는 진행 중 프로젝트 목록을 조회할 수 있다.")
    void getOngoingProjects_authenticated() throws Exception {
        // given
        Member userMember = Member.builder()
                .memberId(1L)
                .email("user@gongmozip.com")
                .role(MemberRole.USER)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(userMember);

        OngoingProjectsResponse response = new OngoingProjectsResponse(List.of(), 0, 10, 0L, 0);
        given(myPageService.getOngoingProjects(1L, 0, 10)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/mypage/projects/ongoing")
                        .with(user(userDetails))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projects").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("로그인한 사용자는 완료 프로젝트 목록을 조회할 수 있다.")
    void getCompletedProjects_authenticated() throws Exception {
        // given
        Member userMember = Member.builder()
                .memberId(1L)
                .email("user@gongmozip.com")
                .role(MemberRole.USER)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(userMember);

        CompletedProjectsResponse response = new CompletedProjectsResponse(List.of(), 0, 10, 0L, 0);
        given(myPageService.getCompletedProjects(1L, 0, 10)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/mypage/projects/completed")
                        .with(user(userDetails))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projects").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("로그인한 사용자는 받은 팀원 후기 통계를 조회할 수 있다.")
    void getReviewStatistics_authenticated() throws Exception {
        // given
        Member userMember = Member.builder()
                .memberId(1L)
                .email("user@gongmozip.com")
                .role(MemberRole.USER)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(userMember);

        ReviewStatisticsResponse response = new ReviewStatisticsResponse(0, List.of());
        given(myPageService.getReviewStatistics(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/mypage/reviews").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalReviewCount").value(0))
                .andExpect(jsonPath("$.data.keywords").isEmpty());
    }

    @Test
    @DisplayName("로그인한 사용자는 내 스크랩 공모전 목록을 조회할 수 있다.")
    void getScrappedContests_authenticated() throws Exception {
        // given
        Member userMember = Member.builder()
                .memberId(1L)
                .email("user@gongmozip.com")
                .role(MemberRole.USER)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(userMember);

        ScrappedContestItem contestItem = new ScrappedContestItem(15L, "2026 AI 해커톤", "IT_AI_TECH", "2026-08-10", true);
        ScrappedContestsResponse response = new ScrappedContestsResponse(List.of(contestItem), 0, 10, 1L, 1);

        given(myPageService.getScrappedContests(1L, 0, 10)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/mypage/scrapped-contests")
                        .with(user(userDetails))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contests[0].contestId").value(15L))
                .andExpect(jsonPath("$.data.contests[0].title").value("2026 AI 해커톤"))
                .andExpect(jsonPath("$.data.contests[0].isScrapped").value(true));
    }
}
