package org.cotato.gongmozip.domains.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.character.service.CharacterService;
import org.cotato.gongmozip.domains.matching.repository.MatchingApplicationRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.profile.dto.request.ProfileRequest.*;
import org.cotato.gongmozip.domains.profile.dto.response.ProfileResponse.*;
import org.cotato.gongmozip.domains.profile.entity.*;
import org.cotato.gongmozip.domains.profile.enums.AiSummaryStatus;
import org.cotato.gongmozip.domains.profile.enums.CertificationCategory;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.profile.enums.ProjectCategory;
import org.cotato.gongmozip.domains.profile.exception.ProfileException;
import org.cotato.gongmozip.domains.profile.exception.codes.ProfileErrorCode;
import org.cotato.gongmozip.domains.profile.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private CharacterService characterService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ProjectExperienceRepository projectExperienceRepository;

    @Mock
    private AwardRepository awardRepository;

    @Mock
    private CertificationRepository certificationRepository;

    @Mock
    private ProfileCertificationRepository profileCertificationRepository;

    @Mock
    private MatchingApplicationRepository matchingApplicationRepository;

    @Mock
    private ProjectAiSummaryService projectAiSummaryService;

    @Mock
    private ProjectEvaluationRepository projectEvaluationRepository;

    @Mock
    private ProjectEvaluationService projectEvaluationService;

    @Mock
    private ProjectEvaluationTxService projectEvaluationTxService;

    @InjectMocks
    private ProfileService profileService;

    private Member member;
    private Member otherMember;

    @BeforeEach
    void setUp() {
        member = Member.builder().memberId(1L).email("test@gongmozip.com").build();

        otherMember = Member.builder().memberId(2L).email("other@gongmozip.com").build();

        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clear();
        }
    }

    // ==========================================
    // 프로필 생성 테스트
    // ==========================================

    @DisplayName("첫 프로필 생성 시 성공한다.")
    @Test
    void 첫_프로필_생성_시_성공한다() {
        // given
        CreateProfileRequest request = new CreateProfileRequest(
                "러너", "학교", 3, "소프트웨어", "경영", 4.0, 4.5, List.of(InterestCategory.IT_AI_TECH), true);
        given(memberRepository.findByIdWithLock(1L)).willReturn(Optional.of(member));
        given(profileRepository.existsByNickname("러너")).willReturn(false);

        // when
        CreateProfileResponse response = profileService.createProfile(request, member);

        // then
        then(profileRepository).should().save(any(Profile.class));
    }

    // 권한 및 접근 제어 테스트

    @DisplayName("다른 회원의 프로필 수정 시 권한 에러(PROFILE_ACCESS_DENIED)가 발생한다.")
    @Test
    void 다른_회원의_프로필_수정_시_권한_에러가_발생한다() {
        // given
        Profile profile = Profile.builder()
                .profileId(10L)
                .member(otherMember)
                .nickname("타인")
                .build();
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));

        UpdateProfileRequest request = new UpdateProfileRequest("수정닉네임", null, null, null, null, null, null, null);

        // when & then
        assertThatThrownBy(() -> profileService.updateProfile(10L, request, member))
                .isInstanceOf(ProfileException.class)
                .hasMessage(ProfileErrorCode.PROFILE_ACCESS_DENIED.getMessage());
    }

    @DisplayName("공개 프로필(isPublic = true)은 로그인 여부와 관계없이 조회할 수 있다.")
    @Test
    void 공개_프로필은_정상_조회된다() {
        // given
        Member testMember = Member.builder()
                .memberId(1L)
                .email("test@gongmozip.com")
                .gender(org.cotato.gongmozip.domains.member.enums.Gender.MALE)
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();
        Profile profile = Profile.builder()
                .profileId(10L)
                .member(testMember)
                .nickname("공개")
                .schoolName("학교")
                .grade(3)
                .major("컴공")
                .gpa(4.0)
                .gpaScale(4.5)
                .isPublic(true)
                .build();
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));

        // when
        PublicProfileResponse response = profileService.getPublicProfile(10L);

        // then
        assertThat(response.nickname()).isEqualTo("공개");
        assertThat(response.gender()).isEqualTo(org.cotato.gongmozip.domains.member.enums.Gender.MALE);
        assertThat(response.birthYear()).isEqualTo(2000);
        assertThat(response.age())
                .isEqualTo(java.time.Period.between(LocalDate.of(2000, 1, 1), LocalDate.now())
                        .getYears());
        assertThat(response.gpa()).isEqualTo(4.0);
        assertThat(response.gpaScale()).isEqualTo(4.5);
    }

    @DisplayName("비공개 프로필(isPublic = false)을 조회하면 닉네임만 채워지고 나머지는 비운 채로 응답한다.")
    @Test
    void 비공개_프로필은_닉네임만_채워진_채로_응답한다() {
        // given
        Profile profile = Profile.builder()
                .profileId(10L)
                .member(member)
                .nickname("비공개")
                .schoolName("학교")
                .grade(3)
                .major("컴공")
                .isPublic(false)
                .build();
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));

        // when
        PublicProfileResponse response = profileService.getPublicProfile(10L);

        // then
        assertThat(response.isPublic()).isFalse();
        assertThat(response.nickname()).isEqualTo("비공개");
        assertThat(response.schoolName()).isNull();
        assertThat(response.schoolRegion()).isNull();
        assertThat(response.grade()).isNull();
        assertThat(response.major()).isNull();
        assertThat(response.projects()).isEmpty();
        assertThat(response.awards()).isEmpty();
        assertThat(response.certifications()).isEmpty();
        assertThat(response.gender()).isNull();
        assertThat(response.age()).isNull();
        assertThat(response.birthYear()).isNull();
        assertThat(response.gpa()).isNull();
        assertThat(response.gpaScale()).isNull();
    }

    // 프로필 수정 및 대표 프로필 삭제 테스트

    @DisplayName("프로필 수정 시 올바른 필드를 입력하면 프로필 정보가 정상 반영된다.")
    @Test
    void 프로필_수정_시_정상_반영된다() {
        // given
        Profile profile = Profile.builder()
                .profileId(10L)
                .member(member)
                .nickname("이전")
                .schoolName("이전학교")
                .grade(2)
                .major("이전전공")
                .gpa(3.5)
                .gpaScale(4.5)
                .build();
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));

        UpdateProfileRequest request = new UpdateProfileRequest(
                "수정닉네임", "수정학교", 3, "수정전공", "복전", 4.0, 4.5, List.of(InterestCategory.IT_AI_TECH));

        // when
        UpdateProfileResponse response = profileService.updateProfile(10L, request, member);

        // then
        assertThat(profile.getNickname()).isEqualTo("수정닉네임");
        assertThat(profile.getSchoolName()).isEqualTo("수정학교");
        assertThat(profile.getGrade()).isEqualTo(3);
    }

    @DisplayName("프로필 삭제에 성공한다.")
    @Test
    void 프로필_삭제에_성공한다() {
        // given
        Profile mainProfile =
                Profile.builder().profileId(10L).member(member).nickname("메인").build();

        given(memberRepository.findByIdWithLock(1L)).willReturn(Optional.of(member));
        given(profileRepository.findById(10L)).willReturn(Optional.of(mainProfile));
        given(matchingApplicationRepository.existsByProfile(mainProfile)).willReturn(false);

        // when
        profileService.deleteProfile(10L, member);

        // then
        then(profileRepository).should().delete(mainProfile);
    }

    @DisplayName("프로필 삭제 시 매칭 신청 이력이 존재하면 예외가 발생한다.")
    @Test
    void 프로필_삭제_시_매칭_신청_이력이_존재하면_예외가_발생한다() {
        // given
        Profile mainProfile =
                Profile.builder().profileId(10L).member(member).nickname("메인").build();

        given(memberRepository.findByIdWithLock(1L)).willReturn(Optional.of(member));
        given(profileRepository.findById(10L)).willReturn(Optional.of(mainProfile));
        given(matchingApplicationRepository.existsByProfile(mainProfile)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> profileService.deleteProfile(10L, member))
                .isInstanceOf(ProfileException.class)
                .hasMessage(ProfileErrorCode.CANNOT_DELETE_REFERENCED_PROFILE.getMessage());
    }

    @DisplayName("프로젝트명과 카테고리만 입력하고 나머지 필드가 null이어도 정상 등록된다.")
    @Test
    void 프로젝트명과_카테고리만_있어도_정상_등록된다() {
        // given
        Profile profile =
                Profile.builder().profileId(10L).member(member).nickname("러너").build();
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));

        CreateProjectRequest request =
                new CreateProjectRequest("프로젝트", ProjectCategory.CONTEST, null, null, null, null, null, null);

        // when
        ProjectResponse response = profileService.createProject(10L, request, member);

        // then
        assertThat(response.projectName()).isEqualTo("프로젝트");
        assertThat(response.isOngoing()).isFalse();
        assertThat(response.techStacks()).isEmpty();
        then(projectExperienceRepository).should().save(any(ProjectExperience.class));
    }

    @DisplayName("시작일만 입력하고 나머지 필드가 null이어도 정상 등록된다.")
    @Test
    void 시작일만_있어도_정상_등록된다() {
        // given
        Profile profile =
                Profile.builder().profileId(10L).member(member).nickname("러너").build();
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));

        CreateProjectRequest request = new CreateProjectRequest(
                "프로젝트", ProjectCategory.CONTEST, null, null, null, LocalDate.of(2026, 1, 1), null, null);

        // when
        ProjectResponse response = profileService.createProject(10L, request, member);

        // then
        assertThat(response.projectName()).isEqualTo("프로젝트");
        assertThat(response.startedAt()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(response.isOngoing()).isFalse();
        then(projectExperienceRepository).should().save(any(ProjectExperience.class));
    }

    // 프로젝트 경험 CRUD & 날짜 검증 테스트

    @DisplayName("진행 중인 프로젝트 경험을 등록하는 경우 종료일(endedAt)이 null이어도 정상 등록된다.")
    @Test
    void 진행_중인_프로젝트는_종료일이_null이어도_정상_등록된다() {
        // given
        Profile profile =
                Profile.builder().profileId(10L).member(member).nickname("러너").build();
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));

        CreateProjectRequest request = new CreateProjectRequest(
                "프로젝트", ProjectCategory.CONTEST, "설명", "역할", List.of("Spring"), LocalDate.of(2026, 1, 1), null, true);

        // when
        ProjectResponse response = profileService.createProject(10L, request, member);

        // then
        assertThat(response.isOngoing()).isTrue();
        assertThat(response.endedAt()).isNull();
        then(projectExperienceRepository).should().save(any(ProjectExperience.class));
    }

    @DisplayName("프로젝트 종료일(endedAt)이 시작일(startedAt)보다 빠르면 예외가 발생한다.")
    @Test
    void 프로젝트_종료일이_시작일보다_빠르면_예외가_발생한다() {
        // given
        Profile profile =
                Profile.builder().profileId(10L).member(member).nickname("러너").build();
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));

        CreateProjectRequest request = new CreateProjectRequest(
                "프로젝트",
                ProjectCategory.CONTEST,
                "설명",
                "역할",
                List.of("Spring"),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 4, 1),
                false);

        // when & then
        assertThatThrownBy(() -> profileService.createProject(10L, request, member))
                .isInstanceOf(ProfileException.class)
                .hasMessage(ProfileErrorCode.INVALID_PROJECT_PERIOD.getMessage());
    }

    // 수상 경험 CRUD 테스트

    @DisplayName("수상 경험을 등록하면 정상적으로 DB에 저장된다.")
    @Test
    void 수상_경험_등록이_정상_저장된다() {
        // given
        Profile profile =
                Profile.builder().profileId(10L).member(member).nickname("러너").build();
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));

        CreateAwardRequest request = new CreateAwardRequest("대상", "기관", "1위", LocalDate.of(2026, 4, 1));

        // when
        AwardResponse response = profileService.createAward(10L, request, member);

        // then
        assertThat(response.awardName()).isEqualTo("대상");
        then(awardRepository).should().save(any(Award.class));
    }

    @DisplayName("수상 경험 등록 시 미래 날짜를 입력하면 예외가 발생한다.")
    @Test
    void 수상_경험_등록_시_미래_날짜_입력시_예외가_발생한다() {
        // given
        Profile profile =
                Profile.builder().profileId(10L).member(member).nickname("러너").build();
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));

        CreateAwardRequest request =
                new CreateAwardRequest("대상", "기관", "1위", LocalDate.now().plusDays(1));

        // when & then
        assertThatThrownBy(() -> profileService.createAward(10L, request, member))
                .isInstanceOf(ProfileException.class)
                .hasMessage(ProfileErrorCode.INVALID_DATE.getMessage());
    }

    // 자격증 등록 및 중복 검증 테스트

    @DisplayName("동일한 마스터 자격증이 프로필에 중복 등록되어 있으면 자격증 중복 등록 에러가 발생한다.")
    @Test
    void 동일한_자격증_중복_등록_시_예외가_발생한다() {
        // given
        Profile profile =
                Profile.builder().profileId(10L).member(member).nickname("러너").build();
        Certification certification = Certification.builder()
                .certificationId(5L)
                .certificationCode("SQLD")
                .certificateName("SQLD")
                .build();

        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));
        given(certificationRepository.findByCertificationCode("SQLD")).willReturn(Optional.of(certification));
        given(profileCertificationRepository.existsByProfileAndCertification(profile, certification))
                .willReturn(true);

        CreateCertificationRequest request = new CreateCertificationRequest(
                "SQLD", null, CertificationCategory.DATA_AI, "기관", LocalDate.of(2026, 1, 1), false);

        // when & then
        assertThatThrownBy(() -> profileService.createProfileCertification(10L, request, member))
                .isInstanceOf(ProfileException.class)
                .hasMessage(ProfileErrorCode.DUPLICATE_CERTIFICATION.getMessage());
    }

    @DisplayName("직접 입력 자격증을 등록할 때 정보가 정상 저장된다.")
    @Test
    void 직접_입력_자격증이_정상_등록된다() {
        // given
        Profile profile =
                Profile.builder().profileId(10L).member(member).nickname("러너").build();
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));
        given(profileCertificationRepository.existsByProfileAndIsCustomTrueAndCertificateNameIgnoreCase(
                        profile, "커스텀자격증"))
                .willReturn(false);

        CreateCertificationRequest request = new CreateCertificationRequest(
                null, "커스텀자격증", CertificationCategory.COMPUTER_IT, "기관", LocalDate.of(2026, 1, 1), true);

        // when
        ProfileCertificationResponse response = profileService.createProfileCertification(10L, request, member);

        // then
        assertThat(response.isCustom()).isTrue();
        assertThat(response.certificateName()).isEqualTo("커스텀자격증");
        then(profileCertificationRepository).should().save(any(ProfileCertification.class));
    }

    // 존재하지 않는 리소스 및 하위 리소스 정합성 검증 테스트

    @DisplayName("존재하지 않는 프로젝트 경험 상세 조회 시 프로젝트를 찾을 수 없는 예외(PROJECT_NOT_FOUND)가 발생한다.")
    @Test
    void 존재하지_않는_프로젝트_경험_상세_조회_시_예외가_발생한다() {
        // given
        Profile profile =
                Profile.builder().profileId(10L).member(member).nickname("러너").build();
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));
        given(projectExperienceRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> profileService.getProjectDetail(10L, 999L, member))
                .isInstanceOf(ProfileException.class)
                .hasMessage(ProfileErrorCode.PROJECT_NOT_FOUND.getMessage());
    }

    @DisplayName("다른 프로필에 소속된 프로젝트 ID를 사용해 조회하면 접근 차단 예외가 발생한다.")
    @Test
    void 다른_프로필에_소속된_하위_리소스_접근_시_차단된다() {
        // given
        Profile profile =
                Profile.builder().profileId(10L).member(member).nickname("러너").build();
        Profile otherProfile = Profile.builder()
                .profileId(20L)
                .member(member)
                .nickname("다른프로필")
                .build();
        ProjectExperience project = ProjectExperience.builder()
                .projectId(5L)
                .profile(otherProfile)
                .projectName("다른프로젝트")
                .build();

        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));
        given(projectExperienceRepository.findById(5L)).willReturn(Optional.of(project));

        // when & then
        assertThatThrownBy(() -> profileService.getProjectDetail(10L, 5L, member))
                .isInstanceOf(ProfileException.class)
                .hasMessage(ProfileErrorCode.PROJECT_NOT_FOUND.getMessage());
    }

    @DisplayName("진행 종료만 전달하면 기존 종료일을 유지한다.")
    @Test
    void 진행_종료만_전달하면_기존_종료일을_유지한다() {
        Profile profile =
                Profile.builder().profileId(10L).member(member).nickname("러너").build();
        LocalDate originalEndedAt = LocalDate.of(2026, 6, 30);
        ProjectExperience project = ProjectExperience.builder()
                .projectId(20L)
                .profile(profile)
                .projectName("프로젝트")
                .description("설명")
                .role("역할")
                .techStacks(List.of("Spring"))
                .startedAt(LocalDate.of(2026, 1, 1))
                .endedAt(originalEndedAt)
                .isOngoing(true)
                .build();
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));
        given(projectExperienceRepository.findById(20L)).willReturn(Optional.of(project));

        profileService.updateProject(
                10L, 20L, new UpdateProjectRequest(null, null, null, null, null, null, null, false), member);

        assertThat(project.isOngoing()).isFalse();
        assertThat(project.getEndedAt()).isEqualTo(originalEndedAt);
    }

    @DisplayName("종료일만 전달하면 진행 중 상태가 해제된다.")
    @Test
    void 종료일만_전달하면_진행_중_상태가_해제된다() {
        Profile profile =
                Profile.builder().profileId(10L).member(member).nickname("러너").build();
        ProjectExperience project = ProjectExperience.builder()
                .projectId(20L)
                .profile(profile)
                .projectName("프로젝트")
                .description("설명")
                .role("역할")
                .techStacks(List.of("Spring"))
                .startedAt(LocalDate.of(2026, 1, 1))
                .isOngoing(true)
                .build();
        LocalDate endedAt = LocalDate.of(2026, 7, 1);
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));
        given(projectExperienceRepository.findById(20L)).willReturn(Optional.of(project));

        profileService.updateProject(
                10L, 20L, new UpdateProjectRequest(null, null, null, null, null, null, endedAt, null), member);

        assertThat(project.isOngoing()).isFalse();
        assertThat(project.getEndedAt()).isEqualTo(endedAt);
    }

    @DisplayName("프로젝트 콘텐츠가 변경되면 자동으로 AI 요약을 재요청한다.")
    @Test
    void 프로젝트_콘텐츠가_변경되면_자동으로_AI_요약을_재요청한다() {
        Profile profile =
                Profile.builder().profileId(10L).member(member).nickname("러너").build();
        ProjectExperience project = ProjectExperience.builder()
                .projectId(20L)
                .profile(profile)
                .projectName("프로젝트")
                .description("설명")
                .role("역할")
                .techStacks(List.of("Spring"))
                .startedAt(LocalDate.of(2026, 1, 1))
                .isOngoing(true)
                .aiSummary("기존 요약")
                .aiSummaryStatus(AiSummaryStatus.COMPLETED)
                .build();
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));
        given(projectExperienceRepository.findById(20L)).willReturn(Optional.of(project));

        ProjectResponse response = profileService.updateProject(
                10L, 20L, new UpdateProjectRequest("변경된 프로젝트", null, null, null, null, null, null, null), member);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        assertThat(project.getAiSummaryStatus()).isEqualTo(AiSummaryStatus.PENDING);
        assertThat(response.aiSummaryStatus()).isEqualTo("PENDING");
        then(projectExperienceRepository).should().save(project);
        then(projectAiSummaryService).should().generateSummaryAsync(20L, "변경된 프로젝트", "역할", "설명", null);
    }

    @DisplayName("프로젝트 AI 요약 생성 요청 시 정상 접수된다.")
    @Test
    void 프로젝트_AI_요약_생성_요청_시_정상_접수된다() {
        // given
        Profile profile =
                Profile.builder().profileId(10L).member(member).nickname("러너").build();
        ProjectExperience project = ProjectExperience.builder()
                .projectId(20L)
                .profile(profile)
                .projectName("프로젝트")
                .description("설명")
                .role("역할")
                .aiSummaryStatus(AiSummaryStatus.NOT_CREATED)
                .build();
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));
        given(projectExperienceRepository.findById(20L)).willReturn(Optional.of(project));

        // when
        profileService.generateProjectAiSummary(10L, 20L, member);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        // then
        assertThat(project.getAiSummaryStatus()).isEqualTo(AiSummaryStatus.PENDING);
        then(projectExperienceRepository).should().save(project);
        then(projectAiSummaryService).should().generateSummaryAsync(20L, "프로젝트", "역할", "설명", null);
    }

    @DisplayName("이미 생성 중인 프로젝트 AI 요약 생성 요청 시 예외가 발생한다.")
    @Test
    void 이미_생성_중인_프로젝트_AI_요약_생성_요청_시_예외가_발생한다() {
        // given
        Profile profile =
                Profile.builder().profileId(10L).member(member).nickname("러너").build();
        ProjectExperience project = ProjectExperience.builder()
                .projectId(20L)
                .profile(profile)
                .aiSummaryStatus(AiSummaryStatus.PROCESSING)
                .build();
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));
        given(projectExperienceRepository.findById(20L)).willReturn(Optional.of(project));

        // when & then
        assertThatThrownBy(() -> profileService.generateProjectAiSummary(10L, 20L, member))
                .isInstanceOf(ProfileException.class)
                .hasMessage(ProfileErrorCode.AI_SUMMARY_GENERATION_IN_PROGRESS.getMessage());
    }

    @DisplayName("프로젝트 AI 요약 조회 시 성공한다.")
    @Test
    void 프로젝트_AI_요약_조회_시_성공한다() {
        // given
        Profile profile =
                Profile.builder().profileId(10L).member(member).nickname("러너").build();
        ProjectExperience project = ProjectExperience.builder()
                .projectId(20L)
                .profile(profile)
                .aiSummary("생성된 요약")
                .aiSummaryStatus(AiSummaryStatus.COMPLETED)
                .build();
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));
        given(projectExperienceRepository.findById(20L)).willReturn(Optional.of(project));

        // when
        ProjectAiSummaryResponse response = profileService.getProjectAiSummary(10L, 20L, member);

        // then
        assertThat(response.summary()).isEqualTo("생성된 요약");
        assertThat(response.status()).isEqualTo("COMPLETED");
    }

    @DisplayName("생성되지 않은 프로젝트 AI 요약 조회 시 예외가 발생한다.")
    @Test
    void 생성되지_않은_프로젝트_AI_요약_조회_시_예외가_발생한다() {
        // given
        Profile profile =
                Profile.builder().profileId(10L).member(member).nickname("러너").build();
        ProjectExperience project = ProjectExperience.builder()
                .projectId(20L)
                .profile(profile)
                .aiSummaryStatus(AiSummaryStatus.NOT_CREATED)
                .build();
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));
        given(projectExperienceRepository.findById(20L)).willReturn(Optional.of(project));

        // when & then
        assertThatThrownBy(() -> profileService.getProjectAiSummary(10L, 20L, member))
                .isInstanceOf(ProfileException.class)
                .hasMessage(ProfileErrorCode.AI_SUMMARY_NOT_FOUND.getMessage());
    }

    @DisplayName("이미 완료된 프로젝트 AI 요약 재생성 요청 시 정상 접수된다.")
    @Test
    void 이미_완료된_프로젝트_AI_요약_재생성_요청_시_정상_접수된다() {
        // given
        Profile profile =
                Profile.builder().profileId(10L).member(member).nickname("러너").build();
        ProjectExperience project = ProjectExperience.builder()
                .projectId(20L)
                .profile(profile)
                .projectName("프로젝트")
                .description("설명")
                .role("역할")
                .aiSummary("기존 요약")
                .aiSummaryStatus(AiSummaryStatus.COMPLETED)
                .build();
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));
        given(projectExperienceRepository.findById(20L)).willReturn(Optional.of(project));

        // when
        profileService.generateProjectAiSummary(10L, 20L, member);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        // then
        assertThat(project.getAiSummaryStatus()).isEqualTo(AiSummaryStatus.PENDING);
        then(projectExperienceRepository).should().save(project);
        then(projectAiSummaryService).should().generateSummaryAsync(20L, "프로젝트", "역할", "설명", null);
    }

    @DisplayName("프로필 수정 시 닉네임을 변경할 때 trim을 처리하고 본인 제외 중복 체크가 정상 수행된다.")
    @Test
    void 프로필_수정_닉네임_trim_및_본인제외_중복체크() {
        // given
        Profile profile =
                Profile.builder().profileId(10L).member(member).nickname("러너").build();
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));
        given(profileRepository.existsByNicknameAndProfileIdNot("새로운닉네임", 10L)).willReturn(false);

        UpdateProfileRequest request = new UpdateProfileRequest("  새로운닉네임  ", null, null, null, null, null, null, null);

        // when
        profileService.updateProfile(10L, request, member);

        // then
        assertThat(profile.getNickname()).isEqualTo("새로운닉네임");
        then(profileRepository).should().existsByNicknameAndProfileIdNot("새로운닉네임", 10L);
    }

    @DisplayName("자격증 직접 입력 시 명칭이 공백이면 예외가 발생한다.")
    @Test
    void 자격증_직접입력_명칭_공백_예외_발생() {
        // given
        Profile profile =
                Profile.builder().profileId(10L).member(member).nickname("러너").build();
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile));

        CreateCertificationRequest request = new CreateCertificationRequest(
                null, "   ", CertificationCategory.LANGUAGE, "기관", java.time.LocalDate.now(), true);

        // when & then
        assertThatThrownBy(() -> profileService.createProfileCertification(10L, request, member))
                .isInstanceOf(ProfileException.class)
                .hasMessage(ProfileErrorCode.CERTIFICATE_NAME_REQUIRED.getMessage());
    }
}
