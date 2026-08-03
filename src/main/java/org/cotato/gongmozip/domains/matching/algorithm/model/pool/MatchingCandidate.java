package org.cotato.gongmozip.domains.matching.algorithm.model.pool;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;

/**
 * 매칭 계산을 JPA 엔티티와 분리하기 위해 신청 시점의 알고리즘 입력만 모은 불변 후보 모델이다.
 * 트랜잭션 밖에서도 지연 로딩이나 엔티티 상태 변경 없이 점수 계산과 반복 탐색을 수행할 수 있다.
 */
public record MatchingCandidate(
        Long applicationId,
        Long memberId,
        Long profileId,
        LocalDateTime appliedAt,
        InterestCategory category,
        BigDecimal skillScore,
        LeaderPreference leaderPreference,
        boolean reassignmentPriority,
        BigDecimal goalPreferenceScore,
        BigDecimal workStyleScore,
        BigDecimal communicationStyleScore,
        BigDecimal agreeablenessScore,
        BigDecimal conscientiousnessScore,
        BigDecimal honestyHumilityScore,
        ExtroversionType extroversionType) {

    public MatchingCandidate {
        Objects.requireNonNull(applicationId, "신청 ID는 null일 수 없습니다.");
        Objects.requireNonNull(memberId, "회원 ID는 null일 수 없습니다.");
        Objects.requireNonNull(profileId, "프로필 ID는 null일 수 없습니다.");
        Objects.requireNonNull(appliedAt, "신청 시각은 null일 수 없습니다.");
        Objects.requireNonNull(category, "카테고리는 null일 수 없습니다.");
        Objects.requireNonNull(skillScore, "역량 점수는 null일 수 없습니다.");
        Objects.requireNonNull(leaderPreference, "팀장 선호도는 null일 수 없습니다.");
        Objects.requireNonNull(goalPreferenceScore, "목표 선호도 점수는 null일 수 없습니다.");
        Objects.requireNonNull(workStyleScore, "업무 방식 점수는 null일 수 없습니다.");
        Objects.requireNonNull(communicationStyleScore, "의사소통 방식 점수는 null일 수 없습니다.");
        Objects.requireNonNull(agreeablenessScore, "원만성 점수는 null일 수 없습니다.");
        Objects.requireNonNull(conscientiousnessScore, "성실성 점수는 null일 수 없습니다.");
        Objects.requireNonNull(honestyHumilityScore, "정직·겸손성 점수는 null일 수 없습니다.");
        Objects.requireNonNull(extroversionType, "외향성 유형은 null일 수 없습니다.");
    }

    /** 영속 신청 엔티티에 고정된 스냅샷을 알고리즘용 후보로 변환한다. */
    public static MatchingCandidate from(MatchingApplication application) {
        // 테스트·레거시 행에 createdAt이 없을 때도 결정적인 정렬 기준을 만들기 위한 보정값이다.
        LocalDateTime appliedAt = application.getCreatedAt();
        if (appliedAt == null && application.getApplicationDate() != null) {
            appliedAt = application.getApplicationDate().atStartOfDay();
        }
        return new MatchingCandidate(
                application.getMatchingApplicationId(),
                application.getMember().getMemberId(),
                application.getProfile().getProfileId(),
                appliedAt,
                application.getContestCategory(),
                application.getSkillScore(),
                application.getLeaderPreference(),
                application.isReassignmentPriority(),
                application.getGoalPreferenceScore(),
                application.getWorkStyleScore(),
                application.getCommunicationStyleScore(),
                application.getAgreeablenessScore(),
                application.getConscientiousnessScore(),
                application.getHonestyHumilityScore(),
                application.getExtroversionType());
    }
}
