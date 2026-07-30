package org.cotato.gongmozip.domains.profile.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;

public class ProfileResponse {

    // 캐릭터 요약 정보 DTO
    public record CharacterSummary(String characterType, String paletteCode) {}

    // 1. 프로필 생성 응답
    public record CreateProfileResponse(Long profileId, String nickname, boolean isPublic, LocalDateTime createdAt) {}

    // 2. 내 프로필 목록 조회 응답
    public record ProfileListResponse(List<ProfileListItemResponse> profiles, int profileCount) {}

    public record ProfileListItemResponse(
            Long profileId,
            String nickname,
            String schoolName,
            Integer grade,
            String major,
            Double gpa,
            Double gpaScale,
            boolean isPublic,
            LocalDateTime updatedAt) {}

    // 3. 프로필 상세 조회 응답
    public record ProfileDetailResponse(
            Long profileId,
            String nickname,
            @Schema(nullable = true, description = "성향 검사 미완료 시 null") CharacterSummary character,
            String schoolName,
            Integer grade,
            String major,
            String secondaryMajor,
            Double gpa,
            Double gpaScale,
            List<InterestCategory> interestCategories,
            boolean isPublic,
            List<ProjectDetailResponse> projects,
            List<AwardListItemResponse> awards,
            List<ProfileCertificationListItem> certifications,
            LocalDateTime updatedAt) {}

    // 4. 프로필 수정 응답
    public record UpdateProfileResponse(
            Long profileId,
            String nickname,
            String schoolName,
            Integer grade,
            String major,
            String secondaryMajor,
            Double gpa,
            Double gpaScale,
            List<InterestCategory> interestCategories,
            LocalDateTime updatedAt) {}

    // 6. 프로필 공개 여부 변경 응답
    public record UpdateVisibilityResponse(Long profileId, boolean isPublic, LocalDateTime updatedAt) {}

    // 7. 프로필 미리보기 조회 응답
    public record ProfilePreviewResponse(
            Long profileId,
            String nickname,
            String characterType,
            String characterPaletteCode,
            String schoolName,
            Integer grade,
            String major,
            String secondaryMajor,
            Double gpa,
            Double gpaScale,
            List<ProjectPreviewSummary> projectSummaries,
            int awardCount,
            int certificationCount,
            boolean isPublic) {}

    public record ProjectPreviewSummary(Long projectId, String projectName, String summary) {}

    // 8. 공개 프로필 조회 응답
    public record PublicProfileResponse(
            Long profileId,
            String nickname,
            @Schema(nullable = true, description = "성향 검사 미완료 시 null") CharacterSummary character,
            String schoolRegion,
            String schoolName,
            Integer grade,
            String major,
            String secondaryMajor,
            List<PublicProjectResponse> projects,
            List<PublicAwardResponse> awards,
            List<PublicCertificationResponse> certifications) {}

    public record PublicProjectResponse(String projectName, String role, String aiSummary) {}

    public record PublicAwardResponse(String awardName, String organizationName) {}

    public record PublicCertificationResponse(String certificateName) {}

    // 9. 프로젝트 경험 등록/수정 응답
    public record ProjectResponse(
            Long projectId,
            Long profileId,
            String projectName,
            String description,
            String role,
            List<String> techStacks,
            LocalDate startedAt,
            LocalDate endedAt,
            boolean isOngoing,
            String aiSummaryStatus,
            LocalDateTime createdAt // 또는 updatedAt으로 공용 사용
            ) {}

    // 10. 프로젝트 경험 목록 조회 응답
    public record ProjectListResponse(
            List<ProjectListItemResponse> projects,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext) {}

    public record ProjectListItemResponse(
            Long projectId,
            String projectName,
            String role,
            List<String> techStacks,
            LocalDate startedAt,
            LocalDate endedAt,
            boolean isOngoing,
            String aiSummary) {}

    // 11. 프로젝트 경험 상세 조회 응답
    public record ProjectDetailResponse(
            Long projectId,
            Long profileId,
            String projectName,
            String description,
            String role,
            List<String> techStacks,
            LocalDate startedAt,
            LocalDate endedAt,
            boolean isOngoing,
            String aiSummary,
            LocalDateTime aiSummaryUpdatedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {}

    // 12. 수상 경험 등록/수정 응답
    public record AwardResponse(
            Long awardId,
            Long profileId,
            String awardName,
            String organizationName,
            String awardRank,
            LocalDate awardedAt,
            LocalDateTime createdAt // 또는 updatedAt
            ) {}

    // 13. 수상 경험 목록 조회 응답
    public record AwardListResponse(
            List<AwardListItemResponse> awards,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext) {}

    public record AwardListItemResponse(
            Long awardId, String awardName, String organizationName, String awardRank, LocalDate awardedAt) {}

    // 14. 자격증 카테고리 목록 조회 응답
    public record CertificationCategoriesResponse(List<CertificationCategoryResponse> categories) {}

    public record CertificationCategoryResponse(String categoryCode, String categoryName, int displayOrder) {}

    // 15. 자격증 검색 응답
    public record CertificationSearchResponse(
            List<CertificationSearchItem> certifications,
            boolean allowCustomInput,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext) {}

    public record CertificationSearchItem(
            String certificationCode, String certificateName, String categoryCode, String categoryName) {}

    // 16. 자격증 등록/수정/상세 응답
    public record ProfileCertificationResponse(
            Long certificationId,
            Long profileId,
            String certificationCode,
            String certificateName,
            String categoryCode,
            String issuer,
            LocalDate acquiredAt,
            boolean isCustom,
            LocalDateTime createdAt // 혹은 updatedAt
            ) {}

    // 17. 자격증 목록 조회 응답
    public record ProfileCertificationListResponse(
            List<ProfileCertificationListItem> certifications,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext) {}

    public record ProfileCertificationListItem(
            Long certificationId,
            String certificateName,
            String categoryCode,
            String categoryName,
            String issuer,
            LocalDate acquiredAt,
            boolean isCustom) {}

    // 18. 프로젝트 AI 요약 조회 응답
    public record ProjectAiSummaryResponse(String summary, String status, LocalDateTime generatedAt) {}

    // 19. 프로젝트 경험 AI 평가 조회 응답
    public record ProjectEvaluationResponse(Long evaluationId, String status, Integer score, String feedback) {}
}
