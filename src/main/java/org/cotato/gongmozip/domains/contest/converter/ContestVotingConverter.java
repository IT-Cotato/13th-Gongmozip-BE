package org.cotato.gongmozip.domains.contest.converter;

import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.contest.dto.response.ContestResponse.ContestCandidateItemResponse;
import org.cotato.gongmozip.domains.contest.dto.response.ContestResponse.ContestCandidateListResponse;
import org.cotato.gongmozip.domains.contest.entity.ContestCandidate;

public final class ContestVotingConverter {

    private ContestVotingConverter() {}

    public static ContestCandidateItemResponse toContestCandidateItemResponse(
            ContestCandidate contestCandidate, LocalDateTime now) {
        return new ContestCandidateItemResponse(
                contestCandidate.getContestCandidateId(),
                ContestConverter.toContestSummaryResponse(contestCandidate.getContest(), now),
                contestCandidate.getAddedByTeamMember().getTeamMemberId());
    }

    public static ContestCandidateListResponse toContestCandidateListResponse(
            List<ContestCandidate> contestCandidates, LocalDateTime now) {
        List<ContestCandidateItemResponse> items = contestCandidates.stream()
                .map(candidate -> toContestCandidateItemResponse(candidate, now))
                .toList();
        return new ContestCandidateListResponse(items);
    }
}
