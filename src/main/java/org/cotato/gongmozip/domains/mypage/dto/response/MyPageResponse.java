package org.cotato.gongmozip.domains.mypage.dto.response;

import java.util.List;

public class MyPageResponse {

    // 1. 마이페이지 메인 response
    public record MyPageMainResponse(
            CharacterSummary character,
            CollaborationDistanceSummary collaborationDistance,
            MainProfileSummary mainProfile,
            Integer ongoingProjectCount,
            Integer completedProjectCount,
            Integer reviewCount,
            Integer scrapContestCount) {}

    public record CharacterSummary(String characterType, String imageUrl) {}

    public record CollaborationDistanceSummary(Integer current, Integer max, Integer progress) {}

    public record MainProfileSummary(Long profileId, String nickname, String schoolName, String major, Integer grade) {}

    // 2. 진행 중 프로젝트 response
    public record OngoingProjectsResponse(
            List<OngoingProjectItem> projects, Integer page, Integer size, long totalElements, Integer totalPages) {}

    public record OngoingProjectItem(
            Long teamId,
            Long contestId,
            String contestTitle,
            String contestImageUrl,
            String startedAt,
            String deadline,
            Integer memberCount) {}

    // 3. 완료 프로젝트 response
    public record CompletedProjectsResponse(
            List<CompletedProjectItem> projects, Integer page, Integer size, long totalElements, Integer totalPages) {}

    public record CompletedProjectItem(
            Long contestId, String contestTitle, String completedAt, String medal, String award) {}

    // 4. 받은 팀원 후기 response
    public record ReviewStatisticsResponse(Integer totalReviewCount, List<ReviewKeywordItem> keywords) {}

    public record ReviewKeywordItem(String keyword, Integer count) {}

    // 5. 내 스크랩 공모전 response
    public record ScrappedContestsResponse(
            List<ScrappedContestItem> contests, Integer page, Integer size, long totalElements, Integer totalPages) {}

    public record ScrappedContestItem(
            Long contestId, String title, String category, String deadline, boolean isScrapped) {}
}
