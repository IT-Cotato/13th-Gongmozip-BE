package org.cotato.gongmozip.domains.mypage.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.enums.MemberRole;
import org.cotato.gongmozip.domains.member.enums.MemberStatus;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.mypage.dto.response.MyPageResponse.CompletedProjectsResponse;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.profile.repository.ProfileRepository;
import org.cotato.gongmozip.domains.review.repository.ReviewRepository;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.LeaderSelectionMode;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.enums.TeamRole;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MyPageServiceIntegrationTest {

    @Autowired
    private MyPageService myPageService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Test
    @DisplayName("완료 프로젝트 기록 삭제 API 호출 후 완료 프로젝트 조회 시 삭제한 프로젝트가 결과 목록에서 배제된다.")
    void deleteCompletedProjectHidesItFromRetrieval() {
        // given
        Member member = Member.builder()
                .email("integration@gongmozip.com")
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.USER)
                .collaborationPoint(100)
                .build();
        memberRepository.save(member);

        Profile profile = Profile.builder()
                .member(member)
                .nickname("integration-nick")
                .schoolName("school")
                .grade(3)
                .major("major")
                .gpa(4.0)
                .gpaScale(4.5)
                .interestCategories(List.of(InterestCategory.IT_AI_TECH))
                .isPublic(true)
                .build();
        profileRepository.save(profile);

        Team team = Team.builder()
                .status(TeamStatus.SUBMITTED)
                .preferredCategory(InterestCategory.IT_AI_TECH)
                .leaderSelectionMode(LeaderSelectionMode.OPEN_NOMINATION)
                .contestDecidedAt(LocalDateTime.now().minusDays(10))
                .completedAt(LocalDateTime.now())
                .build();
        teamRepository.save(team);

        TeamMember teamMember = TeamMember.builder()
                .team(team)
                .member(member)
                .profile(profile)
                .leaderPreference(LeaderPreference.WANTS)
                .extroversionType(ExtroversionType.E)
                .extroversionScore(new BigDecimal("4.0"))
                .isPreLeaderCandidate(false)
                .role(TeamRole.MEMBER)
                .status(TeamMemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now().minusDays(10))
                .build();
        teamMemberRepository.save(teamMember);

        // 삭제 전: 완료 프로젝트 목록에 조회됨
        CompletedProjectsResponse responseBefore = myPageService.getCompletedProjects(member.getMemberId(), 0, 10);
        assertThat(responseBefore.projects()).hasSize(1);
        assertThat(responseBefore.projects().get(0).teamId()).isEqualTo(team.getTeamId());

        // when: 완료 프로젝트 삭제(숨김) 처리
        myPageService.deleteCompletedProject(member.getMemberId(), team.getTeamId());

        // then: 완료 프로젝트 목록에서 배제되어 결과가 빈 목록이어야 함
        CompletedProjectsResponse responseAfter = myPageService.getCompletedProjects(member.getMemberId(), 0, 10);
        assertThat(responseAfter.projects()).isEmpty();
    }

    @Test
    @DisplayName("진행 중 프로젝트 조회 시 진행 중 상태의 팀만 반환하고 완료 상태의 팀은 제외한다.")
    void getOngoingProjectsReturnsOngoingOnly() {
        // given
        Member member = Member.builder()
                .email("ongoing-integration@gongmozip.com")
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.USER)
                .collaborationPoint(100)
                .build();
        memberRepository.save(member);

        Profile profile = Profile.builder()
                .member(member)
                .nickname("ongoing-integration-nick")
                .schoolName("school")
                .grade(3)
                .major("major")
                .gpa(4.0)
                .gpaScale(4.5)
                .interestCategories(List.of(InterestCategory.IT_AI_TECH))
                .isPublic(true)
                .build();
        profileRepository.save(profile);

        // 진행 중인 팀 생성
        Team ongoingTeam = Team.builder()
                .status(TeamStatus.IN_PROGRESS)
                .preferredCategory(InterestCategory.IT_AI_TECH)
                .leaderSelectionMode(LeaderSelectionMode.OPEN_NOMINATION)
                .build();
        teamRepository.save(ongoingTeam);

        TeamMember ongoingMember = TeamMember.builder()
                .team(ongoingTeam)
                .member(member)
                .profile(profile)
                .leaderPreference(LeaderPreference.WANTS)
                .extroversionType(ExtroversionType.E)
                .extroversionScore(new BigDecimal("4.0"))
                .isPreLeaderCandidate(false)
                .role(TeamRole.MEMBER)
                .status(TeamMemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now().minusDays(5))
                .build();
        teamMemberRepository.save(ongoingMember);

        // 완료된 팀 생성
        Team completedTeam = Team.builder()
                .status(TeamStatus.COMPLETED)
                .preferredCategory(InterestCategory.IT_AI_TECH)
                .leaderSelectionMode(LeaderSelectionMode.OPEN_NOMINATION)
                .completedAt(LocalDateTime.now())
                .build();
        teamRepository.save(completedTeam);

        TeamMember completedMember = TeamMember.builder()
                .team(completedTeam)
                .member(member)
                .profile(profile)
                .leaderPreference(LeaderPreference.WANTS)
                .extroversionType(ExtroversionType.E)
                .extroversionScore(new BigDecimal("4.0"))
                .isPreLeaderCandidate(false)
                .role(TeamRole.MEMBER)
                .status(TeamMemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now().minusDays(10))
                .build();
        teamMemberRepository.save(completedMember);

        // when
        org.cotato.gongmozip.domains.mypage.dto.response.MyPageResponse.OngoingProjectsResponse ongoingResponse =
                myPageService.getOngoingProjects(member.getMemberId(), 0, 10);
        org.cotato.gongmozip.domains.mypage.dto.response.MyPageResponse.MyPageMainResponse mainResponse =
                myPageService.getMyPageMain(member.getMemberId());

        // then: 진행 중 프로젝트 목록에 ongoingTeam만 반환되어야 함
        assertThat(ongoingResponse.projects()).hasSize(1);
        assertThat(ongoingResponse.projects().get(0).teamId()).isEqualTo(ongoingTeam.getTeamId());

        // then: 마이페이지 메인의 카운트 정보가 정확해야 함
        assertThat(mainResponse.ongoingProjectCount()).isEqualTo(1);
        assertThat(mainResponse.completedProjectCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("받은 팀원 후기 통계 조회 시 저장된 리뷰 데이터들의 키워드 빈도수가 정렬되어 조회되고 마이페이지 메인의 reviewCount에도 반영된다.")
    void getReviewStatisticsAndVerifyCalculations() {
        // given
        Member reviewer = Member.builder()
                .email("reviewer@gongmozip.com")
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.USER)
                .collaborationPoint(100)
                .build();
        memberRepository.save(reviewer);

        Member reviewee = Member.builder()
                .email("reviewee@gongmozip.com")
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.USER)
                .collaborationPoint(100)
                .build();
        memberRepository.save(reviewee);

        Profile reviewerProfile = Profile.builder()
                .member(reviewer)
                .nickname("reviewer-nick")
                .schoolName("school")
                .grade(3)
                .major("major")
                .gpa(4.0)
                .gpaScale(4.5)
                .interestCategories(List.of(InterestCategory.IT_AI_TECH))
                .isPublic(true)
                .build();
        profileRepository.save(reviewerProfile);

        Profile revieweeProfile = Profile.builder()
                .member(reviewee)
                .nickname("reviewee-nick")
                .schoolName("school")
                .grade(3)
                .major("major")
                .gpa(4.0)
                .gpaScale(4.5)
                .interestCategories(List.of(InterestCategory.IT_AI_TECH))
                .isPublic(true)
                .build();
        profileRepository.save(revieweeProfile);

        Team team = Team.builder()
                .status(TeamStatus.COMPLETED)
                .preferredCategory(InterestCategory.IT_AI_TECH)
                .leaderSelectionMode(LeaderSelectionMode.OPEN_NOMINATION)
                .build();
        teamRepository.save(team);

        TeamMember reviewerTm = TeamMember.builder()
                .team(team)
                .member(reviewer)
                .profile(reviewerProfile)
                .leaderPreference(LeaderPreference.WANTS)
                .extroversionType(ExtroversionType.E)
                .extroversionScore(new BigDecimal("4.0"))
                .isPreLeaderCandidate(false)
                .role(TeamRole.MEMBER)
                .status(TeamMemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now().minusDays(10))
                .build();
        teamMemberRepository.save(reviewerTm);

        TeamMember revieweeTm = TeamMember.builder()
                .team(team)
                .member(reviewee)
                .profile(revieweeProfile)
                .leaderPreference(LeaderPreference.WANTS)
                .extroversionType(ExtroversionType.E)
                .extroversionScore(new BigDecimal("4.0"))
                .isPreLeaderCandidate(false)
                .role(TeamRole.MEMBER)
                .status(TeamMemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now().minusDays(10))
                .build();
        teamMemberRepository.save(revieweeTm);

        // 두 번째 후기 작성자 추가 (동일한 team_id + reviewee_team_member_id 에 대해 중복 리뷰 방지 제약조건 회피)
        Member reviewer2 = Member.builder()
                .email("reviewer2@gongmozip.com")
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.USER)
                .collaborationPoint(100)
                .build();
        memberRepository.save(reviewer2);

        Profile reviewer2Profile = Profile.builder()
                .member(reviewer2)
                .nickname("reviewer2-nick")
                .schoolName("school")
                .grade(3)
                .major("major")
                .gpa(4.0)
                .gpaScale(4.5)
                .interestCategories(List.of(InterestCategory.IT_AI_TECH))
                .isPublic(true)
                .build();
        profileRepository.save(reviewer2Profile);

        TeamMember reviewer2Tm = TeamMember.builder()
                .team(team)
                .member(reviewer2)
                .profile(reviewer2Profile)
                .leaderPreference(LeaderPreference.WANTS)
                .extroversionType(ExtroversionType.E)
                .extroversionScore(new BigDecimal("4.0"))
                .isPreLeaderCandidate(false)
                .role(TeamRole.MEMBER)
                .status(TeamMemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now().minusDays(10))
                .build();
        teamMemberRepository.save(reviewer2Tm);

        // 후기 저장
        org.cotato.gongmozip.domains.review.entity.Review review1 =
                org.cotato.gongmozip.domains.review.entity.Review.builder()
                        .team(team)
                        .reviewer(reviewerTm)
                        .reviewee(revieweeTm)
                        .communicationScore(org.cotato.gongmozip.domains.review.enums.ReviewAgreementLevel.AGREE)
                        .participationScore(org.cotato.gongmozip.domains.review.enums.ReviewAgreementLevel.AGREE)
                        .keywords(List.of("GOOD_COMMUNICATOR", "TRUSTWORTHY"))
                        .build();
        reviewRepository.save(review1);

        org.cotato.gongmozip.domains.review.entity.Review review2 =
                org.cotato.gongmozip.domains.review.entity.Review.builder()
                        .team(team)
                        .reviewer(reviewer2Tm)
                        .reviewee(revieweeTm)
                        .communicationScore(org.cotato.gongmozip.domains.review.enums.ReviewAgreementLevel.AGREE)
                        .participationScore(org.cotato.gongmozip.domains.review.enums.ReviewAgreementLevel.AGREE)
                        .keywords(List.of("TRUSTWORTHY", "CREATIVE"))
                        .build();
        reviewRepository.save(review2);

        // when
        org.cotato.gongmozip.domains.mypage.dto.response.MyPageResponse.ReviewStatisticsResponse statisticsResponse =
                myPageService.getReviewStatistics(reviewee.getMemberId());
        org.cotato.gongmozip.domains.mypage.dto.response.MyPageResponse.MyPageMainResponse mainResponse =
                myPageService.getMyPageMain(reviewee.getMemberId());

        // then
        assertThat(statisticsResponse.totalReviewCount()).isEqualTo(2);
        assertThat(statisticsResponse.keywords()).hasSize(3);
        // TRUSTWORTHY = 2
        assertThat(statisticsResponse.keywords().get(0).keyword()).isEqualTo("TRUSTWORTHY");
        assertThat(statisticsResponse.keywords().get(0).count()).isEqualTo(2);
        // CREATIVE = 1, GOOD_COMMUNICATOR = 1 (sorted alphabetically)
        assertThat(statisticsResponse.keywords().get(1).keyword()).isEqualTo("CREATIVE");
        assertThat(statisticsResponse.keywords().get(1).count()).isEqualTo(1);
        assertThat(statisticsResponse.keywords().get(2).keyword()).isEqualTo("GOOD_COMMUNICATOR");
        assertThat(statisticsResponse.keywords().get(2).count()).isEqualTo(1);

        // 마이페이지 메인의 reviewCount 검증
        assertThat(mainResponse.reviewCount()).isEqualTo(2);
    }
}
