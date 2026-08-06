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

    @Mock
    private org.cotato.gongmozip.domains.team.repository.TeamMemberRepository teamMemberRepository;

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
                .collaborationPoint(100)
                .build();
    }

    @Test
    @DisplayName("마이페이지 메인 조회 - 정보 조회가 성공한다.")
    void getMyPageMain_success() {
        // given
        given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
        given(contestScrapRepository.countByMember(testMember)).willReturn(3);
        given(teamMemberRepository.findCompletedProjects(any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of()));

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
        given(teamMemberRepository.findCompletedProjects(any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of()));

        // when
        CompletedProjectsResponse response = myPageService.getCompletedProjects(1L, 0, 10);

        // then
        assertThat(response).isNotNull();
        assertThat(response.projects()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0L);
    }

    @Test
    @DisplayName("완료 프로젝트 조회 - 비어있지 않은 완료 프로젝트 목록 조회 시 DTO에 teamId 및 메달 정보가 정상 반영된다.")
    void getCompletedProjects_nonEmpty_success() {
        // given
        given(memberRepository.existsById(1L)).willReturn(true);

        org.cotato.gongmozip.domains.team.entity.Team team = org.cotato.gongmozip.domains.team.entity.Team.builder()
                .teamId(10L)
                .status(org.cotato.gongmozip.domains.team.enums.TeamStatus.SUBMITTED)
                .contestDecidedAt(LocalDateTime.of(2026, 8, 1, 12, 0))
                .completedAt(LocalDateTime.of(2026, 8, 5, 12, 0))
                .build();
        org.cotato.gongmozip.domains.team.entity.TeamMember teamMember =
                org.cotato.gongmozip.domains.team.entity.TeamMember.builder()
                        .team(team)
                        .build();

        given(teamMemberRepository.findCompletedProjects(
                        eq(1L), eq(org.cotato.gongmozip.domains.team.enums.TeamMemberStatus.ACTIVE), any(), any()))
                .willReturn(new PageImpl<>(List.of(teamMember)));

        // when
        CompletedProjectsResponse response = myPageService.getCompletedProjects(1L, 0, 10);

        // then
        assertThat(response).isNotNull();
        assertThat(response.projects()).hasSize(1);
        assertThat(response.projects().get(0).teamId()).isEqualTo(10L);
        assertThat(response.projects().get(0).medal()).isEqualTo("스프린트 완주 메달");
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

    @Test
    @DisplayName("완료 프로젝트 삭제 - 완료된 프로젝트에 대해 정상적으로 소프트 딜리트 플래그를 변경한다.")
    void deleteCompletedProject_success() {
        // given
        given(memberRepository.existsById(1L)).willReturn(true);
        org.cotato.gongmozip.domains.team.entity.Team team = org.cotato.gongmozip.domains.team.entity.Team.builder()
                .status(org.cotato.gongmozip.domains.team.enums.TeamStatus.SUBMITTED)
                .build();
        org.cotato.gongmozip.domains.team.entity.TeamMember teamMember =
                org.cotato.gongmozip.domains.team.entity.TeamMember.builder()
                        .team(team)
                        .isCompletedProjectDeleted(false)
                        .build();

        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(10L, 1L)).willReturn(Optional.of(teamMember));

        // when
        myPageService.deleteCompletedProject(1L, 10L);

        // then
        assertThat(teamMember.isCompletedProjectDeleted()).isTrue();
    }

    @Test
    @DisplayName("완료 프로젝트 삭제 - 진행 중인 프로젝트를 삭제하려 할 경우 예외가 발생한다.")
    void deleteCompletedProject_fail_notCompleted() {
        // given
        given(memberRepository.existsById(1L)).willReturn(true);
        org.cotato.gongmozip.domains.team.entity.Team team = org.cotato.gongmozip.domains.team.entity.Team.builder()
                .status(org.cotato.gongmozip.domains.team.enums.TeamStatus.IN_PROGRESS)
                .build();
        org.cotato.gongmozip.domains.team.entity.TeamMember teamMember =
                org.cotato.gongmozip.domains.team.entity.TeamMember.builder()
                        .team(team)
                        .isCompletedProjectDeleted(false)
                        .build();

        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(10L, 1L)).willReturn(Optional.of(teamMember));

        // when & then
        assertThatThrownBy(() -> myPageService.deleteCompletedProject(1L, 10L))
                .isInstanceOf(MyPageException.class)
                .hasMessage(MyPageErrorCode.PROJECT_NOT_COMPLETED.getMessage());
    }
}
