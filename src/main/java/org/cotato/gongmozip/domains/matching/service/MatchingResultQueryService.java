package org.cotato.gongmozip.domains.matching.service;

import static org.cotato.gongmozip.domains.matching.dto.response.MatchingResultResponse.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.entity.MatchingBatch;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroup;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroupMember;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingResultStatus;
import org.cotato.gongmozip.domains.matching.exception.MatchingException;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.matching.repository.MatchingApplicationRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 저장된 당일 매칭 결과를 공개 시각 이후 본인에게만 제공하는 읽기 전용 서비스다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchingResultQueryService {

    private final MatchingApplicationRepository matchingApplicationRepository;
    private final MatchingGroupMemberRepository matchingGroupMemberRepository;
    private final MatchingTimePolicy matchingTimePolicy;

    public TodayMatchingResultResponse getTodayResult(Long memberId) {
        LocalDate today = matchingTimePolicy.today();
        return matchingApplicationRepository
                .findResultApplication(memberId, today)
                .map(application -> toResult(memberId, application))
                .orElseGet(() -> emptyResult(MatchingResultStatus.NOT_APPLIED));
    }

    private TodayMatchingResultResponse toResult(Long memberId, MatchingApplication application) {
        LocalDate applicationDate = resolveApplicationDate(application);
        LocalDateTime publishedAt = matchingTimePolicy.resultPublishAt(applicationDate);
        MatchingApplicationStatus applicationStatus = application.getStatus();

        if (applicationStatus == MatchingApplicationStatus.CANCELED
                || applicationStatus == MatchingApplicationStatus.PASSED) {
            return applicationOnlyResult(MatchingResultStatus.WITHDRAWN, application, publishedAt);
        }
        // 계산이 먼저 끝나더라도 공개 시각 전에는 그룹·팀원·점수를 조회하지 않는다.
        if (!matchingTimePolicy.isResultPublished(applicationDate, matchingTimePolicy.now())) {
            return applicationOnlyResult(MatchingResultStatus.NOT_PUBLISHED, application, publishedAt);
        }
        if (applicationStatus == MatchingApplicationStatus.FAILED) {
            return applicationOnlyResult(MatchingResultStatus.UNMATCHED, application, publishedAt);
        }
        if (applicationStatus != MatchingApplicationStatus.PROPOSED
                && applicationStatus != MatchingApplicationStatus.MATCHED) {
            return applicationOnlyResult(MatchingResultStatus.PROCESSING, application, publishedAt);
        }

        MatchingGroupMember membership = matchingGroupMemberRepository
                .findResultMembership(application)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.MATCHING_GROUP_NOT_FOUND));
        MatchingGroup group = membership.getMatchingGroup();
        validateResultBelongsToApplication(application, group);

        List<MatchingGroupMember> groupMembers = matchingGroupMemberRepository.findResultMembers(group);
        validateGroupMembers(group, groupMembers, application);

        return new TodayMatchingResultResponse(
                MatchingResultStatus.MATCHED,
                application.getMatchingApplicationId(),
                application.getApplicationDate(),
                applicationStatus,
                publishedAt,
                application.getContestCategory(),
                group.getMatchingGroupId(),
                group.getTeamSize(),
                group.getMatchingScore(),
                toScoreBreakdown(group),
                groupMembers.stream()
                        .map(groupMember -> toMember(memberId, groupMember))
                        .toList());
    }

    private LocalDate resolveApplicationDate(MatchingApplication application) {
        if (application.getApplicationDate() != null) {
            return application.getApplicationDate();
        }
        MatchingBatch batch = application.getMatchingBatch();
        if (batch != null && batch.getApplicationDate() != null) {
            return batch.getApplicationDate();
        }
        throw new MatchingException(MatchingErrorCode.MATCHING_RESULT_NOT_PUBLISHED);
    }

    private TodayMatchingResultResponse emptyResult(MatchingResultStatus resultStatus) {
        return new TodayMatchingResultResponse(
                resultStatus, null, null, null, null, null, null, null, null, null, List.of());
    }

    private TodayMatchingResultResponse applicationOnlyResult(
            MatchingResultStatus resultStatus, MatchingApplication application, LocalDateTime publishedAt) {
        return new TodayMatchingResultResponse(
                resultStatus,
                application.getMatchingApplicationId(),
                application.getApplicationDate(),
                application.getStatus(),
                publishedAt,
                application.getContestCategory(),
                null,
                null,
                null,
                null,
                List.of());
    }

    private MatchingScoreBreakdown toScoreBreakdown(MatchingGroup group) {
        return new MatchingScoreBreakdown(
                group.getLeaderHarmonyScore(),
                group.getGoalSimilarityScore(),
                group.getWorkStyleSimilarityScore(),
                group.getCommunicationSimilarityScore(),
                group.getAgreeablenessSimilarityScore(),
                group.getConscientiousnessSimilarityScore(),
                group.getHonestyHumilitySimilarityScore(),
                group.getExtroversionComplementScore());
    }

    private MatchingResultMemberResponse toMember(Long currentMemberId, MatchingGroupMember groupMember) {
        MatchingApplication application = groupMember.getMatchingApplication();
        return new MatchingResultMemberResponse(
                groupMember.getMember().getMemberId(),
                application.getProfile().getProfileId(),
                application.getProfile().getNickname(),
                application.getCharacterType(),
                application.getLeaderPreference(),
                groupMember.getResponseStatus(),
                groupMember.getMember().getMemberId().equals(currentMemberId));
    }

    private void validateResultBelongsToApplication(MatchingApplication application, MatchingGroup group) {
        if (application.getMatchingBatch() == null
                || group.getMatchingBatch() == null
                || !Objects.equals(
                        application.getMatchingBatch().getMatchingBatchId(),
                        group.getMatchingBatch().getMatchingBatchId())) {
            throw new IllegalStateException("매칭 신청과 결과 그룹의 배치가 일치하지 않습니다.");
        }
    }

    private void validateGroupMembers(
            MatchingGroup group, List<MatchingGroupMember> groupMembers, MatchingApplication application) {
        boolean containsApplication = groupMembers.stream()
                .map(MatchingGroupMember::getMatchingApplication)
                .map(MatchingApplication::getMatchingApplicationId)
                .anyMatch(application.getMatchingApplicationId()::equals);
        if (!containsApplication || group.getTeamSize() == null || groupMembers.size() != group.getTeamSize()) {
            throw new IllegalStateException("매칭 결과의 팀원 구성이 저장된 팀 크기와 일치하지 않습니다.");
        }
    }
}
