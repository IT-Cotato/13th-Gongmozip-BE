package org.cotato.gongmozip.domains.profile.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.cotato.gongmozip.domains.profile.enums.CertificationCategory;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;

public class ProfileRequest {

    public record CreateProfileRequest(
            @NotBlank @Size(min = 1, max = 50) String nickname,
            @NotBlank @Size(max = 150) String schoolName,
            @NotNull @Min(1) @Max(6) Integer grade,
            @NotBlank @Size(max = 150) String major,
            @Size(max = 150) String secondaryMajor,
            @NotNull @PositiveOrZero Double gpa,
            @NotNull @Positive Double gpaScale,
            @NotNull List<InterestCategory> interestCategories,
            @NotNull Boolean isPublic) {}

    public record UpdateProfileRequest(
            @Size(min = 1, max = 50) String nickname,
            @Size(max = 150) String schoolName,
            @Min(1) @Max(6) Integer grade,
            @Size(max = 150) String major,
            @Size(max = 150) String secondaryMajor,
            Double gpa,
            Double gpaScale,
            List<InterestCategory> interestCategories) {}

    public record UpdateVisibilityRequest(@NotNull Boolean isPublic) {}

    public record CreateProjectRequest(
            @NotBlank @Size(min = 1, max = 200) String projectName,
            @NotBlank @Size(max = 3000) String description,
            @NotBlank @Size(max = 200) String role,
            @NotNull @Size(min = 1) List<String> techStacks,
            @NotNull LocalDate startedAt,
            LocalDate endedAt,
            @NotNull Boolean isOngoing) {}

    public record UpdateProjectRequest(
            @Size(min = 1, max = 200) String projectName,
            @Size(max = 3000) String description,
            @Size(max = 200) String role,
            List<String> techStacks,
            LocalDate startedAt,
            LocalDate endedAt,
            Boolean isOngoing) {}

    public record CreateAwardRequest(
            @NotBlank @Size(min = 1, max = 200) String awardName,
            @Size(max = 200) String organizationName,
            @Size(max = 100) String awardRank,
            LocalDate awardedAt) {}

    public record UpdateAwardRequest(
            @Size(min = 1, max = 200) String awardName,
            @Size(max = 200) String organizationName,
            @Size(max = 100) String awardRank,
            LocalDate awardedAt) {}

    public record CreateCertificationRequest(
            String certificationCode,
            @Size(max = 100) String certificateName,
            @NotNull CertificationCategory categoryCode,
            @Size(max = 100) String issuer,
            LocalDate acquiredAt,
            @NotNull Boolean isCustom) {}

    public record UpdateCertificationRequest(
            @Size(max = 100) String certificateName,
            CertificationCategory categoryCode,
            @Size(max = 100) String issuer,
            LocalDate acquiredAt) {}

    public record ProjectEvaluationRequest(@NotNull @Positive Long projectExperienceId) {}
}
