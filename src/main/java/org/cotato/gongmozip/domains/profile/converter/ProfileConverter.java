package org.cotato.gongmozip.domains.profile.converter;

import java.util.List;
import java.util.stream.Collectors;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.dto.request.ProfileRequest.CreateAwardRequest;
import org.cotato.gongmozip.domains.profile.dto.request.ProfileRequest.CreateCertificationRequest;
import org.cotato.gongmozip.domains.profile.dto.request.ProfileRequest.CreateProfileRequest;
import org.cotato.gongmozip.domains.profile.dto.request.ProfileRequest.CreateProjectRequest;
import org.cotato.gongmozip.domains.profile.dto.response.ProfileResponse.*;
import org.cotato.gongmozip.domains.profile.entity.*;
import org.springframework.data.domain.Page;

public class ProfileConverter {

    // 프로필
    public static Profile toProfile(CreateProfileRequest request, Member member, boolean isMain) {
        return Profile.builder()
                .member(member)
                .nickname(request.nickname())
                .schoolName(request.schoolName())
                .grade(request.grade())
                .major(request.major())
                .secondaryMajor(request.secondaryMajor())
                .gpa(request.gpa())
                .gpaScale(request.gpaScale())
                .interestCategories(request.interestCategories())
                .isMain(isMain)
                .isPublic(request.isPublic() == null ? true : request.isPublic())
                .build();
    }

    public static CreateProfileResponse toCreateProfileResponse(Profile profile) {
        return new CreateProfileResponse(
                profile.getProfileId(),
                profile.getNickname(),
                profile.isMain(),
                profile.isPublic(),
                profile.getCreatedAt());
    }

    public static ProfileListItemResponse toProfileListItemResponse(Profile profile) {
        return new ProfileListItemResponse(
                profile.getProfileId(),
                profile.getNickname(),
                profile.getSchoolName(),
                profile.getGrade(),
                profile.getMajor(),
                profile.getGpa(),
                profile.getGpaScale(),
                profile.isMain(),
                profile.isPublic(),
                profile.getUpdatedAt());
    }

    public static ProfileListResponse toProfileListResponse(List<Profile> profiles) {
        List<ProfileListItemResponse> list = profiles.stream()
                .map(ProfileConverter::toProfileListItemResponse)
                .collect(Collectors.toList());
        return new ProfileListResponse(list, list.size());
    }

    public static ProfileDetailResponse toProfileDetailResponse(
            Profile profile,
            List<ProjectExperience> projects,
            List<Award> awards,
            List<ProfileCertification> certifications) {
        // character는 성향 검사 전이므로 null
        CharacterSummary character = null;

        List<ProjectDetailResponse> projectDetails =
                projects.stream().map(ProfileConverter::toProjectDetailResponse).collect(Collectors.toList());

        List<AwardListItemResponse> awardDetails =
                awards.stream().map(ProfileConverter::toAwardListItemResponse).collect(Collectors.toList());

        List<ProfileCertificationListItem> certDetails = certifications.stream()
                .map(ProfileConverter::toProfileCertificationListItem)
                .collect(Collectors.toList());

        return new ProfileDetailResponse(
                profile.getProfileId(),
                profile.getNickname(),
                character,
                profile.getSchoolName(),
                profile.getGrade(),
                profile.getMajor(),
                profile.getSecondaryMajor(),
                profile.getGpa(),
                profile.getGpaScale(),
                profile.getInterestCategories(),
                profile.isMain(),
                profile.isPublic(),
                projectDetails,
                awardDetails,
                certDetails,
                profile.getUpdatedAt());
    }

    public static UpdateProfileResponse toUpdateProfileResponse(Profile profile) {
        return new UpdateProfileResponse(
                profile.getProfileId(),
                profile.getNickname(),
                profile.getSchoolName(),
                profile.getGrade(),
                profile.getMajor(),
                profile.getSecondaryMajor(),
                profile.getGpa(),
                profile.getGpaScale(),
                profile.getInterestCategories(),
                profile.getUpdatedAt());
    }

    public static UpdateMainProfileResponse toUpdateMainProfileResponse(Profile profile) {
        return new UpdateMainProfileResponse(
                profile.getProfileId(), profile.getNickname(), profile.isMain(), profile.getUpdatedAt());
    }

    public static UpdateVisibilityResponse toUpdateVisibilityResponse(Profile profile) {
        return new UpdateVisibilityResponse(profile.getProfileId(), profile.isPublic(), profile.getUpdatedAt());
    }

    public static ProfilePreviewResponse toProfilePreviewResponse(
            Profile profile, List<ProjectExperience> projects, int awardCount, int certificationCount) {
        // AI 캐릭터 없음으로 설정 (null/기본값)
        String characterType = null;
        String characterImageUrl = null;

        List<ProjectPreviewSummary> summaries = projects.stream()
                .map(p -> new ProjectPreviewSummary(
                        p.getProjectId(), p.getProjectName(), p.getAiSummary() != null ? p.getAiSummary() : ""))
                .collect(Collectors.toList());

        return new ProfilePreviewResponse(
                profile.getProfileId(),
                profile.getNickname(),
                characterType,
                characterImageUrl,
                profile.getSchoolName(),
                profile.getGrade(),
                profile.getMajor(),
                profile.getSecondaryMajor(),
                profile.getGpa(),
                profile.getGpaScale(),
                summaries,
                awardCount,
                certificationCount,
                profile.isMain(),
                profile.isPublic());
    }

    public static PublicProfileResponse toPublicProfileResponse(
            Profile profile,
            List<ProjectExperience> projects,
            List<Award> awards,
            List<ProfileCertification> certifications) {
        CharacterSummary character = null;

        String schoolRegion = getSchoolRegion(profile.getSchoolName());
        String maskedSchoolName = "ㅇㅇ대학교"; // 명세서 예시에 따른 마스킹 처리

        List<PublicProjectResponse> projectDetails = projects.stream()
                .map(p -> new PublicProjectResponse(p.getProjectName(), p.getRole(), p.getAiSummary()))
                .collect(Collectors.toList());

        List<PublicAwardResponse> awardDetails = awards.stream()
                .map(a -> new PublicAwardResponse(a.getAwardName(), a.getOrganizationName()))
                .collect(Collectors.toList());

        List<PublicCertificationResponse> certDetails = certifications.stream()
                .map(c -> new PublicCertificationResponse(c.getCertificateName()))
                .collect(Collectors.toList());

        return new PublicProfileResponse(
                profile.getProfileId(),
                profile.getNickname(),
                character,
                schoolRegion,
                maskedSchoolName,
                profile.getGrade(),
                profile.getMajor(),
                profile.getSecondaryMajor(),
                projectDetails,
                awardDetails,
                certDetails);
    }

    private static String getSchoolRegion(String schoolName) {
        if (schoolName == null) return null;
        if (schoolName.contains("숙명")
                || schoolName.contains("서울")
                || schoolName.contains("연세")
                || schoolName.contains("고려")
                || schoolName.contains("한양")
                || schoolName.contains("서강")
                || schoolName.contains("성균관")
                || schoolName.contains("이화")
                || schoolName.contains("중앙")
                || schoolName.contains("경희")
                || schoolName.contains("외대")
                || schoolName.contains("시립")
                || schoolName.contains("건국")
                || schoolName.contains("동국")
                || schoolName.contains("홍익")
                || schoolName.contains("국민")
                || schoolName.contains("숭실")
                || schoolName.contains("세종")
                || schoolName.contains("단국")
                || schoolName.contains("광운")
                || schoolName.contains("명지")
                || schoolName.contains("상명")
                || schoolName.contains("가톨릭")
                || schoolName.contains("덕성")
                || schoolName.contains("동덕")
                || schoolName.contains("서울여대")) {
            return "서울 소재 대학교";
        }
        return "기타 지역 대학교";
    }

    // 프로젝트
    public static ProjectExperience toProjectExperience(CreateProjectRequest request, Profile profile) {
        return ProjectExperience.builder()
                .profile(profile)
                .projectName(request.projectName())
                .description(request.description())
                .role(request.role())
                .techStacks(request.techStacks())
                .startedAt(request.startedAt())
                .endedAt(request.endedAt())
                .isOngoing(request.isOngoing())
                .build();
    }

    public static ProjectResponse toProjectResponse(ProjectExperience project) {
        String aiStatus = project.getAiSummary() == null ? "NOT_CREATED" : "CREATED";
        return new ProjectResponse(
                project.getProjectId(),
                project.getProfile().getProfileId(),
                project.getProjectName(),
                project.getDescription(),
                project.getRole(),
                project.getTechStacks(),
                project.getStartedAt(),
                project.getEndedAt(),
                project.isOngoing(),
                aiStatus,
                project.getCreatedAt());
    }

    public static ProjectResponse toProjectUpdateResponse(ProjectExperience project, String aiSummaryStatus) {
        return new ProjectResponse(
                project.getProjectId(),
                project.getProfile().getProfileId(),
                project.getProjectName(),
                project.getDescription(),
                project.getRole(),
                project.getTechStacks(),
                project.getStartedAt(),
                project.getEndedAt(),
                project.isOngoing(),
                aiSummaryStatus,
                project.getUpdatedAt());
    }

    public static ProjectListItemResponse toProjectListItemResponse(ProjectExperience project) {
        return new ProjectListItemResponse(
                project.getProjectId(),
                project.getProjectName(),
                project.getRole(),
                project.getTechStacks(),
                project.getStartedAt(),
                project.getEndedAt(),
                project.isOngoing(),
                project.getAiSummary());
    }

    public static ProjectListResponse toProjectListResponse(Page<ProjectExperience> page) {
        List<ProjectListItemResponse> list = page.getContent().stream()
                .map(ProfileConverter::toProjectListItemResponse)
                .collect(Collectors.toList());
        return new ProjectListResponse(
                list, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.hasNext());
    }

    public static ProjectDetailResponse toProjectDetailResponse(ProjectExperience project) {
        return new ProjectDetailResponse(
                project.getProjectId(),
                project.getProfile().getProfileId(),
                project.getProjectName(),
                project.getDescription(),
                project.getRole(),
                project.getTechStacks(),
                project.getStartedAt(),
                project.getEndedAt(),
                project.isOngoing(),
                project.getAiSummary(),
                project.getAiSummary() != null ? project.getUpdatedAt() : null, // AI 요약 업데이트 시각
                project.getCreatedAt(),
                project.getUpdatedAt());
    }

    // === Award ===
    public static Award toAward(CreateAwardRequest request, Profile profile) {
        return Award.builder()
                .profile(profile)
                .awardName(request.awardName())
                .organizationName(request.organizationName())
                .awardRank(request.awardRank())
                .awardedAt(request.awardedAt())
                .build();
    }

    public static AwardResponse toAwardResponse(Award award) {
        return new AwardResponse(
                award.getAwardId(),
                award.getProfile().getProfileId(),
                award.getAwardName(),
                award.getOrganizationName(),
                award.getAwardRank(),
                award.getAwardedAt(),
                award.getCreatedAt());
    }

    public static AwardResponse toAwardUpdateResponse(Award award) {
        return new AwardResponse(
                award.getAwardId(),
                award.getProfile().getProfileId(),
                award.getAwardName(),
                award.getOrganizationName(),
                award.getAwardRank(),
                award.getAwardedAt(),
                award.getUpdatedAt());
    }

    public static AwardListItemResponse toAwardListItemResponse(Award award) {
        return new AwardListItemResponse(
                award.getAwardId(),
                award.getAwardName(),
                award.getOrganizationName(),
                award.getAwardRank(),
                award.getAwardedAt());
    }

    public static AwardListResponse toAwardListResponse(Page<Award> page) {
        List<AwardListItemResponse> list = page.getContent().stream()
                .map(ProfileConverter::toAwardListItemResponse)
                .collect(Collectors.toList());
        return new AwardListResponse(
                list, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.hasNext());
    }

    // === Certification ===
    public static ProfileCertification toProfileCertification(
            CreateCertificationRequest request, Profile profile, Certification certification) {
        String certName = certification != null ? certification.getCertificateName() : request.certificateName();
        return ProfileCertification.builder()
                .profile(profile)
                .certification(certification)
                .certificateName(certName)
                .categoryCode(request.categoryCode())
                .issuer(request.issuer())
                .acquiredAt(request.acquiredAt())
                .isCustom(request.isCustom())
                .build();
    }

    public static ProfileCertificationResponse toProfileCertificationResponse(ProfileCertification cert) {
        String code = cert.getCertification() != null ? cert.getCertification().getCertificationCode() : null;
        return new ProfileCertificationResponse(
                cert.getProfileCertificationId(),
                cert.getProfile().getProfileId(),
                code,
                cert.getCertificateName(),
                cert.getCategoryCode().name(),
                cert.getIssuer(),
                cert.getAcquiredAt(),
                cert.isCustom(),
                cert.getCreatedAt());
    }

    public static ProfileCertificationResponse toProfileCertificationUpdateResponse(ProfileCertification cert) {
        String code = cert.getCertification() != null ? cert.getCertification().getCertificationCode() : null;
        return new ProfileCertificationResponse(
                cert.getProfileCertificationId(),
                cert.getProfile().getProfileId(),
                code,
                cert.getCertificateName(),
                cert.getCategoryCode().name(),
                cert.getIssuer(),
                cert.getAcquiredAt(),
                cert.isCustom(),
                cert.getUpdatedAt());
    }

    public static ProfileCertificationListItem toProfileCertificationListItem(ProfileCertification cert) {
        return new ProfileCertificationListItem(
                cert.getProfileCertificationId(),
                cert.getCertificateName(),
                cert.getCategoryCode().name(),
                cert.getCategoryCode().getCategoryName(),
                cert.getIssuer(),
                cert.getAcquiredAt(),
                cert.isCustom());
    }

    public static ProfileCertificationListResponse toProfileCertificationListResponse(Page<ProfileCertification> page) {
        List<ProfileCertificationListItem> list = page.getContent().stream()
                .map(ProfileConverter::toProfileCertificationListItem)
                .collect(Collectors.toList());
        return new ProfileCertificationListResponse(
                list, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.hasNext());
    }

    public static CertificationSearchItem toCertificationSearchItem(Certification cert) {
        return new CertificationSearchItem(
                cert.getCertificationCode(),
                cert.getCertificateName(),
                cert.getCategoryCode().name(),
                cert.getCategoryCode().getCategoryName());
    }

    public static CertificationSearchResponse toCertificationSearchResponse(Page<Certification> page) {
        List<CertificationSearchItem> list = page.getContent().stream()
                .map(ProfileConverter::toCertificationSearchItem)
                .collect(Collectors.toList());
        return new CertificationSearchResponse(
                list,
                true, // allowCustomInput = true 항상 고정
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext());
    }
}
