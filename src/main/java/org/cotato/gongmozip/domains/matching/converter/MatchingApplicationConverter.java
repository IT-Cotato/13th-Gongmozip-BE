package org.cotato.gongmozip.domains.matching.converter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.matching.dto.response.MatchingApplicationResponse.ApplicationResponse;
import org.cotato.gongmozip.domains.matching.dto.response.MatchingApplicationResponse.EligibilityResponse;
import org.cotato.gongmozip.domains.matching.dto.response.MatchingApplicationResponse.TodayApplicationResponse;
import org.cotato.gongmozip.domains.matching.dto.response.MatchingApplicationResponse.WithdrawalAvailability;
import org.cotato.gongmozip.domains.matching.dto.response.MatchingApplicationResponse.WithdrawalResponse;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingIneligibilityReason;
import org.cotato.gongmozip.domains.matching.enums.WithdrawalType;
import org.cotato.gongmozip.domains.matching.vo.SkillScoreSnapshot;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.survey.entity.SurveySubmission;

public final class MatchingApplicationConverter {

    private MatchingApplicationConverter() {}

    // 프로필·설문·역량 계산 결과를 매칭 신청 시점의 스냅샷 엔티티로 변환
    public static MatchingApplication toMatchingApplication(
            SurveySubmission submission,
            Profile profile,
            InterestCategory contestCategory,
            LeaderPreference leaderPreference,
            LocalDate applicationDate,
            SkillScoreSnapshot skillScore,
            int collaborationDistance) {
        return MatchingApplication.builder()
                .member(submission.getMember())
                .profile(profile)
                .applicationDate(applicationDate)
                .status(MatchingApplicationStatus.WAITING)
                .leaderPreference(leaderPreference)
                .firstMatching(skillScore.firstMatching())
                .contestCategory(contestCategory)
                .gpaScore(skillScore.gpaScore())
                .projectScore(skillScore.projectScore())
                .awardScore(skillScore.awardScore())
                .certificationScore(skillScore.certificationScore())
                .collaborationScore(skillScore.collaborationScore())
                .skillScore(skillScore.totalScore())
                .collaborationDistance(collaborationDistance)
                .agreeablenessScore(submission.getAgreeablenessScore())
                .conscientiousnessScore(submission.getConscientiousnessScore())
                .honestyHumilityScore(submission.getHonestyHumilityScore())
                .extroversionScore(submission.getExtroversionScore())
                .goalPreferenceScore(submission.getGoalPreferenceScore())
                .workStyleScore(submission.getWorkStyleScore())
                .communicationStyleScore(submission.getCommunicationStyleScore())
                .extroversion2Score(submission.getExtroversion2Score())
                .extroversion3Score(submission.getExtroversion3Score())
                .extroversionType(submission.getExtroversionType())
                .characterType(submission.getCharacterType())
                .characterXScore(submission.getCharacterXScore())
                .characterYScore(submission.getCharacterYScore())
                .build();
    }

    // 신청 자격 검사 결과와 대상 신청일 매칭풀 현황을 조회 응답으로 변환
    public static EligibilityResponse toEligibilityResponse(
            List<MatchingIneligibilityReason> reasons,
            boolean hasProfile,
            boolean surveyCompleted,
            boolean appliedToday,
            LocalDateTime matchingBlockedUntil,
            LocalDate applicationDate,
            LocalDateTime applicationDeadlineAt,
            long participantCount) {
        return new EligibilityResponse(
                reasons.isEmpty(),
                List.copyOf(reasons),
                hasProfile,
                surveyCompleted,
                appliedToday,
                matchingBlockedUntil,
                applicationDate,
                applicationDeadlineAt,
                participantCount);
    }

    // 저장된 신청 엔티티를 신청 완료 응답으로 변환
    public static ApplicationResponse toApplicationResponse(
            MatchingApplication application, LocalDateTime applicationDeadlineAt) {
        return new ApplicationResponse(
                application.getMatchingApplicationId(),
                application.getStatus().name(),
                application.getApplicationDate(),
                application.getContestCategory(),
                application.getLeaderPreference(),
                application.getSkillScore(),
                application.getSkillGroup(),
                application.getCollaborationDistance(),
                applicationDeadlineAt);
    }

    // 오늘 신청 엔티티와 서비스에서 판정한 철회 정보를 하나의 조회 응답으로 변환
    public static TodayApplicationResponse toTodayApplicationResponse(
            MatchingApplication application, WithdrawalAvailability withdrawal) {
        return new TodayApplicationResponse(
                true,
                application.getMatchingApplicationId(),
                application.getStatus().name(),
                application.getApplicationDate(),
                application.getContestCategory(),
                application.getLeaderPreference(),
                application.getSkillScore(),
                application.getSkillGroup(),
                application.getCollaborationDistance(),
                withdrawal);
    }

    // 오늘 신청이 없는 경우 사용하는 빈 조회 응답
    public static TodayApplicationResponse toEmptyTodayApplicationResponse() {
        return new TodayApplicationResponse(false, null, "NONE", null, null, null, null, null, null, null);
    }

    // 서비스에서 계산한 철회 유형·예상 감점·마감 시각을 철회 가능 정보로 변환
    public static WithdrawalAvailability toWithdrawalAvailability(
            WithdrawalType type, int expectedPenalty, LocalDateTime deadlineAt) {
        return new WithdrawalAvailability(true, type, expectedPenalty, deadlineAt);
    }

    // 신청이 없거나 이미 처리되어 철회할 수 없는 경우 사용하는 응답
    public static WithdrawalAvailability toUnavailableWithdrawalAvailability() {
        return new WithdrawalAvailability(false, null, 0, null);
    }

    // 철회로 변경된 신청 상태와 실제 협업거리 차감 결과를 응답으로 변환
    public static WithdrawalResponse toWithdrawalResponse(
            MatchingApplication application,
            WithdrawalType withdrawalType,
            int collaborationPenalty,
            int currentCollaborationDistance) {
        return new WithdrawalResponse(
                application.getMatchingApplicationId(),
                application.getStatus().name(),
                withdrawalType,
                collaborationPenalty,
                currentCollaborationDistance);
    }
}
