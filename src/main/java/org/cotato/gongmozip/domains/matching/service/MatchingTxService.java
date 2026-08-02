package org.cotato.gongmozip.domains.matching.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.LeaderCandidateResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.TitleDescriptionInfo;
import org.cotato.gongmozip.domains.matching.entity.LeaderRecommendation;
import org.cotato.gongmozip.domains.matching.entity.MatchingReason;
import org.cotato.gongmozip.domains.matching.enums.MatchingAiStatus;
import org.cotato.gongmozip.domains.matching.repository.LeaderRecommendationRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingReasonRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchingTxService {

    private final MatchingReasonRepository matchingReasonRepository;
    private final LeaderRecommendationRepository leaderRecommendationRepository;
    private final MemberRepository memberRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startReasonProcessing(Long reasonId) {
        MatchingReason reason = matchingReasonRepository
                .findById(reasonId)
                .orElseThrow(() -> new IllegalArgumentException("MatchingReason not found: " + reasonId));
        if (reason.getStatus() == MatchingAiStatus.PROCESSING || reason.getStatus() == MatchingAiStatus.COMPLETED) {
            throw new IllegalStateException("MatchingReason is already processing or completed: " + reasonId);
        }
        reason.startProcessing();
        matchingReasonRepository.saveAndFlush(reason);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeReason(
            Long reasonId,
            String headline,
            String summary,
            List<TitleDescriptionInfo> strengths,
            List<String> commonPoints,
            List<TitleDescriptionInfo> complementaryPoints,
            List<String> cautions,
            Integer totalCompatibilityScore,
            Integer teamGoalScore,
            Integer personalityScore,
            Integer extraversionComplementScore) {
        MatchingReason reason = matchingReasonRepository
                .findById(reasonId)
                .orElseThrow(() -> new IllegalArgumentException("MatchingReason not found: " + reasonId));

        reason.complete(
                headline,
                summary,
                strengths,
                commonPoints,
                complementaryPoints,
                cautions,
                totalCompatibilityScore,
                teamGoalScore,
                personalityScore,
                extraversionComplementScore);
        matchingReasonRepository.saveAndFlush(reason);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failReason(Long reasonId, String failureMessage) {
        MatchingReason reason = matchingReasonRepository
                .findById(reasonId)
                .orElseThrow(() -> new IllegalArgumentException("MatchingReason not found: " + reasonId));
        reason.fail(failureMessage);
        matchingReasonRepository.saveAndFlush(reason);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startLeaderRecProcessing(Long recId) {
        LeaderRecommendation rec = leaderRecommendationRepository
                .findById(recId)
                .orElseThrow(() -> new IllegalArgumentException("LeaderRecommendation not found: " + recId));
        if (rec.getStatus() == MatchingAiStatus.PROCESSING || rec.getStatus() == MatchingAiStatus.COMPLETED) {
            throw new IllegalStateException("LeaderRecommendation is already processing or completed: " + recId);
        }
        rec.startProcessing();
        leaderRecommendationRepository.saveAndFlush(rec);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeLeaderRec(
            Long recId,
            Long recommendedMemberId,
            String recommendationReason,
            List<LeaderCandidateResponse> candidates,
            String teamSummary,
            String caution) {
        LeaderRecommendation rec = leaderRecommendationRepository
                .findById(recId)
                .orElseThrow(() -> new IllegalArgumentException("LeaderRecommendation not found: " + recId));

        Member recommendedMember = null;
        if (recommendedMemberId != null) {
            recommendedMember = memberRepository
                    .findById(recommendedMemberId)
                    .orElseThrow(() -> new IllegalArgumentException("Member not found: " + recommendedMemberId));
        }

        rec.complete(recommendedMember, recommendationReason, candidates, teamSummary, caution);
        leaderRecommendationRepository.saveAndFlush(rec);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failLeaderRec(Long recId, String failureMessage) {
        LeaderRecommendation rec = leaderRecommendationRepository
                .findById(recId)
                .orElseThrow(() -> new IllegalArgumentException("LeaderRecommendation not found: " + recId));
        rec.fail(failureMessage);
        leaderRecommendationRepository.saveAndFlush(rec);
    }
}
