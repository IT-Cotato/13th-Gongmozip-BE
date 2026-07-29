package org.cotato.gongmozip.domains.mypage.converter;

import java.util.Collections;
import java.util.List;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.CurrentCharacterResponse;
import org.cotato.gongmozip.domains.contest.entity.Contest;
import org.cotato.gongmozip.domains.contest.entity.ContestScrap;
import org.cotato.gongmozip.domains.mypage.dto.response.MyPageResponse.*;
import org.springframework.data.domain.Page;

public class MyPageConverter {

    private MyPageConverter() {}

    public static MyPageMainResponse toMyPageMainResponse(
            CurrentCharacterResponse currentCharacter,
            int scrapContestCount,
            int ongoingProjectCount,
            int completedProjectCount,
            int reviewCount) {
        // 초기 기본 협업거리 (현재 100m, Max 500m, 진행률 20%)
        CollaborationDistanceSummary distanceSummary = new CollaborationDistanceSummary(100, 500, 20);

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

    // TODO: 프로젝트 도메인 구현 후 완료된 프로젝트 연동 및 매핑 로직 추가 필요
    public static CompletedProjectsResponse toCompletedProjectsResponse(int page, int size) {
        return new CompletedProjectsResponse(Collections.emptyList(), page, size, 0L, 0);
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
