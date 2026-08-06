package org.cotato.gongmozip.domains.mypage.converter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.CurrentCharacterResponse;
import org.cotato.gongmozip.domains.contest.entity.Contest;
import org.cotato.gongmozip.domains.contest.entity.ContestScrap;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.mypage.dto.response.MyPageResponse.*;
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

    // TODO: 프로젝트 도메인 구현 후 진행 중인 프로젝트 연동 및 매핑 로직 추가 필요
    public static OngoingProjectsResponse toOngoingProjectsResponse(int page, int size) {
        return new OngoingProjectsResponse(Collections.emptyList(), page, size, 0L, 0);
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

    // TODO: 협업 후기/리뷰 도메인 구현 후 리뷰 통계 연동 및 매핑 로직 추가 필요
    public static ReviewStatisticsResponse toReviewStatisticsResponse() {
        return new ReviewStatisticsResponse(0, Collections.emptyList());
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
