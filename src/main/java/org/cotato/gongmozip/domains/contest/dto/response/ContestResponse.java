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

    public record ContestCandidateItemResponse(
            Long contestCandidateId, ContestSummaryResponse contest, Long addedByTeamMemberId) {}

    public record ContestCandidateListResponse(List<ContestCandidateItemResponse> candidates) {}

    // 현재 라운드의 투표 진행 상황과 후보별 득표수. 전원이 투표를 마치기 전에도 조회할 수 있어
    // "N명 참여중" 실시간 카운터와 후보별 득표 막대그래프를 그리는 데 쓴다(투표 자체를 확정하지
    // 않음 — 확정 여부는 팀 채팅에 CONTEST_RESULT_CARD가 오는지로 판단).
    public record ContestVoteStatusResponse(
            int round,
            int requiredVoterCount,
            long participatedVoterCount,
            boolean myVoted,
            List<ContestVoteTallyItemResponse> results) {}

    public record ContestVoteTallyItemResponse(
            Long contestCandidateId, ContestSummaryResponse contest, long voteCount) {}
}
