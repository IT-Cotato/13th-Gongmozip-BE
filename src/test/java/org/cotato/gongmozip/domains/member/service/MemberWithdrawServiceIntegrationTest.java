package org.cotato.gongmozip.domains.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.auth.entity.AuthAccount;
import org.cotato.gongmozip.domains.auth.enums.AuthProvider;
import org.cotato.gongmozip.domains.auth.repository.AuthAccountRepository;
import org.cotato.gongmozip.domains.auth.service.AuthService;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
import org.cotato.gongmozip.domains.matching.repository.MatchingApplicationRepository;
import org.cotato.gongmozip.domains.member.dto.request.MemberAuthRequest.EmailVerifyRequest;
import org.cotato.gongmozip.domains.member.dto.request.MemberRequest.WithdrawMemberRequest;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.enums.MemberRole;
import org.cotato.gongmozip.domains.member.enums.MemberStatus;
import org.cotato.gongmozip.domains.member.enums.WithdrawalReasonType;
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.member.repository.MemberWithdrawalReasonRepository;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.profile.repository.ProfileRepository;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MemberWithdrawServiceIntegrationTest {

    private static final String PASSWORD = "password123!";

    @Autowired
    private MemberWithdrawService memberWithdrawService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberWithdrawalReasonRepository memberWithdrawalReasonRepository;

    @Autowired
    private AuthAccountRepository authAccountRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private MatchingApplicationRepository matchingApplicationRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @Test
    @DisplayName("이메일 가입 회원은 올바른 비밀번호로 탈퇴하면 개인 데이터가 삭제되고 탈퇴 상태가 된다")
    void withdrawEmailMemberSuccess() {
        Member member = saveEmailMember("withdraw@gongmozip.com");
        Profile profile = saveProfile(member);

        memberWithdrawService.withdraw(
                member.getMemberId(),
                "access-token",
                new WithdrawMemberRequest(PASSWORD, WithdrawalReasonType.MATCHING_DISSATISFIED, null));

        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(member.getWithdrawnAt()).isNotNull();
        assertThat(authAccountRepository.existsByMemberAndProvider(member, AuthProvider.EMAIL))
                .isFalse();
        assertThat(profileRepository
                        .findById(profile.getProfileId())
                        .orElseThrow()
                        .isPublic())
                .isFalse();
        assertThat(memberWithdrawalReasonRepository.findAll())
                .hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("reason", WithdrawalReasonType.MATCHING_DISSATISFIED);
    }

    @Test
    @DisplayName("이메일 가입 회원이 비밀번호 없이 탈퇴를 요청하면 실패한다")
    void withdrawFailsWithoutPassword() {
        Member member = saveEmailMember("nopassword@gongmozip.com");

        assertThatThrownBy(() -> memberWithdrawService.withdraw(
                        member.getMemberId(),
                        "access-token",
                        new WithdrawMemberRequest(null, WithdrawalReasonType.NO_LONGER_NEEDED, null)))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.WITHDRAW_PASSWORD_REQUIRED);
    }

    @Test
    @DisplayName("이메일 가입 회원이 틀린 비밀번호로 탈퇴를 요청하면 실패한다")
    void withdrawFailsWithWrongPassword() {
        Member member = saveEmailMember("wrongpassword@gongmozip.com");

        assertThatThrownBy(() -> memberWithdrawService.withdraw(
                        member.getMemberId(),
                        "access-token",
                        new WithdrawMemberRequest("wrong-password", WithdrawalReasonType.NO_LONGER_NEEDED, null)))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.WITHDRAW_INVALID_PASSWORD);
    }

    @Test
    @DisplayName("소셜 전용 회원은 비밀번호 없이 탈퇴할 수 있다")
    void withdrawSocialOnlyMemberWithoutPassword() {
        Member member = saveSocialMember("social@gongmozip.com");

        memberWithdrawService.withdraw(
                member.getMemberId(),
                "access-token",
                new WithdrawMemberRequest(null, WithdrawalReasonType.NEW_ACCOUNT, null));

        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(authAccountRepository.existsByMemberAndProvider(member, AuthProvider.GOOGLE))
                .isFalse();
    }

    @Test
    @DisplayName("기타 사유로 탈퇴하면 상세 사유가 함께 저장된다")
    void withdrawSavesEtcReasonDetail() {
        Member member = saveEmailMember("etc@gongmozip.com");

        memberWithdrawService.withdraw(
                member.getMemberId(),
                "access-token",
                new WithdrawMemberRequest(PASSWORD, WithdrawalReasonType.ETC, "서비스가 저와 맞지 않아요"));

        assertThat(memberWithdrawalReasonRepository.findAll())
                .hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("reason", WithdrawalReasonType.ETC)
                .hasFieldOrPropertyWithValue("reasonDetail", "서비스가 저와 맞지 않아요");
    }

    @Test
    @DisplayName("진행 중인 팀 프로젝트가 있으면 탈퇴할 수 없다")
    void withdrawBlockedByOngoingTeam() {
        Member member = saveEmailMember("team@gongmozip.com");
        Profile profile = saveProfile(member);
        saveTeamMember(member, profile, TeamStatus.IN_PROGRESS);

        assertThatThrownBy(() -> memberWithdrawService.withdraw(
                        member.getMemberId(),
                        "access-token",
                        new WithdrawMemberRequest(PASSWORD, WithdrawalReasonType.BAD_MANNER_USER, null)))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.WITHDRAWAL_BLOCKED_BY_ACTIVE_TEAM);
    }

    @Test
    @DisplayName("완료된 팀 프로젝트만 있으면 탈퇴할 수 있고 팀 이력은 유지된다")
    void withdrawAllowedWithCompletedTeam() {
        Member member = saveEmailMember("completedteam@gongmozip.com");
        Profile profile = saveProfile(member);
        TeamMember teamMember = saveTeamMember(member, profile, TeamStatus.COMPLETED);

        memberWithdrawService.withdraw(
                member.getMemberId(),
                "access-token",
                new WithdrawMemberRequest(PASSWORD, WithdrawalReasonType.NO_LONGER_NEEDED, null));

        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(teamMemberRepository.findById(teamMember.getTeamMemberId())).isPresent();
    }

    @Test
    @DisplayName("대기 중인 매칭 신청은 탈퇴 시 자동 취소된다")
    void withdrawCancelsWaitingApplication() {
        Member member = saveEmailMember("waiting@gongmozip.com");
        Profile profile = saveProfile(member);
        MatchingApplication application = saveApplication(member, profile, MatchingApplicationStatus.WAITING);

        memberWithdrawService.withdraw(
                member.getMemberId(),
                "access-token",
                new WithdrawMemberRequest(PASSWORD, WithdrawalReasonType.NO_LONGER_NEEDED, null));

        assertThat(application.getStatus()).isEqualTo(MatchingApplicationStatus.CANCELED);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
    }

    @Test
    @DisplayName("매칭 결과 제안이 진행 중이면 탈퇴할 수 없다")
    void withdrawBlockedByProposedApplication() {
        Member member = saveEmailMember("proposed@gongmozip.com");
        Profile profile = saveProfile(member);
        saveApplication(member, profile, MatchingApplicationStatus.PROPOSED);

        assertThatThrownBy(() -> memberWithdrawService.withdraw(
                        member.getMemberId(),
                        "access-token",
                        new WithdrawMemberRequest(PASSWORD, WithdrawalReasonType.NO_LONGER_NEEDED, null)))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.WITHDRAWAL_BLOCKED_BY_MATCHING);
    }

    @Test
    @DisplayName("탈퇴한 회원은 로그인할 수 없다")
    void withdrawnMemberCannotLogin() {
        Member member = saveEmailMember("login@gongmozip.com");
        memberWithdrawService.withdraw(
                member.getMemberId(),
                "access-token",
                new WithdrawMemberRequest(PASSWORD, WithdrawalReasonType.NO_LONGER_NEEDED, null));

        assertThatThrownBy(() ->
                        authService.login(new org.cotato.gongmozip.domains.auth.dto.request.AuthRequest.LoginRequest(
                                "login@gongmozip.com", PASSWORD)))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.WITHDRAWN_MEMBER);
    }

    @Test
    @DisplayName("탈퇴 후 재가입 제한 기간에는 같은 이메일로 인증코드를 요청할 수 없다")
    void rejoinRestrictedDuringRestrictionPeriod() {
        Member member = saveEmailMember("rejoin@gongmozip.com");
        memberWithdrawService.withdraw(
                member.getMemberId(),
                "access-token",
                new WithdrawMemberRequest(PASSWORD, WithdrawalReasonType.NEW_ACCOUNT, null));

        assertThatThrownBy(() -> memberService.sendVerificationCode(new EmailVerifyRequest("rejoin@gongmozip.com")))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.REJOIN_RESTRICTED);
    }

    @Test
    @DisplayName("재가입 제한 기간이 지난 탈퇴 회원만 익명화 배치 대상이 된다")
    void anonymizeOnlyExpiredWithdrawnMembers() {
        LocalDateTime now = LocalDateTime.now();

        Member expiredMember = saveEmailMember("expired@gongmozip.com");
        Profile expiredProfile = saveProfile(expiredMember);
        expiredMember.withdraw(now.minusDays(15));

        Member recentMember = saveEmailMember("recent@gongmozip.com");
        recentMember.withdraw(now.minusDays(13));

        int anonymizedCount = memberWithdrawService.anonymizeExpiredWithdrawnMembers(now);

        assertThat(anonymizedCount).isEqualTo(1);
        assertThat(expiredMember.isAnonymized()).isTrue();
        assertThat(expiredMember.getEmail()).doesNotContain("expired@gongmozip.com");
        assertThat(expiredMember.getName()).isNull();
        assertThat(profileRepository
                        .findById(expiredProfile.getProfileId())
                        .orElseThrow()
                        .getNickname())
                .isEqualTo(MemberWithdrawService.ANONYMIZED_NICKNAME);

        assertThat(recentMember.isAnonymized()).isFalse();
        assertThat(recentMember.getEmail()).isEqualTo("recent@gongmozip.com");

        // 익명화 이후에는 원래 이메일로 재가입할 수 있다
        assertThat(memberRepository.existsByEmail("expired@gongmozip.com")).isFalse();
    }

    private Member saveEmailMember(String email) {
        Member member = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.USER)
                .name("탈퇴테스트")
                .build();
        memberRepository.save(member);
        authAccountRepository.save(AuthAccount.builder()
                .member(member)
                .provider(AuthProvider.EMAIL)
                .build());
        return member;
    }

    private Member saveSocialMember(String email) {
        Member member = Member.builder()
                .email(email)
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.USER)
                .build();
        memberRepository.save(member);
        authAccountRepository.save(AuthAccount.builder()
                .member(member)
                .provider(AuthProvider.GOOGLE)
                .providerMemberId("google-" + email)
                .build());
        return member;
    }

    private Profile saveProfile(Member member) {
        Profile profile = Profile.builder()
                .member(member)
                .nickname("nick-" + member.getEmail().hashCode())
                .schoolName("school")
                .grade(3)
                .major("major")
                .gpa(4.0)
                .gpaScale(4.5)
                .interestCategories(List.of(InterestCategory.IT_AI_TECH))
                .isPublic(true)
                .build();
        return profileRepository.save(profile);
    }

    private TeamMember saveTeamMember(Member member, Profile profile, TeamStatus teamStatus) {
        Team team = Team.builder()
                .status(teamStatus)
                .preferredCategory(InterestCategory.IT_AI_TECH)
                .leaderSelectionMode(LeaderSelectionMode.OPEN_NOMINATION)
                .completedAt(teamStatus == TeamStatus.COMPLETED ? LocalDateTime.now() : null)
                .build();
        teamRepository.save(team);

        TeamMember teamMember = TeamMember.builder()
                .team(team)
                .member(member)
                .profile(profile)
                .leaderPreference(LeaderPreference.NEUTRAL)
                .extroversionType(ExtroversionType.E)
                .extroversionScore(new BigDecimal("4.0"))
                .isPreLeaderCandidate(false)
                .role(TeamRole.MEMBER)
                .status(TeamMemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build();
        return teamMemberRepository.save(teamMember);
    }

    private MatchingApplication saveApplication(Member member, Profile profile, MatchingApplicationStatus status) {
        BigDecimal score = new BigDecimal("50.00");
        MatchingApplication application = MatchingApplication.builder()
                .member(member)
                .profile(profile)
                .applicationDate(LocalDate.now())
                .status(status)
                .leaderPreference(LeaderPreference.NEUTRAL)
                .firstMatching(false)
                .contestCategory(InterestCategory.IT_AI_TECH)
                .gpaScore(score)
                .projectScore(score)
                .awardScore(score)
                .certificationScore(score)
                .collaborationScore(score)
                .skillScore(score)
                .collaborationDistance(100)
                .agreeablenessScore(score)
                .conscientiousnessScore(score)
                .honestyHumilityScore(score)
                .extroversionScore(score)
                .goalPreferenceScore(score)
                .workStyleScore(score)
                .communicationStyleScore(score)
                .extroversion2Score(score)
                .extroversion3Score(score)
                .extroversionType(ExtroversionType.A)
                .characterType(CharacterType.TRACK_RUNNER)
                .characterXScore(score)
                .characterYScore(score)
                .build();
        return matchingApplicationRepository.save(application);
    }
}
