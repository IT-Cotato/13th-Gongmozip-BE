package org.cotato.gongmozip.domains.contest.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public final class ContestResponse {

    private ContestResponse() {}

    public record ContestCreateResponse(
            Long contestId,
            String title,
            String category,
            String status,
            LocalDateTime applyEndAt,
            LocalDateTime createdAt) {}

    public record ContestUpdateResponse(
            Long contestId, String title, String status, LocalDateTime applyEndAt, LocalDateTime updatedAt) {}

    public record ContestDetailResponse(
            Long contestId,
            String title,
            String summary,
            String description,
            String category,
            String status,
            String hostName,
            LocalDateTime applyStartAt,
            LocalDateTime applyEndAt,
            LocalDateTime announcementAt,
            String eligibilityText,
            String prizeText,
            String locationText,
            String thumbnailUrl,
            List<String> detailImageUrls,
            String sourceUrl,
            Boolean isTeamParticipation,
            Integer minTeamSize,
            Integer maxTeamSize,
            Integer daysRemaining,
            Integer viewCount) {}

    public record ContestSummaryResponse(
            Long contestId,
            String title,
            String category,
            String status,
            String hostName,
            String thumbnailUrl,
            LocalDateTime applyEndAt,
            Integer daysRemaining) {}

    public record ContestListResponse(
            List<ContestSummaryResponse> contests,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext) {}

    public record ScrapResponse(Long contestId, Boolean isScrapped, LocalDateTime scrappedAt) {}

    public record ScrapStatusResponse(Long contestId, Boolean isScrapped, LocalDateTime scrappedAt) {}

    public record SharePreviewResponse(
            Long contestId,
            String title,
            String thumbnailUrl,
            String category,
            String hostName,
            LocalDateTime applyEndAt,
            Integer daysRemaining,
            String detailUrl) {}
}
