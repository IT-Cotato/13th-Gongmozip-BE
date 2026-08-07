package org.cotato.gongmozip.domains.profile.converter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.CurrentCharacterResponse;
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
    public static Profile toProfile(CreateProfileRequest request, Member member) {
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
                .isPublic(request.isPublic())
                .build();
    }

    public static CreateProfileResponse toCreateProfileResponse(Profile profile) {
        return new CreateProfileResponse(
                profile.getProfileId(), profile.getNickname(), profile.isPublic(), profile.getCreatedAt());
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
            List<ProfileCertification> certifications,
            CurrentCharacterResponse currentCharacter) {
        CharacterSummary character = toCharacterSummary(currentCharacter);

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

    public static UpdateVisibilityResponse toUpdateVisibilityResponse(Profile profile) {
        return new UpdateVisibilityResponse(profile.getProfileId(), profile.isPublic(), profile.getUpdatedAt());
    }

    public static ProfilePreviewResponse toProfilePreviewResponse(
            Profile profile,
            List<ProjectExperience> projects,
            int awardCount,
            int certificationCount,
            CurrentCharacterResponse currentCharacter) {
        String characterType = currentCharacter == null
                ? null
                : currentCharacter.characterType().name();
        String characterPaletteCode =
                currentCharacter == null ? null : currentCharacter.paletteCode().name();

        List<ProjectPreviewSummary> summaries = projects.stream()
                .map(p -> new ProjectPreviewSummary(
                        p.getProjectId(), p.getProjectName(), p.getAiSummary() != null ? p.getAiSummary() : ""))
                .collect(Collectors.toList());

        return new ProfilePreviewResponse(
                profile.getProfileId(),
                profile.getNickname(),
                characterType,
                characterPaletteCode,
                profile.getSchoolName(),
                profile.getGrade(),
                profile.getMajor(),
                profile.getSecondaryMajor(),
                profile.getGpa(),
                profile.getGpaScale(),
                summaries,
                awardCount,
                certificationCount,
                profile.isPublic());
    }

    public static PublicProfileResponse toPublicProfileResponse(
            Profile profile,
            List<ProjectExperience> projects,
            List<Award> awards,
            List<ProfileCertification> certifications,
            CurrentCharacterResponse currentCharacter) {
        CharacterSummary character = toCharacterSummary(currentCharacter);

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
                true,
                schoolRegion,
                maskedSchoolName,
                profile.getGrade(),
                profile.getMajor(),
                profile.getSecondaryMajor(),
                projectDetails,
                awardDetails,
                certDetails);
    }

    // 비공개 프로필 — 닉네임/캐릭터(아바타)만 채우고 나머지는 전부 비워서 내려준다.
    public static PublicProfileResponse toPrivateProfileResponse(
            Profile profile, CurrentCharacterResponse currentCharacter) {
        return new PublicProfileResponse(
                profile.getProfileId(),
                profile.getNickname(),
                toCharacterSummary(currentCharacter),
                false,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of());
    }

    private static CharacterSummary toCharacterSummary(CurrentCharacterResponse currentCharacter) {
        if (currentCharacter == null) {
            return null;
        }
        return new CharacterSummary(
                currentCharacter.characterType().name(),
                currentCharacter.paletteCode().name());
    }

    // 분교(입결 독립) 또는 이원화이지만 입결 차이가 현저한 캠퍼스 패턴
    // 정규화(공백제거+대문자) 후 매칭
    private static final Set<String> REGIONAL_CAMPUS_PATTERNS = Set.of(
            // 분교 – 별도 브랜드·입결
            "연세대학교미래",
            "연세대미래",
            "연세대학교원주",
            "연세대원주",
            "고려대학교세종",
            "고려대세종",
            "한양대학교ERICA",
            "한양대ERICA",
            "한양대학교에리카",
            "한양대에리카",
            "동국대학교경주",
            "동국대경주",
            "동국대학교WISE",
            "동국대WISE",
            "건국대학교글로컬",
            "건국대글로컬",
            // 이원화이지만 입결 차이가 큰 캠퍼스
            "단국대학교천안",
            "단국대천안",
            "홍익대학교세종",
            "홍익대세종",
            "상명대학교천안",
            "상명대천안");

    // 서울 소재 대학교 키워드 (이원화 캠퍼스는 본교 키워드로 포괄)
    private static final Set<String> SEOUL_UNIVERSITY_KEYWORDS = Set.of(
            "서울대학교",
            "서울대",
            "연세대학교",
            "연세대",
            "고려대학교",
            "고려대",
            "서강대학교",
            "서강대",
            "성균관대학교",
            "성균관대",
            "한양대학교",
            "한양대",
            "중앙대학교",
            "중앙대",
            "경희대학교",
            "경희대",
            "한국외국어대학교",
            "한국외대",
            "외국어대",
            "서울시립대학교",
            "시립대",
            "이화여자대학교",
            "이화여대",
            "이화대",
            "건국대학교",
            "건국대",
            "동국대학교",
            "동국대",
            "홍익대학교",
            "홍익대",
            "숙명여자대학교",
            "숙명여대",
            "숙명대",
            "성신여자대학교",
            "성신여대",
            "세종대학교",
            "세종대",
            "광운대학교",
            "광운대",
            "국민대학교",
            "국민대",
            "숭실대학교",
            "숭실대",
            "단국대학교",
            "단국대",
            "명지대학교",
            "명지대",
            "덕성여자대학교",
            "덕성여대",
            "동덕여자대학교",
            "동덕여대",
            "상명대학교",
            "상명대",
            "서울여자대학교",
            "서울여대",
            "가톨릭대학교",
            "가톨릭대",
            "삼육대학교",
            "삼육대",
            "한성대학교",
            "한성대",
            "서경대학교",
            "서경대",
            "성공회대학교",
            "성공회대");

    /**
     * 학교 이름을 기반으로 대학 지역을 판별합니다.
     *
     * <p>판별 기준: 행정적 이원화 편제보다 실제 입결(입시 경쟁률/컷라인) 및 시장에서의 브랜드 위상을 기준으로 합니다.
     *
     * <ol>
     *   <li>입력값 정규화 (공백 제거 + 대문자 변환)
     *   <li>지방 분교 패턴 우선 차단 → "기타 지역 대학교"
     *   <li>"서울캠퍼스" / "서울교정" 명시 여부 확인 → "서울 소재 대학교"
     *   <li>서울 소재 대학 키워드 매칭 → "서울 소재 대학교"
     *   <li>Fallback → "기타 지역 대학교"
     * </ol>
     */
    private static String getSchoolRegion(String schoolName) {
        if (schoolName == null) return null;

        // STEP 1: 정규화
        String clean = schoolName.replaceAll("\\s+", "").toUpperCase();

        // STEP 2: 분교 패턴 우선 차단
        for (String pattern : REGIONAL_CAMPUS_PATTERNS) {
            if (clean.contains(pattern.toUpperCase())) {
                return "기타 지역 대학교";
            }
        }

        // STEP 3: 서울 캠퍼스 명시 여부
        if (clean.contains("서울캠퍼스") || clean.contains("서울교정")) {
            return "서울 소재 대학교";
        }

        // STEP 4: 서울 소재 대학 키워드 매칭
        for (String keyword : SEOUL_UNIVERSITY_KEYWORDS) {
            if (clean.contains(keyword.toUpperCase())) {
                return "서울 소재 대학교";
            }
        }

        // STEP 5: Fallback
        return "기타 지역 대학교";
    }

    // 프로젝트
    public static ProjectExperience toProjectExperience(CreateProjectRequest request, Profile profile) {
        return ProjectExperience.builder()
                .profile(profile)
                .projectName(request.projectName())
                .category(request.category())
                .description(request.description())
                .role(request.role())
                .techStacks(request.techStacks())
                .startedAt(request.startedAt())
                .endedAt(request.endedAt())
                .isOngoing(request.isOngoing())
                .build();
    }

    public static ProjectResponse toProjectResponse(ProjectExperience project) {
        String aiStatus = project.getAiSummaryStatus().name();
        return new ProjectResponse(
                project.getProjectId(),
                project.getProfile().getProfileId(),
                project.getProjectName(),
                project.getCategory(),
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
                project.getCategory(),
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
                project.getCategory(),
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
                project.getCategory(),
                project.getDescription(),
                project.getRole(),
                project.getTechStacks(),
                project.getStartedAt(),
                project.getEndedAt(),
                project.isOngoing(),
                project.getAiSummary(),
                project.getAiSummaryGeneratedAt(), // AI 요약 업데이트 시각
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

    // 자격증
    public static ProfileCertification toProfileCertification(
            CreateCertificationRequest request, Profile profile, Certification certification) {
        // 마스터 자격증이 있는 경우 이름과 카테고리를 마스터 자격증 기준으로 세팅
        String sourceName = certification != null ? certification.getCertificateName() : request.certificateName();
        String certName = sourceName != null ? sourceName.trim() : null;
        org.cotato.gongmozip.domains.profile.enums.CertificationCategory categoryCode =
                certification != null ? certification.getCategoryCode() : request.categoryCode();
        return ProfileCertification.builder()
                .profile(profile)
                .certification(certification)
                .certificateName(certName)
                .categoryCode(categoryCode)
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
