package org.cotato.gongmozip.domains.profile.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.profile.converter.ProfileConverter;
import org.cotato.gongmozip.domains.profile.dto.request.ProfileRequest.*;
import org.cotato.gongmozip.domains.profile.dto.response.ProfileResponse.*;
import org.cotato.gongmozip.domains.profile.entity.*;
import org.cotato.gongmozip.domains.profile.enums.CertificationCategory;
import org.cotato.gongmozip.domains.profile.exception.ProfileException;
import org.cotato.gongmozip.domains.profile.exception.codes.ProfileErrorCode;
import org.cotato.gongmozip.domains.profile.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // 프로필 비즈니스 로직

    @Transactional
    public CreateProfileResponse createProfile(CreateProfileRequest request, Member member) {
        // 동시성 보안: 회원 행에 비관적 락을 겁산 후 대표 프로필 생성
        memberRepository
                .findByIdWithLock(member.getMemberId())
                .orElseThrow(() -> new ProfileException(ProfileErrorCode.PROFILE_ACCESS_DENIED));

        validateGpa(request.gpa(), request.gpaScale());

        // 닉네임 중복 체크
        if (profileRepository.existsByNickname(request.nickname())) {
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

        return ProfileConverter.toProfileDetailResponse(profile, projects, awards, certifications);
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

        // 닉네임 변경 시 중복 검사
        if (request.nickname() != null && !request.nickname().equals(profile.getNickname())) {
            if (profileRepository.existsByNickname(request.nickname())) {
                throw new ProfileException(ProfileErrorCode.DUPLICATE_NICKNAME);
            }
            profile.updateNickname(request.nickname());
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

        return ProfileConverter.toProfilePreviewResponse(profile, projects, awardCount, certificationCount);
    }

    public PublicProfileResponse getPublicProfile(Long profileId) {
        Profile profile = profileRepository
                .findById(profileId)
                .orElseThrow(() -> new ProfileException(ProfileErrorCode.PROFILE_NOT_FOUND));

        // 비공개 프로필은 존재하지 않는 리소스와 동일하게 처리 (PROFILE_NOT_FOUND)
        if (!profile.isPublic()) {
            throw new ProfileException(ProfileErrorCode.PROFILE_NOT_FOUND);
        }

        List<ProjectExperience> projects = projectExperienceRepository.findAllByProfile(profile);
        List<Award> awards = awardRepository.findAllByProfile(profile);
        List<ProfileCertification> certifications = profileCertificationRepository.findAllByProfile(profile);

        return ProfileConverter.toPublicProfileResponse(profile, projects, awards, certifications);
    }

    // 프로젝트 경험 비즈니스 로직

    @Transactional
    public ProjectResponse createProject(Long profileId, CreateProjectRequest request, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);
        validateProjectPeriod(request.startedAt(), request.endedAt(), request.isOngoing());

        ProjectExperience project = ProfileConverter.toProjectExperience(request, profile);
        projectExperienceRepository.save(project);

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

        // 콘텐츠가 수정되었고 기존 AI 요약이 존재하면 OUTDATED 처리
        String aiStatus = "NOT_CREATED";
        if (project.getAiSummary() != null) {
            if (contentChanged) {
                aiStatus = "OUTDATED";
                project.updateAiSummary(null);
            } else {
                aiStatus = "CREATED";
            }
        }

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

    public CertificationSearchResponse searchCertifications(
            String keyword, CertificationCategory category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Certification> pageResult = certificationRepository.searchCertifications(keyword, category, pageable);
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
                throw new ProfileException(ProfileErrorCode.CERTIFICATION_NOT_FOUND);
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
                throw new ProfileException(ProfileErrorCode.NO_FIELDS_TO_UPDATE); // 필수값이 없으므로 BAD_REQUEST 처리
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
            Long profileId, CertificationCategory category, int page, int size, String sort, Member member) {
        Profile profile = getProfileAndValidateOwner(profileId, member);

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
}
