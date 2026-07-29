package org.cotato.gongmozip.domains.mypage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.character.service.CharacterService;
import org.cotato.gongmozip.domains.contest.entity.Contest;
import org.cotato.gongmozip.domains.contest.entity.ContestScrap;
import org.cotato.gongmozip.domains.contest.enums.ContestStatus;
import org.cotato.gongmozip.domains.contest.repository.ContestScrapRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.enums.MemberRole;
import org.cotato.gongmozip.domains.member.enums.MemberStatus;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.mypage.dto.response.MyPageResponse.*;
import org.cotato.gongmozip.domains.mypage.exception.MyPageException;
import org.cotato.gongmozip.domains.mypage.exception.codes.MyPageErrorCode;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class MyPageServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ContestScrapRepository contestScrapRepository;

    @Mock
    private CharacterService characterService;

    @InjectMocks
    private MyPageService myPageService;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .memberId(1L)
                .email("test@gongmozip.com")
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.USER)
                .build();
    }

    @Test
    @DisplayName("마이페이지 메인 조회 - 정보 조회가 성공한다.")
    void getMyPageMain_success() {
        // given
        given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
        given(contestScrapRepository.countByMember(testMember)).willReturn(3);

        // when
        MyPageMainResponse response = myPageService.getMyPageMain(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.collaborationDistance().current()).isEqualTo(100);
        assertThat(response.collaborationDistance().max()).isEqualTo(500);
        assertThat(response.collaborationDistance().progress()).isEqualTo(20);
        assertThat(response.scrapContestCount()).isEqualTo(3);
        assertThat(response.ongoingProjectCount()).isEqualTo(0);
        assertThat(response.completedProjectCount()).isEqualTo(0);
        assertThat(response.reviewCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("마이페이지 메인 조회 - 존재하지 않는 회원 시 예외가 발생한다.")
    void getMyPageMain_memberNotFound() {
        // given
        given(memberRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> myPageService.getMyPageMain(999L))
                .isInstanceOf(MyPageException.class)
                .hasMessage(MyPageErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("진행 중 프로젝트 조회 - 빈 프로젝트 목록이 성공적으로 반환된다.")
    void getOngoingProjects_success() {
        // given
        given(memberRepository.existsById(1L)).willReturn(true);

        // when
        OngoingProjectsResponse response = myPageService.getOngoingProjects(1L, 0, 10);

        // then
        assertThat(response).isNotNull();
        assertThat(response.projects()).isEmpty();
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(0L);
        assertThat(response.totalPages()).isEqualTo(0);
    }

    @Test
    @DisplayName("진행 중 프로젝트 조회 - 잘못된 페이지 번호 전달 시 예외가 발생한다.")
    void getOngoingProjects_invalidPaging() {
        // given
        given(memberRepository.existsById(1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> myPageService.getOngoingProjects(1L, -1, 10))
                .isInstanceOf(MyPageException.class)
                .hasMessage(MyPageErrorCode.INVALID_PAGE_INFO.getMessage());
    }

    @Test
    @DisplayName("완료 프로젝트 조회 - 빈 완료 프로젝트 목록이 성공적으로 반환된다.")
    void getCompletedProjects_success() {
        // given
        given(memberRepository.existsById(1L)).willReturn(true);

        // when
        CompletedProjectsResponse response = myPageService.getCompletedProjects(1L, 0, 10);

        // then
        assertThat(response).isNotNull();
        assertThat(response.projects()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0L);
    }

    @Test
    @DisplayName("받은 팀원 후기 조회 - 빈 후기 통계가 성공적으로 반환된다.")
    void getReviewStatistics_success() {
        // given
        given(memberRepository.existsById(1L)).willReturn(true);

        // when
        ReviewStatisticsResponse response = myPageService.getReviewStatistics(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.totalReviewCount()).isEqualTo(0);
        assertThat(response.keywords()).isEmpty();
    }

    @Test
    @DisplayName("내 스크랩 공모전 조회 - 스크랩 목록 조회가 성공한다.")
    void getScrappedContests_success() {
        // given
        given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));

        Contest contest = Contest.builder()
                .contestId(100L)
                .title("2026 AI 해커톤")
                .description("AI 해커톤 설명")
                .category(InterestCategory.IT_AI_TECH)
                .status(ContestStatus.OPEN)
                .hostName("구글")
                .applyEndAt(LocalDateTime.of(2026, 8, 10, 23, 59))
                .isTeamParticipation(true)
                .build();

        ContestScrap scrap = ContestScrap.builder()
                .contestScrapId(50L)
                .member(testMember)
                .contest(contest)
                .build();

        Page<ContestScrap> scrapPage = new PageImpl<>(List.of(scrap), PageRequest.of(0, 10), 1);
        given(contestScrapRepository.findAllByMemberWithContest(eq(testMember), any(Pageable.class)))
                .willReturn(scrapPage);

        // when
        ScrappedContestsResponse response = myPageService.getScrappedContests(1L, 0, 10);

        // then
        assertThat(response).isNotNull();
        assertThat(response.contests()).hasSize(1);
        assertThat(response.contests().get(0).contestId()).isEqualTo(100L);
        assertThat(response.contests().get(0).title()).isEqualTo("2026 AI 해커톤");
        assertThat(response.contests().get(0).category()).isEqualTo("IT_AI_TECH");
        assertThat(response.contests().get(0).deadline()).isEqualTo("2026-08-10");
        assertThat(response.contests().get(0).isScrapped()).isTrue();
        assertThat(response.totalElements()).isEqualTo(1L);
    }
}
