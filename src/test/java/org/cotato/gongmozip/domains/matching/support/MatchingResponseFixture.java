package org.cotato.gongmozip.domains.matching.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.entity.MatchingBatch;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroup;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroupMember;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupMemberStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupStatus;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;

public final class MatchingResponseFixture {

    public static final LocalDate APPLICATION_DATE = LocalDate.of(2026, 8, 4);
    public static final LocalDateTime PUBLISHED_AT = APPLICATION_DATE.atTime(16, 0);
    public static final LocalDateTime DEADLINE_AT = APPLICATION_DATE.plusDays(1).atTime(12, 0);

    private MatchingResponseFixture() {}

    public static MatchingBatch batch() {
        return MatchingBatch.builder()
                .matchingBatchId(10L)
                .applicationDate(APPLICATION_DATE)
                .category(InterestCategory.IT_AI_TECH)
                .poolOrdinal(1)
                .build();
    }

    public static MatchingGroup group(Long groupId, int teamSize) {
        return MatchingGroup.builder()
                .matchingGroupId(groupId)
                .matchingBatch(batch())
                .category(InterestCategory.IT_AI_TECH)
                .skillGroup(1)
                .teamSize(teamSize)
                .matchingScore(new BigDecimal("80.00"))
                .status(MatchingGroupStatus.PROPOSED)
                .responseDeadlineAt(DEADLINE_AT)
                .build();
    }

    public static MatchingGroupMember groupMember(
            Long groupMemberId, MatchingGroup group, Long memberId, MatchingGroupMemberStatus responseStatus) {
        Member member = Member.builder()
                .memberId(memberId)
                .email("member" + memberId + "@example.com")
                .collaborationPoint(100)
                .build();
        Profile profile = Profile.builder()
                .profileId(memberId * 10)
                .member(member)
                .nickname("member-" + memberId)
                .build();
        MatchingApplication application = MatchingApplication.builder()
                .matchingApplicationId(memberId * 100)
                .member(member)
                .profile(profile)
                .matchingBatch(group.getMatchingBatch())
                .applicationDate(APPLICATION_DATE)
                .status(MatchingApplicationStatus.PROPOSED)
                .leaderPreference(LeaderPreference.NEUTRAL)
                .firstMatching(false)
                .contestCategory(InterestCategory.IT_AI_TECH)
                .gpaScore(score())
                .projectScore(score())
                .awardScore(score())
                .certificationScore(score())
                .collaborationScore(score())
                .skillScore(score())
                .collaborationDistance(100)
                .agreeablenessScore(score())
                .conscientiousnessScore(score())
                .honestyHumilityScore(score())
                .extroversionScore(score())
                .goalPreferenceScore(score())
                .workStyleScore(score())
                .communicationStyleScore(score())
                .extroversion2Score(score())
                .extroversion3Score(score())
                .extroversionType(ExtroversionType.A)
                .characterType(CharacterType.TRACK_RUNNER)
                .characterXScore(score())
                .characterYScore(score())
                .build();
        return MatchingGroupMember.builder()
                .matchingGroupMemberId(groupMemberId)
                .matchingGroup(group)
                .matchingApplication(application)
                .member(member)
                .responseStatus(responseStatus)
                .build();
    }

    private static BigDecimal score() {
        return new BigDecimal("50.00");
    }
}
