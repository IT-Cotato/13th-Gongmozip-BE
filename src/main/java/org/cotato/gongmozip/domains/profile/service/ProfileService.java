package org.cotato.gongmozip.domains.profile.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.CurrentCharacterResponse;
import org.cotato.gongmozip.domains.character.service.CharacterService;
import org.cotato.gongmozip.domains.matching.repository.MatchingApplicationRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.profile.converter.ProfileConverter;
import org.cotato.gongmozip.domains.profile.converter.ProjectEvaluationConverter;
import org.cotato.gongmozip.domains.profile.dto.request.ProfileRequest.*;
import org.cotato.gongmozip.domains.profile.dto.response.ProfileResponse.*;
import org.cotato.gongmozip.domains.profile.entity.*;
import org.cotato.gongmozip.domains.profile.enums.AiSummaryStatus;
import org.cotato.gongmozip.domains.profile.enums.CertificationCategory;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.profile.exception.ProfileException;
import org.cotato.gongmozip.domains.profile.exception.codes.ProfileErrorCode;
import org.cotato.gongmozip.domains.profile.repository.*;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final MemberRepository memberRepository;
    private final ProfileRepository profileRepository;
    private final ProjectExperienceRepository projectExperienceRepository;
    private final AwardRepository awardRepository;
    private final CertificationRepository certificationRepository;
    private final ProfileCertificationRepository profileCertificationRepository;
    private final MatchingApplicationRepository matchingApplicationRepository;
    private final ProjectAiSummaryService projectAiSummaryService;
    private final ProjectAiSummaryTxService projectAiSummaryTxService;
    private final ProjectEvaluationRepository projectEvaluationRepository;
    private final ProjectEvaluationService projectEvaluationService;
    private final ProjectEvaluationTxService projectEvaluationTxService;
    private final CharacterService characterService;

    // 프로필 비즈니스 로직

    @Transactional
    public CreateProfileResponse createProfile(CreateProfileRequest request, Member member) {
        // 동시성 보안: 회원 행에 비관적 락을 겁산 후 대표 프로필 생성
        memberRepository
                .findByIdWithLock(member.getMemberId())
                .orElseThrow(() -> new ProfileException(ProfileErrorCode.PROFILE_ACCESS_DENIED));

        validateGpa(request.gpa(), request.gpaScale());

        // 닉네임 중복 체크 (trim 적용)
        String trimmedNickname = request.nickname() != null ? request.nickname().trim() : "";
        if (profileRepository.existsByNickname(trimmedNickname)) {
            throw new ProfileException(ProfileErrorCode.DUPLICATE_NICKNAME);
        }

        Profile profile = ProfileConverter.toProfile(request, member);
        profileRepository.save(profile);

        return ProfileConverter.toCreateProfileResponse(profile);
    }

    public ProfileListResponse getMyProfiles(Member member) {
        List<Profile> profiles = profileRepository.findAllByMemberOrderByUpdatedAtDesc(member);
        return ProfileConverter.toProfileListResponse(profiles);
    }

    public ProfileDetailResponse getProfileDetail(Long profileId, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);

        List<ProjectExperience> projects = projectExperienceRepository.findAllByProfile(profile);
        List<Award> awards = awardRepository.findAllByProfile(profile);
        List<ProfileCertification> certifications = profileCertificationRepository.findAllByProfile(profile);
        CurrentCharacterResponse character =
                // 없으면 빈 값 허용
                characterService.findCurrentCharacter(member).orElse(null);

        return ProfileConverter.toProfileDetailResponse(profile, projects, awards, certifications, character);
    }

    @Transactional
    public UpdateProfileResponse updateProfile(Long profileId, UpdateProfileRequest request, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);

        // 수정할 필드가 없는 경우
        if (request.nickname() == null
                && request.schoolName() == null
                && request.grade() == null
                && request.major() == null
                && request.secondaryMajor() == null
                && request.gpa() == null
                && request.gpaScale() == null
                && request.interestCategories() == null) {
            throw new ProfileException(ProfileErrorCode.NO_FIELDS_TO_UPDATE);
        }

        // 학점 유효성 검사
        Double newGpa = request.gpa() != null ? request.gpa() : profile.getGpa();
        Double newGpaScale = request.gpaScale() != null ? request.gpaScale() : profile.getGpaScale();
        validateGpa(newGpa, newGpaScale);

        // 닉네임 변경 시 중복 검사 (trim, 공백 검사 및 본인 제외 검증)
        if (request.nickname() != null) {
            String trimmedNickname = request.nickname().trim();
            if (trimmedNickname.isEmpty()) {
                throw new ProfileException(ProfileErrorCode.NO_FIELDS_TO_UPDATE);
            }
            if (!trimmedNickname.equals(profile.getNickname())) {
                if (profileRepository.existsByNicknameAndProfileIdNot(trimmedNickname, profileId)) {
                    throw new ProfileException(ProfileErrorCode.DUPLICATE_NICKNAME);
                }
                profile.updateNickname(trimmedNickname);
            }
        }

        if (request.schoolName() != null) profile.updateSchoolName(request.schoolName());
        if (request.grade() != null) profile.updateGrade(request.grade());
        if (request.major() != null) profile.updateMajor(request.major());
        if (request.secondaryMajor() != null) {
            profile.updateSecondaryMajor(request.secondaryMajor().isEmpty() ? null : request.secondaryMajor());
        }
        if (request.gpa() != null) profile.updateGpa(request.gpa());
        if (request.gpaScale() != null) profile.updateGpaScale(request.gpaScale());
        if (request.interestCategories() != null) profile.updateInterestCategories(request.interestCategories());

        return ProfileConverter.toUpdateProfileResponse(profile);
    }

    @Transactional
    public void deleteProfile(Long profileId, Member member) {
        // 동시성 보안: 회원 행에 비관적 락을 겁산 후 삭제 진행
        memberRepository
                .findByIdWithLock(member.getMemberId())
                .orElseThrow(() -> new ProfileException(ProfileErrorCode.PROFILE_ACCESS_DENIED));

        Profile profile = getProfileAndValidateOwner(profileId, member);

        // 매칭 신청 참조 검증
        if (matchingApplicationRepository.existsByProfile(profile)) {
            throw new ProfileException(ProfileErrorCode.CANNOT_DELETE_REFERENCED_PROFILE);
        }

        profileRepository.delete(profile);
        profileRepository.flush();
    }

    @Transactional
    public UpdateVisibilityResponse updateVisibility(Long profileId, UpdateVisibilityRequest request, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);
        profile.setPublic(request.isPublic());
        return ProfileConverter.toUpdateVisibilityResponse(profile);
    }

    public ProfilePreviewResponse getProfilePreview(Long profileId, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);

        List<ProjectExperience> projects = projectExperienceRepository.findAllByProfile(profile);
        int awardCount = awardRepository.findAllByProfile(profile).size();
        int certificationCount =
                profileCertificationRepository.findAllByProfile(profile).size();
        CurrentCharacterResponse character =
                characterService.findCurrentCharacter(member).orElse(null);

        return ProfileConverter.toProfilePreviewResponse(profile, projects, awardCount, certificationCount, character);
    }

    public PublicProfileResponse getPublicProfile(Long profileId) {
        Profile profile = profileRepository
                .findById(profileId)
                .orElseThrow(() -> new ProfileException(ProfileErrorCode.PROFILE_NOT_FOUND));

        // 비공개 프로필은 (팀 채팅 등에서) 닉네임/아바타만 보여주고 나머지는 비운 채로 응답한다
        // (docs/decisions/03-chat.md의 "팀원 프로필 열람" 참고) — 존재 자체를 숨기지는 않는다.
        if (!profile.isPublic()) {
            CurrentCharacterResponse character =
                    characterService.findCurrentCharacter(profile.getMember()).orElse(null);
            return ProfileConverter.toPrivateProfileResponse(profile, character);
        }

        List<ProjectExperience> projects = projectExperienceRepository.findAllByProfile(profile);
        List<Award> awards = awardRepository.findAllByProfile(profile);
        List<ProfileCertification> certifications = profileCertificationRepository.findAllByProfile(profile);
        CurrentCharacterResponse character =
                characterService.findCurrentCharacter(profile.getMember()).orElse(null);

        return ProfileConverter.toPublicProfileResponse(profile, projects, awards, certifications, character);
    }

    // 프로젝트 경험 비즈니스 로직

    @Transactional
    public ProjectResponse createProject(Long profileId, CreateProjectRequest request, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);
        validateProjectPeriod(request.startedAt(), request.endedAt(), request.isOngoing());

        ProjectExperience project = ProfileConverter.toProjectExperience(request, profile);
        projectExperienceRepository.save(project);
        prepareProjectEvaluationForCreate(project);

        return ProfileConverter.toProjectResponse(project);
    }

    public ProjectListResponse getProjects(Long profileId, int page, int size, String sort, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);

        Sort.Direction direction = "oldest".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable =
                PageRequest.of(page, size, Sort.by(direction, "startedAt").and(Sort.by(direction, "createdAt")));

        Page<ProjectExperience> projectPage = projectExperienceRepository.findAllByProfile(profile, pageable);
        return ProfileConverter.toProjectListResponse(projectPage);
    }

    public ProjectDetailResponse getProjectDetail(Long profileId, Long projectId, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);
        ProjectExperience project = getProjectAndValidateRelation(profileId, projectId);

        return ProfileConverter.toProjectDetailResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(Long profileId, Long projectId, UpdateProjectRequest request, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);
        ProjectExperience project = getProjectAndValidateRelation(profileId, projectId);

        if (request.projectName() == null
                && request.category() == null
                && request.description() == null
                && request.role() == null
                && request.techStacks() == null
                && request.startedAt() == null
                && request.endedAt() == null
                && request.isOngoing() == null) {
            throw new ProfileException(ProfileErrorCode.NO_FIELDS_TO_UPDATE);
        }

        LocalDate started = request.startedAt() != null ? request.startedAt() : project.getStartedAt();
        Boolean ongoing;
        if (request.isOngoing() != null) {
            ongoing = request.isOngoing();
        } else if (request.endedAt() != null) {
            ongoing = false;
        } else {
            ongoing = project.isOngoing();
        }
        LocalDate ended = project.getEndedAt();

        if (Boolean.TRUE.equals(ongoing)) {
            ended = null;
        } else if (request.endedAt() != null) {
            ended = request.endedAt();
        }

        validateProjectPeriod(started, ended, ongoing);

        boolean contentChanged = false;
        if (request.category() != null && request.category() != project.getCategory()) {
            project.updateCategory(request.category());
            contentChanged = true;
        }
        if (request.projectName() != null && !request.projectName().equals(project.getProjectName())) {
            project.updateProjectName(request.projectName());
            contentChanged = true;
        }
        if (request.description() != null && !request.description().equals(project.getDescription())) {
            project.updateDescription(request.description());
            contentChanged = true;
        }
        if (request.role() != null && !request.role().equals(project.getRole())) {
            project.updateRole(request.role());
            contentChanged = true;
        }
        if (request.techStacks() != null && !request.techStacks().equals(project.getTechStacks())) {
            project.updateTechStacks(request.techStacks());
            contentChanged = true;
        }

        if (request.startedAt() != null) project.updateStartedAt(request.startedAt());
        project.updateEndedAt(ended);
        if (request.isOngoing() != null || request.endedAt() != null) project.updateIsOngoing(ongoing);

        // 콘텐츠가 수정되었다면 자동으로 비동기 AI 재요약 트리거
        if (contentChanged
                && project.getAiSummaryStatus() != AiSummaryStatus.PENDING
                && project.getAiSummaryStatus() != AiSummaryStatus.PROCESSING) {
            project.pendingAiSummary();
            projectExperienceRepository.save(project);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        projectAiSummaryService.generateSummaryAsync(
                                project.getProjectId(),
                                project.getProjectName(),
                                project.getRole(),
                                project.getDescription(),
                                null);
                    } catch (TaskRejectedException e) {
                        log.error("자동 AI 요약 트리거 중 쓰레드 풀 포화로 작업 제출 실패", e);
                        projectAiSummaryTxService.failSummary(project.getProjectId());
                    }
                }
            });
        }

        // 콘텐츠가 수정되었다면 기존 평가 유무와 관계없이 매칭용 AI 평가를 다시 생성한다.
        if (contentChanged) {
            prepareProjectEvaluationForUpdate(project);
        }
        String aiStatus = project.getAiSummaryStatus().name();

        return ProfileConverter.toProjectUpdateResponse(project, aiStatus);
    }

    @Transactional
    public void deleteProject(Long profileId, Long projectId, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);
        ProjectExperience project = getProjectAndValidateRelation(profileId, projectId);
        projectExperienceRepository.delete(project);
    }

    // 수상경험 비즈니스 로직

    @Transactional
    public AwardResponse createAward(Long profileId, CreateAwardRequest request, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);
        validateAwardedAt(request.awardedAt());

        Award award = ProfileConverter.toAward(request, profile);
        awardRepository.save(award);

        return ProfileConverter.toAwardResponse(award);
    }

    public AwardListResponse getAwards(Long profileId, int page, int size, String sort, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);

        Sort.Direction direction = "oldest".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable =
                PageRequest.of(page, size, Sort.by(direction, "awardedAt").and(Sort.by(direction, "createdAt")));

        Page<Award> awardPage = awardRepository.findAllByProfile(profile, pageable);
        return ProfileConverter.toAwardListResponse(awardPage);
    }

    public AwardResponse getAwardDetail(Long profileId, Long awardId, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);
        Award award = getAwardAndValidateRelation(profileId, awardId);

        return ProfileConverter.toAwardResponse(award);
    }

    @Transactional
    public AwardResponse updateAward(Long profileId, Long awardId, UpdateAwardRequest request, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);
        Award award = getAwardAndValidateRelation(profileId, awardId);

        if (request.awardName() == null
                && request.organizationName() == null
                && request.awardRank() == null
                && request.awardedAt() == null) {
            throw new ProfileException(ProfileErrorCode.NO_FIELDS_TO_UPDATE);
        }

        if (request.awardedAt() != null) {
            validateAwardedAt(request.awardedAt());
            award.updateAwardedAt(request.awardedAt());
        }

        if (request.awardName() != null) award.updateAwardName(request.awardName());
        if (request.organizationName() != null) award.updateOrganizationName(request.organizationName());
        if (request.awardRank() != null) award.updateAwardRank(request.awardRank());

        return ProfileConverter.toAwardUpdateResponse(award);
    }

    @Transactional
    public void deleteAward(Long profileId, Long awardId, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);
        Award award = getAwardAndValidateRelation(profileId, awardId);
        awardRepository.delete(award);
    }

    // 자격증 비즈니스 로직

    public CertificationCategoriesResponse getCertificationCategories() {
        List<CertificationCategoryResponse> list = Arrays.stream(CertificationCategory.values())
                .map(c -> new CertificationCategoryResponse(c.name(), c.getCategoryName(), c.getDisplayOrder()))
                .collect(Collectors.toList());
        return new CertificationCategoriesResponse(list);
    }

    public CertificationSearchResponse searchCertifications(String keyword, String categoryStr, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        CertificationCategory category = parseAndValidateCategory(categoryStr);
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty())
                ? "%" + keyword.trim().toLowerCase() + "%"
                : null;
        Page<Certification> pageResult =
                certificationRepository.searchCertifications(searchKeyword, category, pageable);
        return ProfileConverter.toCertificationSearchResponse(pageResult);
    }

    @Transactional
    public ProfileCertificationResponse createProfileCertification(
            Long profileId, CreateCertificationRequest request, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);
        validateAwardedAt(request.acquiredAt()); // 취득일도 미래 날짜 불가 동일 검증 사용

        Certification certification = null;
        if (!request.isCustom()) {
            if (request.certificationCode() == null
                    || request.certificationCode().trim().isEmpty()) {
                throw new ProfileException(ProfileErrorCode.CERTIFICATION_CODE_REQUIRED);
            }
            certification = certificationRepository
                    .findByCertificationCode(request.certificationCode())
                    .orElseThrow(() -> new ProfileException(ProfileErrorCode.CERTIFICATION_NOT_FOUND));

            // 중복 검증
            if (profileCertificationRepository.existsByProfileAndCertification(profile, certification)) {
                throw new ProfileException(ProfileErrorCode.DUPLICATE_CERTIFICATION);
            }
        } else {
            if (request.certificateName() == null
                    || request.certificateName().trim().isEmpty()) {
                throw new ProfileException(ProfileErrorCode.CERTIFICATE_NAME_REQUIRED);
            }
            // 직접 입력 자격증 중복 검증
            if (profileCertificationRepository.existsByProfileAndIsCustomTrueAndCertificateNameIgnoreCase(
                    profile, request.certificateName().trim())) {
                throw new ProfileException(ProfileErrorCode.DUPLICATE_CERTIFICATION);
            }
        }

        ProfileCertification profileCertification =
                ProfileConverter.toProfileCertification(request, profile, certification);
        profileCertificationRepository.save(profileCertification);

        return ProfileConverter.toProfileCertificationResponse(profileCertification);
    }

    public ProfileCertificationListResponse getProfileCertifications(
            Long profileId, String categoryStr, int page, int size, String sort, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);
        CertificationCategory category = parseAndValidateCategory(categoryStr);

        Sort dbSort = Sort.by(Sort.Direction.DESC, "acquiredAt").and(Sort.by(Sort.Direction.DESC, "createdAt"));
        if ("oldest".equalsIgnoreCase(sort)) {
            dbSort = Sort.by(Sort.Direction.ASC, "acquiredAt").and(Sort.by(Sort.Direction.ASC, "createdAt"));
        } else if ("name".equalsIgnoreCase(sort)) {
            dbSort = Sort.by(Sort.Direction.ASC, "certificateName");
        }

        Pageable pageable = PageRequest.of(page, size, dbSort);
        Page<ProfileCertification> certPage =
                profileCertificationRepository.findAllByProfileAndCategory(profile, category, pageable);

        return ProfileConverter.toProfileCertificationListResponse(certPage);
    }

    public ProfileCertificationResponse getProfileCertificationDetail(
            Long profileId, Long certificationId, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);
        ProfileCertification cert = getCertificationAndValidateRelation(profileId, certificationId);

        return ProfileConverter.toProfileCertificationResponse(cert);
    }

    @Transactional
    public ProfileCertificationResponse updateProfileCertification(
            Long profileId, Long certificationId, UpdateCertificationRequest request, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);
        ProfileCertification cert = getCertificationAndValidateRelation(profileId, certificationId);

        if (request.certificateName() == null
                && request.categoryCode() == null
                && request.issuer() == null
                && request.acquiredAt() == null) {
            throw new ProfileException(ProfileErrorCode.NO_FIELDS_TO_UPDATE);
        }

        // 마스터 자격증 연동인데 이름이나 카테고리를 변경하려는 경우
        if (!cert.isCustom() && (request.certificateName() != null || request.categoryCode() != null)) {
            throw new ProfileException(ProfileErrorCode.UNSUPPORTED_CERTIFICATION_CATEGORY); // 직접 입력이 아닌 자격증은 수정 불가 에러
        }

        if (request.acquiredAt() != null) {
            validateAwardedAt(request.acquiredAt());
            cert.updateAcquiredAt(request.acquiredAt());
        }

        if (cert.isCustom()) {
            if (request.certificateName() != null
                    && !request.certificateName().trim().equals(cert.getCertificateName())) {
                // 자기 자신을 제외한 중복 체크
                if (profileCertificationRepository
                        .existsByProfileAndIsCustomTrueAndCertificateNameIgnoreCaseAndProfileCertificationIdNot(
                                profile, request.certificateName().trim(), cert.getProfileCertificationId())) {
                    throw new ProfileException(ProfileErrorCode.DUPLICATE_CERTIFICATION);
                }
                cert.updateCertificateName(request.certificateName().trim());
            }
            if (request.categoryCode() != null) {
                cert.updateCategoryCode(request.categoryCode());
            }
        }

        if (request.issuer() != null) cert.updateIssuer(request.issuer());

        return ProfileConverter.toProfileCertificationUpdateResponse(cert);
    }

    @Transactional
    public void deleteProfileCertification(Long profileId, Long certificationId, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);
        ProfileCertification cert = getCertificationAndValidateRelation(profileId, certificationId);
        profileCertificationRepository.delete(cert);
    }

    // 공통 헬퍼 검증 로직

    private Profile getProfileAndValidateOwner(Long profileId, Member member) {
        Profile profile = profileRepository
                .findById(profileId)
                .orElseThrow(() -> new ProfileException(ProfileErrorCode.PROFILE_NOT_FOUND));

        if (!profile.getMember().getMemberId().equals(member.getMemberId())) {
            throw new ProfileException(ProfileErrorCode.PROFILE_ACCESS_DENIED);
        }
        return profile;
    }

    private ProjectExperience getProjectAndValidateRelation(Long profileId, Long projectId) {
        ProjectExperience project = projectExperienceRepository
                .findById(projectId)
                .orElseThrow(() -> new ProfileException(ProfileErrorCode.PROJECT_NOT_FOUND));

        if (!project.getProfile().getProfileId().equals(profileId)) {
            throw new ProfileException(ProfileErrorCode.PROJECT_NOT_FOUND);
        }
        return project;
    }

    private CertificationCategory parseAndValidateCategory(String categoryStr) {
        if (categoryStr == null || categoryStr.trim().isEmpty()) {
            return null;
        }
        CertificationCategory category = CertificationCategory.from(categoryStr);
        if (category == null) {
            throw new ProfileException(ProfileErrorCode.UNSUPPORTED_CERTIFICATION_CATEGORY);
        }
        return category;
    }

    private Award getAwardAndValidateRelation(Long profileId, Long awardId) {
        Award award = awardRepository
                .findById(awardId)
                .orElseThrow(() -> new ProfileException(ProfileErrorCode.AWARD_NOT_FOUND));

        if (!award.getProfile().getProfileId().equals(profileId)) {
            throw new ProfileException(ProfileErrorCode.AWARD_NOT_FOUND);
        }
        return award;
    }

    private ProfileCertification getCertificationAndValidateRelation(Long profileId, Long certificationId) {
        ProfileCertification cert = profileCertificationRepository
                .findById(certificationId)
                .orElseThrow(() -> new ProfileException(ProfileErrorCode.CERTIFICATION_NOT_FOUND));

        if (!cert.getProfile().getProfileId().equals(profileId)) {
            throw new ProfileException(ProfileErrorCode.CERTIFICATION_NOT_FOUND);
        }
        return cert;
    }

    private void validateGpa(Double gpa, Double gpaScale) {
        if (gpa == null || gpaScale == null) return;
        if (gpa < 0 || gpaScale <= 0 || gpa > gpaScale) {
            throw new ProfileException(ProfileErrorCode.INVALID_GPA);
        }
    }

    private void prepareProjectEvaluationForCreate(ProjectExperience project) {
        ProjectEvaluation evaluation = projectEvaluationRepository
                .findByProjectExperience(project)
                .orElseGet(() -> ProjectEvaluationConverter.toProjectEvaluation(project));
        evaluation.pending();
        projectEvaluationRepository.save(evaluation);

        Long projectId = project.getProjectId();
        String projectName = project.getProjectName();
        String role = project.getRole();
        String description = project.getDescription();
        InterestCategory category = project.getProfile().getInterestCategories().isEmpty()
                ? InterestCategory.IT_AI_TECH
                : project.getProfile().getInterestCategories().get(0);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    projectAiSummaryService.generateSummaryAsync(projectId, projectName, role, description, category);
                    projectEvaluationService.evaluateProjectAsync(projectId, projectName, role, description, category);
                } catch (TaskRejectedException e) {
                    log.error("자동 프로젝트 평가 및 요약 트리거 중 쓰레드 풀 포화로 작업 제출 실패", e);
                    projectEvaluationTxService.fail(projectId, "Thread pool saturation: " + e.getMessage());
                    projectAiSummaryTxService.failSummary(projectId);
                }
            }
        });
    }

    private void prepareProjectEvaluationForUpdate(ProjectExperience project) {
        ProjectEvaluation evaluation = projectEvaluationRepository
                .findByProjectExperience(project)
                .orElseGet(() -> ProjectEvaluationConverter.toProjectEvaluation(project));
        evaluation.pending();
        projectEvaluationRepository.save(evaluation);

        Long projectId = project.getProjectId();
        String projectName = project.getProjectName();
        String role = project.getRole();
        String description = project.getDescription();
        InterestCategory category = project.getProfile().getInterestCategories().isEmpty()
                ? InterestCategory.IT_AI_TECH
                : project.getProfile().getInterestCategories().get(0);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    projectEvaluationService.evaluateProjectAsync(projectId, projectName, role, description, category);
                } catch (TaskRejectedException e) {
                    log.error("자동 프로젝트 평가 트리거 중 쓰레드 풀 포화로 작업 제출 실패", e);
                    projectEvaluationTxService.fail(projectId, "Thread pool saturation: " + e.getMessage());
                }
            }
        });
    }

    private void validateProjectPeriod(LocalDate started, LocalDate ended, boolean isOngoing) {
        if (started == null) return;
        if (isOngoing) {
            if (ended != null) {
                throw new ProfileException(ProfileErrorCode.INVALID_PROJECT_PERIOD);
            }
        } else {
            if (ended == null) {
                throw new ProfileException(ProfileErrorCode.INVALID_PROJECT_PERIOD);
            }
            if (ended.isBefore(started)) {
                throw new ProfileException(ProfileErrorCode.INVALID_PROJECT_PERIOD);
            }
        }
    }

    private void validateAwardedAt(LocalDate date) {
        if (date != null && date.isAfter(LocalDate.now())) {
            throw new ProfileException(ProfileErrorCode.INVALID_DATE); // 미래 날짜 검증 실패
        }
    }

    @Transactional
    public void generateProjectAiSummary(Long profileId, Long projectId, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);
        ProjectExperience project = getProjectAndValidateRelation(profileId, projectId);

        // 이미 생성 중(PENDING, PROCESSING)인 경우 예외 처리
        if (project.getAiSummaryStatus() == AiSummaryStatus.PENDING
                || project.getAiSummaryStatus() == AiSummaryStatus.PROCESSING) {
            throw new ProfileException(ProfileErrorCode.AI_SUMMARY_GENERATION_IN_PROGRESS);
        }

        project.pendingAiSummary();
        projectExperienceRepository.save(project);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    projectAiSummaryService.generateSummaryAsync(
                            project.getProjectId(),
                            project.getProjectName(),
                            project.getRole(),
                            project.getDescription(),
                            null);
                } catch (TaskRejectedException e) {
                    log.error("AI 요약 생성 요청 중 쓰레드 풀 포화로 작업 제출 실패", e);
                    projectAiSummaryTxService.failSummary(project.getProjectId());
                }
            }
        });
    }

    public ProjectAiSummaryResponse getProjectAiSummary(Long profileId, Long projectId, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);
        ProjectExperience project = getProjectAndValidateRelation(profileId, projectId);

        if (project.getAiSummaryStatus() == AiSummaryStatus.NOT_CREATED) {
            throw new ProfileException(ProfileErrorCode.AI_SUMMARY_NOT_FOUND);
        }

        return new ProjectAiSummaryResponse(
                project.getAiSummary(), project.getAiSummaryStatus().name(), project.getAiSummaryGeneratedAt());
    }
}
