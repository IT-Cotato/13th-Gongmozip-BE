package org.cotato.gongmozip.domains.contest.converter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.cotato.gongmozip.domains.contest.dto.request.ContestRequest.CreateContestRequest;
import org.cotato.gongmozip.domains.contest.dto.response.ContestResponse.*;
import org.cotato.gongmozip.domains.contest.entity.Contest;
import org.cotato.gongmozip.domains.contest.entity.ContestScrap;
import org.cotato.gongmozip.domains.contest.enums.ContestStatus;
import org.cotato.gongmozip.domains.contest.exception.ContestException;
import org.cotato.gongmozip.domains.contest.exception.codes.ContestErrorCode;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.springframework.data.domain.Page;

public final class ContestConverter {

    private ContestConverter() {}

    // API용 카테고리 문자열 -> 도메인 InterestCategory 매핑
    public static InterestCategory toInterestCategory(String categoryStr) {
        if (categoryStr == null) {
            return null;
        }
        return switch (categoryStr.trim()) {
            case "IT_AI_TECH" -> InterestCategory.IT_AI_TECH;
            case "MARKETING" -> InterestCategory.MARKETING_AD_BRANDING;
            case "IDEA_PLANNING" -> InterestCategory.IDEA_PLANNING;
            case "ART_DESIGN" -> InterestCategory.ART_DESIGN;
            case "PHOTO_VIDEO" -> InterestCategory.PHOTO_VIDEO;
            case "DATA_ANALYSIS" -> InterestCategory.DATA_ANALYSIS;
            default -> throw new ContestException(ContestErrorCode.INVALID_CONTEST_INPUT);
        };
    }

    // 도메인 InterestCategory -> API용 카테고리 문자열 매핑
    public static String toCategoryString(InterestCategory category) {
        if (category == null) {
            return null;
        }
        return switch (category) {
            case IT_AI_TECH -> "IT_AI_TECH";
            case MARKETING_AD_BRANDING -> "MARKETING";
            case IDEA_PLANNING -> "IDEA_PLANNING";
            case ART_DESIGN -> "ART_DESIGN";
            case PHOTO_VIDEO -> "PHOTO_VIDEO";
            case DATA_ANALYSIS -> "DATA_ANALYSIS";
        };
    }

    // API용 모집 상태 문자열 -> 도메인 ContestStatus 매핑
    public static ContestStatus toContestStatus(String statusStr) {
        if (statusStr == null) {
            return null;
        }
        try {
            return ContestStatus.valueOf(statusStr.trim());
        } catch (IllegalArgumentException e) {
            throw new ContestException(ContestErrorCode.INVALID_CONTEST_INPUT);
        }
    }

    // 실시간 마감 여부를 판단하여 모집 상태 반환
    public static String getEffectiveStatus(Contest contest, LocalDateTime now) {
        if (contest.getStatus() == ContestStatus.CLOSED
                || contest.getApplyEndAt().isBefore(now)) {
            return "CLOSED";
        }
        return contest.getStatus().name();
    }

    // 남은 일수 계산 (마감 여부에 따른 일수 판단, 음수는 0으로 클램핑)
    public static int calculateDaysRemaining(LocalDateTime applyEndAt, LocalDateTime now) {
        LocalDate endDate = applyEndAt.toLocalDate();
        LocalDate currentDate = now.toLocalDate();
        int days = (int) ChronoUnit.DAYS.between(currentDate, endDate);
        return Math.max(0, days);
    }

    public static Contest toContest(CreateContestRequest request) {
        List<String> detailUrls = request.detailImageUrls() != null ? request.detailImageUrls() : new ArrayList<>();
        return Contest.builder()
                .title(request.title())
                .summary(request.summary())
                .description(request.description())
                .category(toInterestCategory(request.category()))
                .status(toContestStatus(request.status()))
                .hostName(request.hostName())
                .applyStartAt(request.applyStartAt())
                .applyEndAt(request.applyEndAt())
                .announcementAt(request.announcementAt())
                .eligibilityText(request.eligibilityText())
                .prizeText(request.prizeText())
                .locationText(request.locationText())
                .thumbnailUrl(request.thumbnailUrl())
                .detailImageUrls(detailUrls)
                .sourceUrl(request.sourceUrl())
                .isTeamParticipation(request.isTeamParticipation())
                .minTeamSize(request.minTeamSize())
                .maxTeamSize(request.maxTeamSize())
                .build();
    }

    public static ContestCreateResponse toContestCreateResponse(Contest contest) {
        return new ContestCreateResponse(
                contest.getContestId(),
                contest.getTitle(),
                toCategoryString(contest.getCategory()),
                contest.getStatus().name(),
                contest.getApplyEndAt(),
                contest.getCreatedAt());
    }

    public static ContestUpdateResponse toContestUpdateResponse(Contest contest) {
        return new ContestUpdateResponse(
                contest.getContestId(),
                contest.getTitle(),
                contest.getStatus().name(),
                contest.getApplyEndAt(),
                contest.getUpdatedAt());
    }

    public static ContestDetailResponse toContestDetailResponse(Contest contest, LocalDateTime now) {
        return new ContestDetailResponse(
                contest.getContestId(),
                contest.getTitle(),
                contest.getSummary(),
                contest.getDescription(),
                toCategoryString(contest.getCategory()),
                getEffectiveStatus(contest, now),
                contest.getHostName(),
                contest.getApplyStartAt(),
                contest.getApplyEndAt(),
                contest.getAnnouncementAt(),
                contest.getEligibilityText(),
                contest.getPrizeText(),
                contest.getLocationText(),
                contest.getThumbnailUrl(),
                contest.getDetailImageUrls(),
                contest.getSourceUrl(),
                contest.isTeamParticipation(),
                contest.getMinTeamSize(),
                contest.getMaxTeamSize(),
                calculateDaysRemaining(contest.getApplyEndAt(), now),
                contest.getViewCount());
    }

    public static ContestSummaryResponse toContestSummaryResponse(Contest contest, LocalDateTime now) {
        return new ContestSummaryResponse(
                contest.getContestId(),
                contest.getTitle(),
                toCategoryString(contest.getCategory()),
                getEffectiveStatus(contest, now),
                contest.getHostName(),
                contest.getThumbnailUrl(),
                contest.getApplyEndAt(),
                calculateDaysRemaining(contest.getApplyEndAt(), now));
    }

    public static ContestListResponse toContestListResponse(Page<Contest> contestPage, LocalDateTime now) {
        List<ContestSummaryResponse> summaries = contestPage.getContent().stream()
                .map(contest -> toContestSummaryResponse(contest, now))
                .toList();

        return new ContestListResponse(
                summaries,
                contestPage.getNumber(),
                contestPage.getSize(),
                contestPage.getTotalElements(),
                contestPage.getTotalPages(),
                contestPage.hasNext());
    }

    public static ScrapResponse toScrapResponse(ContestScrap scrap) {
        return new ScrapResponse(scrap.getContest().getContestId(), true, scrap.getCreatedAt());
    }

    public static ScrapStatusResponse toScrapStatusResponse(
            Contest contest, boolean isScrapped, LocalDateTime scrappedAt) {
        return new ScrapStatusResponse(contest.getContestId(), isScrapped, scrappedAt);
    }

    public static SharePreviewResponse toSharePreviewResponse(Contest contest, LocalDateTime now) {
        return new SharePreviewResponse(
                contest.getContestId(),
                contest.getTitle(),
                contest.getThumbnailUrl(),
                toCategoryString(contest.getCategory()),
                contest.getHostName(),
                contest.getApplyEndAt(),
                calculateDaysRemaining(contest.getApplyEndAt(), now),
                "/contests/" + contest.getContestId());
    }
}
