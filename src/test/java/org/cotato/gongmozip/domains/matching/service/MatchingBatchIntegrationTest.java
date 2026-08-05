package org.cotato.gongmozip.domains.matching.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.cotato.gongmozip.domains.matching.algorithm.MatchingAlgorithmSelector;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingPoolInput;
import org.cotato.gongmozip.domains.matching.algorithm.model.result.MatchingPlan;
import org.cotato.gongmozip.domains.matching.config.MatchingAlgorithmProperties;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingBatchStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupingMode;
import org.cotato.gongmozip.domains.matching.enums.MatchingResultStatus;
import org.cotato.gongmozip.domains.matching.repository.MatchingApplicationRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingBatchRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupMemberRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.enums.MemberStatus;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.profile.repository.ProfileRepository;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MatchingBatchIntegrationTest {

    @Autowired
    private MatchingBatchClaimService claimService;

    @Autowired
    private MatchingPoolPreparationService preparationService;

    @Autowired
    private MatchingAlgorithmSelector algorithmSelector;

    @Autowired
    private MatchingResultPersistenceService persistenceService;

    @Autowired
    private MatchingBatchRepository batchRepository;

    @Autowired
    private MatchingApplicationRepository applicationRepository;

    @Autowired
    private MatchingGroupRepository groupRepository;

    @Autowired
    private MatchingGroupMemberRepository groupMemberRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private Clock clock;

    @AfterEach
    void cleanUp() {
        groupMemberRepository.deleteAll();
        groupRepository.deleteAll();
        applicationRepository.deleteAll();
        batchRepository.deleteAll();
        profileRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("신청자 4명의 풀을 선점하고 한 팀으로 매칭해 PROPOSED 상태로 저장한다")
    void claimedPoolIsPersistedAsOneFourPersonProposal() {
        LocalDate applicationDate = LocalDate.now(clock);
        List<MatchingApplication> applications = new ArrayList<>();
        Member resultMember = null;
        for (int index = 1; index <= 4; index++) {
            Member member = memberRepository.save(Member.builder()
                    .email("matching-batch-" + index + "@gongmozip.com")
                    .password("password")
                    .status(MemberStatus.ACTIVE)
                    .build());
            Profile profile = profileRepository.save(Profile.builder()
                    .member(member)
                    .nickname("매칭" + index)
                    .schoolName("학교")
                    .grade(3)
                    .major("전공")
                    .gpa(4.0)
                    .gpaScale(4.5)
                    .interestCategories(List.of(InterestCategory.IT_AI_TECH))
                    .isPublic(true)
                    .build());
            applications.add(applicationRepository.save(application(applicationDate, member, profile, index)));
            if (index == 1) {
                resultMember = member;
            }
        }

        preparationService.prepare(applicationDate);
        Long batchId = batchRepository
                .findProcessableIds(applicationDate, List.of(MatchingBatchStatus.PENDING))
                .getFirst();
        var claimed = claimService.claim(batchId).orElseThrow();
        MatchingPlan plan = algorithmSelector.match(new MatchingPoolInput(
                applicationDate,
                InterestCategory.IT_AI_TECH,
                claimed.poolKey().poolOrdinal(),
                claimed.candidates(),
                claimed.teamSizes(),
                claimed.seed(),
                clock.instant().plusSeconds(30)));
        persistenceService.persistSuccess(claimed.batchId(), plan);

        var batch = batchRepository.findById(claimed.batchId()).orElseThrow();
        assertThat(batch.getStatus()).isEqualTo(MatchingBatchStatus.SUCCEEDED);
        assertThat(batch.getGroupingMode()).isEqualTo(MatchingGroupingMode.CATEGORY_ONLY);
        assertThat(batch.getSourceQuartileFrom()).isEqualTo(1);
        assertThat(batch.getSourceQuartileTo()).isEqualTo(4);
        assertThat(groupRepository.findAllByMatchingBatch(batch)).hasSize(1);
        assertThat(applicationRepository.findAllByMatchingBatch(batch))
                .allMatch(application -> application.getStatus() == MatchingApplicationStatus.PROPOSED);
        var group = groupRepository.findAllByMatchingBatch(batch).getFirst();
        assertThat(group.getTeamSize()).isEqualTo(4);
        assertThat(groupMemberRepository.findAllByMatchingGroup(group))
                .hasSize(4)
                .allMatch(member -> member.getMatchingApplication() != null);

        Clock afterPublishClock = Clock.fixed(
                applicationDate.atTime(16, 1).atZone(clock.getZone()).toInstant(), clock.getZone());
        MatchingAlgorithmProperties properties = new MatchingAlgorithmProperties();
        properties.setResultPublishTime(LocalTime.of(16, 0));
        MatchingResultQueryService resultQueryService = new MatchingResultQueryService(
                applicationRepository, groupMemberRepository, new MatchingTimePolicy(afterPublishClock, properties));
        var result = resultQueryService.getTodayResult(resultMember.getMemberId());

        assertThat(result.resultStatus()).isEqualTo(MatchingResultStatus.MATCHED);
        assertThat(result.matchingGroupId()).isEqualTo(group.getMatchingGroupId());
        assertThat(result.members()).hasSize(4).anyMatch(member -> member.me());
    }

    @Test
    @DisplayName("신청자 3명은 한 개의 3인 제안 팀으로 저장한다")
    void claimedPoolIsPersistedAsOneThreePersonProposal() {
        LocalDate applicationDate = LocalDate.now(clock);
        for (int index = 1; index <= 3; index++) {
            Member member = memberRepository.save(Member.builder()
                    .email("matching-batch-three-" + index + "@gongmozip.com")
                    .password("password")
                    .status(MemberStatus.ACTIVE)
                    .build());
            Profile profile = profileRepository.save(Profile.builder()
                    .member(member)
                    .nickname("3인매칭" + index)
                    .schoolName("학교")
                    .grade(3)
                    .major("전공")
                    .gpa(4.0)
                    .gpaScale(4.5)
                    .interestCategories(List.of(InterestCategory.IT_AI_TECH))
                    .isPublic(true)
                    .build());
            applicationRepository.save(application(applicationDate, member, profile, index));
        }

        preparationService.prepare(applicationDate);
        Long batchId = batchRepository
                .findProcessableIds(applicationDate, List.of(MatchingBatchStatus.PENDING))
                .getFirst();
        var claimed = claimService.claim(batchId).orElseThrow();
        MatchingPlan plan = algorithmSelector.match(new MatchingPoolInput(
                applicationDate,
                claimed.poolKey().category(),
                claimed.poolKey().poolOrdinal(),
                claimed.candidates(),
                claimed.teamSizes(),
                claimed.seed(),
                clock.instant().plusSeconds(30)));
        persistenceService.persistSuccess(claimed.batchId(), plan);

        var batch = batchRepository.findById(batchId).orElseThrow();
        var group = groupRepository.findAllByMatchingBatch(batch).getFirst();
        assertThat(group.getTeamSize()).isEqualTo(3);
        assertThat(groupMemberRepository.findAllByMatchingGroup(group)).hasSize(3);
    }

    @Test
    @DisplayName("실패한 배치는 같은 풀과 시드로 다시 선점한다")
    void failedBatchIsClaimedAgainWithSameInputAndSeed() {
        LocalDate applicationDate = LocalDate.now(clock);
        for (int index = 1; index <= 3; index++) {
            Member member = memberRepository.save(Member.builder()
                    .email("matching-batch-retry-" + index + "@gongmozip.com")
                    .password("password")
                    .status(MemberStatus.ACTIVE)
                    .build());
            Profile profile = profileRepository.save(Profile.builder()
                    .member(member)
                    .nickname("retry" + index)
                    .schoolName("school")
                    .grade(3)
                    .major("major")
                    .gpa(4.0)
                    .gpaScale(4.5)
                    .interestCategories(List.of(InterestCategory.IT_AI_TECH))
                    .isPublic(true)
                    .build());
            applicationRepository.save(application(applicationDate, member, profile, index));
        }

        preparationService.prepare(applicationDate);
        Long batchId = batchRepository
                .findProcessableIds(applicationDate, List.of(MatchingBatchStatus.PENDING))
                .getFirst();
        var first = claimService.claim(batchId).orElseThrow();
        persistenceService.persistFailure(batchId, new IllegalStateException("synthetic failure"));
        var retried = claimService.claim(batchId).orElseThrow();

        assertThat(retried.batchId()).isEqualTo(first.batchId());
        assertThat(retried.poolKey()).isEqualTo(first.poolKey());
        assertThat(retried.seed()).isEqualTo(first.seed());
        assertThat(retried.candidates())
                .extracting(candidate -> candidate.applicationId())
                .containsExactlyElementsOf(first.candidates().stream()
                        .map(candidate -> candidate.applicationId())
                        .toList());
        assertThat(batchRepository.findById(batchId).orElseThrow().getRetryCount())
                .isEqualTo(1);
    }

    private MatchingApplication application(LocalDate date, Member member, Profile profile, int index) {
        BigDecimal score = BigDecimal.valueOf(2 + index * 0.5).setScale(2);
        return MatchingApplication.builder()
                .member(member)
                .profile(profile)
                .applicationDate(date)
                .status(MatchingApplicationStatus.WAITING)
                .leaderPreference(index == 1 ? LeaderPreference.WANTS : LeaderPreference.DOES_NOT_WANT)
                .firstMatching(true)
                .contestCategory(InterestCategory.IT_AI_TECH)
                .gpaScore(BigDecimal.valueOf(80).setScale(2))
                .projectScore(BigDecimal.valueOf(70).setScale(2))
                .awardScore(BigDecimal.ZERO.setScale(2))
                .certificationScore(BigDecimal.ZERO.setScale(2))
                .collaborationScore(BigDecimal.valueOf(20).setScale(2))
                .skillScore(BigDecimal.valueOf(50).setScale(2))
                .collaborationDistance(100)
                .agreeablenessScore(score)
                .conscientiousnessScore(score)
                .honestyHumilityScore(score)
                .extroversionScore(score)
                .goalPreferenceScore(score)
                .workStyleScore(score)
                .communicationStyleScore(score)
                .extroversion2Score(score)
                .extroversion3Score(score)
                .extroversionType(index == 1 ? ExtroversionType.E : ExtroversionType.I)
                .characterType(CharacterType.LEAD_RUNNER)
                .characterXScore(score)
                .characterYScore(score)
                .build();
    }
}
