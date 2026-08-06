package org.cotato.gongmozip.domains.mypage.converter;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.CurrentCharacterResponse;
import org.cotato.gongmozip.domains.contest.entity.Contest;
import org.cotato.gongmozip.domains.contest.entity.ContestScrap;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.mypage.dto.response.MyPageResponse.*;
import org.cotato.gongmozip.domains.review.entity.Review;
import org.cotato.gongmozip.domains.review.enums.ReviewKeyword;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.springframework.data.domain.Page;

public class MyPageConverter {

    private MyPageConverter() {}

    public static MyPageMainResponse toMyPageMainResponse(
            CurrentCharacterResponse currentCharacter,
            int scrapContestCount,
            int ongoingProjectCount,
            int completedProjectCount,
            int reviewCount,
            int collaborationPoint) {
        int progress = collaborationPoint * 100 / Member.MAX_COLLABORATION_POINT;
        CollaborationDistanceSummary distanceSummary =
                new CollaborationDistanceSummary(collaborationPoint, Member.MAX_COLLABORATION_POINT, progress);

        CharacterSummary characterSummary = currentCharacter == null
                ? null
                : new CharacterSummary(
                        currentCharacter.characterType().name(),
                        currentCharacter.paletteCode().name());

        return new MyPageMainResponse(
                characterSummary,
                distanceSummary,
                ongoingProjectCount,
                completedProjectCount,
                reviewCount,
                scrapContestCount);
    }

    public static OngoingProjectsResponse toOngoingProjectsResponse(
            Page<TeamMember> teamMemberPage, Map<Long, Long> memberCountMap) {
        List<OngoingProjectItem> items = teamMemberPage.getContent().stream()
                .map(tm -> {
                    Team team = tm.getTeam();
                    Contest contest = team.getContest();
                    Long contestId = contest != null ? contest.getContestId() : null;
                    String contestTitle = contest != null ? contest.getTitle() : "선택 안 함";
                    String contestImageUrl = contest != null ? contest.getThumbnailUrl() : null;

                    String startedAt = team.getContestDecidedAt() != null
                            ? team.getContestDecidedAt().toLocalDate().toString()
                            : team.getCreatedAt().toLocalDate().toString();

                    String deadline = contest != null && contest.getApplyEndAt() != null
                            ? contest.getApplyEndAt().toLocalDate().toString()
                            : null;

                    int memberCount =
                            memberCountMap.getOrDefault(team.getTeamId(), 0L).intValue();

                    return new OngoingProjectItem(
                            team.getTeamId(),
                            contestId,
                            contestTitle,
                            contestImageUrl,
                            startedAt,
                            deadline,
                            memberCount);
                })
                .toList();

        return new OngoingProjectsResponse(
                items,
                teamMemberPage.getNumber(),
                teamMemberPage.getSize(),
                teamMemberPage.getTotalElements(),
                teamMemberPage.getTotalPages());
    }

    public static CompletedProjectsResponse toCompletedProjectsResponse(Page<TeamMember> teamMemberPage) {
        List<CompletedProjectItem> items = teamMemberPage.getContent().stream()
                .map(tm -> {
                    Team team = tm.getTeam();
                    Contest contest = team.getContest();
                    Long contestId = contest != null ? contest.getContestId() : null;
                    String contestTitle = contest != null ? contest.getTitle() : "선택 안 함";
                    String completedAtStr = team.getCompletedAt() != null
                            ? team.getCompletedAt().toLocalDate().toString()
                            : team.getUpdatedAt().toLocalDate().toString();

                    String medal = calculateMedal(team);
                    String award = null;

                    return new CompletedProjectItem(
                            team.getTeamId(), contestId, contestTitle, completedAtStr, medal, award);
                })
                .toList();

        return new CompletedProjectsResponse(
                items,
                teamMemberPage.getNumber(),
                teamMemberPage.getSize(),
                teamMemberPage.getTotalElements(),
                teamMemberPage.getTotalPages());
    }

    private static String calculateMedal(Team team) {
        LocalDateTime start = team.getContestDecidedAt() != null ? team.getContestDecidedAt() : team.getCreatedAt();
        LocalDateTime end = team.getCompletedAt() != null ? team.getCompletedAt() : team.getUpdatedAt();
        if (start == null || end == null) {
            return "스프린트 완주 메달";
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(start, end);
        if (days <= 14) {
            return "스프린트 완주 메달";
        } else if (days <= 28) {
            return "크루즈 완주 메달";
        } else {
            return "마라톤 완주 메달";
        }
    }

    public static ReviewStatisticsResponse toReviewStatisticsResponse(List<Review> reviews) {
        int totalReviewCount = reviews.size();

        Map<String, Integer> keywordCounts = new HashMap<>();
        for (ReviewKeyword keyword : ReviewKeyword.values()) {
            keywordCounts.put(keyword.name(), 0);
        }

        for (Review review : reviews) {
            if (review.getKeywords() != null) {
                for (String kwStr : review.getKeywords()) {
                    keywordCounts.put(kwStr, keywordCounts.getOrDefault(kwStr, 0) + 1);
                }
            }
        }

        List<ReviewKeywordItem> keywordItems = keywordCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> new ReviewKeywordItem(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(ReviewKeywordItem::count)
                        .reversed()
                        .thenComparing(ReviewKeywordItem::keyword))
                .toList();

        return new ReviewStatisticsResponse(totalReviewCount, keywordItems);
    }

    public static ScrappedContestsResponse toScrappedContestsResponse(Page<ContestScrap> scrapPage) {
        List<ScrappedContestItem> items = scrapPage.getContent().stream()
                .map(scrap -> {
                    Contest contest = scrap.getContest();
                    String deadline = contest.getApplyEndAt() != null
                            ? contest.getApplyEndAt().toLocalDate().toString()
                            : null;
                    String category = contest.getCategory() != null
                            ? contest.getCategory().name()
                            : null;
                    return new ScrappedContestItem(
                            contest.getContestId(), contest.getTitle(), category, deadline, true);
                })
                .toList();

        return new ScrappedContestsResponse(
                items,
                scrapPage.getNumber(),
                scrapPage.getSize(),
                scrapPage.getTotalElements(),
                scrapPage.getTotalPages());
    }
}
