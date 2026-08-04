package org.cotato.gongmozip.domains.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.entity.MatchingBatch;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroup;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroupMember;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupMemberStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingResultStatus;
import org.cotato.gongmozip.domains.matching.repository.MatchingApplicationRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupMemberRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchingResultQueryServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 3);
    private static final LocalDateTime PUBLISHED_AT = TODAY.atTime(16, 0);

    @Mock
    private MatchingApplicationRepository matchingApplicationRepository;

    @Mock
    private MatchingGroupMemberRepository matchingGroupMemberRepository;

    @Mock
    private MatchingTimePolicy matchingTimePolicy;

    private MatchingResultQueryService matchingResultQueryService;

    @BeforeEach
    void setUp() {
        matchingResultQueryService = new MatchingResultQueryService(
                matchingApplicationRepository, matchingGroupMemberRepository, matchingTimePolicy);
        given(matchingTimePolicy.today()).willReturn(TODAY);
    }

    @DisplayName("오늘 신청하지 않은 회원은 NOT_APPLIED와 빈 결과를 받는다")
    @Test
    void returnsNotAppliedWhenTodayApplicationDoesNotExist() {
        given(matchingApplicationRepository.findResultApplication(1L, TODAY)).willReturn(Optional.empty());

        var response = matchingResultQueryService.getTodayResult(1L);

        assertThat(response.resultStatus()).isEqualTo(MatchingResultStatus.NOT_APPLIED);
        assertThat(response.applicationId()).isNull();
        assertThat(response.members()).isEmpty();
        verify(matchingGroupMemberRepository, never()).findResultMembership(org.mockito.ArgumentMatchers.any());
    }

    @DisplayName("계산이 끝나도 공개 시각 전에는 그룹과 팀원을 조회하지 않는다")
    @Test
    void hidesMatchedResultBeforePublishedAt() {
        MatchingApplication application = application(1L, MatchingApplicationStatus.PROPOSED, batch());
        given(matchingApplicationRepository.findResultApplication(1L, TODAY)).willReturn(Optional.of(application));
        given(matchingTimePolicy.now()).willReturn(PUBLISHED_AT.minusSeconds(1));
        given(matchingTimePolicy.resultPublishAt(TODAY)).willReturn(PUBLISHED_AT);
        given(matchingTimePolicy.isResultPublished(TODAY, PUBLISHED_AT.minusSeconds(1)))
                .willReturn(false);

        var response = matchingResultQueryService.getTodayResult(1L);

        assertThat(response.resultStatus()).isEqualTo(MatchingResultStatus.NOT_PUBLISHED);
        assertThat(response.publishedAt()).isEqualTo(PUBLISHED_AT);
        assertThat(response.matchingGroupId()).isNull();
        assertThat(response.members()).isEmpty();
        verify(matchingGroupMemberRepository, never()).findResultMembership(application);
    }

    @DisplayName("공개 시각 이후 배정자는 저장된 그룹 점수와 3명의 팀원을 조회한다")
    @Test
    void returnsMatchedGroupAfterPublishedAt() {
        MatchingBatch batch = batch();
        MatchingApplication mine = application(1L, MatchingApplicationStatus.PROPOSED, batch);
        MatchingApplication teammateTwo = application(2L, MatchingApplicationStatus.PROPOSED, batch);
        MatchingApplication teammateThree = application(3L, MatchingApplicationStatus.PROPOSED, batch);
        MatchingGroup group = group(batch);
        MatchingGroupMember myMembership = groupMember(11L, group, mine);
        List<MatchingGroupMember> members =
                List.of(myMembership, groupMember(12L, group, teammateTwo), groupMember(13L, group, teammateThree));

        given(matchingApplicationRepository.findResultApplication(1L, TODAY)).willReturn(Optional.of(mine));
        given(matchingTimePolicy.now()).willReturn(PUBLISHED_AT);
        given(matchingTimePolicy.resultPublishAt(TODAY)).willReturn(PUBLISHED_AT);
        given(matchingTimePolicy.isResultPublished(TODAY, PUBLISHED_AT)).willReturn(true);
        given(matchingGroupMemberRepository.findResultMembership(mine)).willReturn(Optional.of(myMembership));
        given(matchingGroupMemberRepository.findResultMembers(group)).willReturn(members);

        var response = matchingResultQueryService.getTodayResult(1L);

        assertThat(response.resultStatus()).isEqualTo(MatchingResultStatus.MATCHED);
        assertThat(response.matchingGroupId()).isEqualTo(20L);
        assertThat(response.teamSize()).isEqualTo(3);
        assertThat(response.matchingScore()).isEqualByComparingTo("80.00");
        assertThat(response.scoreBreakdown().leaderHarmonyScore()).isEqualByComparingTo("10.00");
        assertThat(response.members())
                .hasSize(3)
                .extracting(member -> member.memberId())
                .containsExactly(1L, 2L, 3L);
        assertThat(response.members())
                .filteredOn(member -> member.me())
                .singleElement()
                .satisfies(member -> {
                    assertThat(member.memberId()).isEqualTo(1L);
                    assertThat(member.nickname()).isEqualTo("nickname-1");
                });
    }

    @DisplayName("공개 시각 이후 미배정 신청은 팀원 정보 없이 UNMATCHED를 반환한다")
    @Test
    void returnsUnmatchedWithoutGroupLookup() {
        MatchingApplication application = application(1L, MatchingApplicationStatus.FAILED, batch());
        given(matchingApplicationRepository.findResultApplication(1L, TODAY)).willReturn(Optional.of(application));
        given(matchingTimePolicy.now()).willReturn(PUBLISHED_AT.plusMinutes(1));
        given(matchingTimePolicy.resultPublishAt(TODAY)).willReturn(PUBLISHED_AT);
        given(matchingTimePolicy.isResultPublished(TODAY, PUBLISHED_AT.plusMinutes(1)))
                .willReturn(true);

        var response = matchingResultQueryService.getTodayResult(1L);

        assertThat(response.resultStatus()).isEqualTo(MatchingResultStatus.UNMATCHED);
        assertThat(response.applicationStatus()).isEqualTo(MatchingApplicationStatus.FAILED);
        assertThat(response.members()).isEmpty();
        verify(matchingGroupMemberRepository, never()).findResultMembership(application);
    }

    @DisplayName("공개 시각이 지났지만 배치가 끝나지 않았으면 PROCESSING을 반환한다")
    @Test
    void returnsProcessingWhenBatchIsNotFinished() {
        MatchingApplication application = application(1L, MatchingApplicationStatus.MATCHING, batch());
        given(matchingApplicationRepository.findResultApplication(1L, TODAY)).willReturn(Optional.of(application));
        given(matchingTimePolicy.now()).willReturn(PUBLISHED_AT.plusMinutes(1));
        given(matchingTimePolicy.resultPublishAt(TODAY)).willReturn(PUBLISHED_AT);
        given(matchingTimePolicy.isResultPublished(TODAY, PUBLISHED_AT.plusMinutes(1)))
                .willReturn(true);

        var response = matchingResultQueryService.getTodayResult(1L);

        assertThat(response.resultStatus()).isEqualTo(MatchingResultStatus.PROCESSING);
        assertThat(response.members()).isEmpty();
        verify(matchingGroupMemberRepository, never()).findResultMembership(application);
    }

    @DisplayName("무료 취소 신청은 공개 시각과 무관하게 WITHDRAWN을 반환한다")
    @Test
    void returnsWithdrawnForCanceledApplication() {
        MatchingApplication application = application(1L, MatchingApplicationStatus.CANCELED, null);
        given(matchingApplicationRepository.findResultApplication(1L, TODAY)).willReturn(Optional.of(application));
        given(matchingTimePolicy.resultPublishAt(TODAY)).willReturn(PUBLISHED_AT);

        var response = matchingResultQueryService.getTodayResult(1L);

        assertThat(response.resultStatus()).isEqualTo(MatchingResultStatus.WITHDRAWN);
        assertThat(response.publishedAt()).isEqualTo(PUBLISHED_AT);
        verify(matchingTimePolicy, never()).now();
    }

    private MatchingBatch batch() {
        return MatchingBatch.builder()
                .matchingBatchId(10L)
                .applicationDate(TODAY)
                .category(InterestCategory.IT_AI_TECH)
                .poolOrdinal(1)
                .build();
    }

    private MatchingApplication application(
            Long memberId, MatchingApplicationStatus status, MatchingBatch matchingBatch) {
        Member member = Member.builder().memberId(memberId).build();
        Profile profile = Profile.builder()
                .profileId(memberId * 10)
                .member(member)
                .nickname("nickname-" + memberId)
                .build();
        return MatchingApplication.builder()
                .matchingApplicationId(memberId * 100)
                .member(member)
                .profile(profile)
                .matchingBatch(matchingBatch)
                .applicationDate(TODAY)
                .status(status)
                .leaderPreference(memberId == 1 ? LeaderPreference.WANTS : LeaderPreference.NEUTRAL)
                .contestCategory(InterestCategory.IT_AI_TECH)
                .characterType(CharacterType.LEAD_RUNNER)
                .build();
    }

    private MatchingGroup group(MatchingBatch batch) {
        return MatchingGroup.builder()
                .matchingGroupId(20L)
                .matchingBatch(batch)
                .category(InterestCategory.IT_AI_TECH)
                .skillGroup(1)
                .teamSize(3)
                .matchingScore(score("80.00"))
                .leaderHarmonyScore(score("10.00"))
                .goalSimilarityScore(score("10.00"))
                .workStyleSimilarityScore(score("10.00"))
                .communicationSimilarityScore(score("10.00"))
                .agreeablenessSimilarityScore(score("10.00"))
                .conscientiousnessSimilarityScore(score("10.00"))
                .honestyHumilitySimilarityScore(score("10.00"))
                .extroversionComplementScore(score("10.00"))
                .build();
    }

    private MatchingGroupMember groupMember(Long groupMemberId, MatchingGroup group, MatchingApplication application) {
        return MatchingGroupMember.builder()
                .matchingGroupMemberId(groupMemberId)
                .matchingGroup(group)
                .matchingApplication(application)
                .member(application.getMember())
                .responseStatus(MatchingGroupMemberStatus.PENDING)
                .build();
    }

    private BigDecimal score(String value) {
        return new BigDecimal(value);
    }
}
