package org.cotato.gongmozip.domains.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.cotato.gongmozip.domains.chatbot.service.ChatbotOrchestrationService;
import org.cotato.gongmozip.domains.collaboration.enums.CollaborationPointReason;
import org.cotato.gongmozip.domains.collaboration.repository.CollaborationPointHistoryRepository;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.entity.MatchingBatch;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroup;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroupMember;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingBatchStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupMemberStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupingMode;
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
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class MatchingResponseIntegrationTest {

    private static final LocalDate APPLICATION_DATE = LocalDate.of(2026, 8, 4);
    private static final LocalDateTime PUBLISHED_AT = APPLICATION_DATE.atTime(16, 0);
    private static final LocalDateTime RESPONSE_TIME = PUBLISHED_AT.plusMinutes(30);
    private static final LocalDateTime DEADLINE_AT =
            APPLICATION_DATE.plusDays(1).atTime(12, 0);

    @Autowired
    private MatchingResponseService matchingResponseService;

    @Autowired
    private MatchingApplicationRepository applicationRepository;

    @Autowired
    private MatchingBatchRepository batchRepository;

    @Autowired
    private MatchingGroupRepository groupRepository;

    @Autowired
    private MatchingGroupMemberRepository groupMemberRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private CollaborationPointHistoryRepository collaborationPointHistoryRepository;

    @MockitoBean
    private MatchingTimePolicy matchingTimePolicy;

    @MockitoBean
    private ChatbotOrchestrationService chatbotOrchestrationService;

    @AfterEach
    void cleanUp() {
        collaborationPointHistoryRepository.deleteAll();
        groupMemberRepository.deleteAll();
        groupRepository.deleteAll();
        applicationRepository.deleteAll();
        batchRepository.deleteAll();
        teamMemberRepository.deleteAll();
        teamRepository.deleteAll();
        profileRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void threeAndFourMemberGroupsCreateExactlyOneTeamAndStartGreeting() {
        given(matchingTimePolicy.now()).willReturn(RESPONSE_TIME);
        given(matchingTimePolicy.isResultPublished(APPLICATION_DATE, RESPONSE_TIME))
                .willReturn(true);
        Proposal threePerson = createProposal(3, 1);
        Proposal fourPerson = createProposal(4, 2);

        acceptAll(threePerson);
        Long threePersonTeamId = groupRepository
                .findById(threePerson.groupId())
                .orElseThrow()
                .getTeam()
                .getTeamId();
        acceptAll(fourPerson);
        Long fourPersonTeamId = groupRepository
                .findById(fourPerson.groupId())
                .orElseThrow()
                .getTeam()
                .getTeamId();

        assertThat(teamRepository.count()).isEqualTo(2);
        assertThat(teamMemberRepository.findAll())
                .filteredOn(member -> member.getTeam().getTeamId().equals(threePersonTeamId))
                .hasSize(3);
        assertThat(teamMemberRepository.findAll())
                .filteredOn(member -> member.getTeam().getTeamId().equals(fourPersonTeamId))
                .hasSize(4);
        assertThat(groupRepository.findById(threePerson.groupId()).orElseThrow().getConfirmedTeamSize())
                .isEqualTo(3);
        assertThat(groupRepository.findById(fourPerson.groupId()).orElseThrow().getConfirmedTeamSize())
                .isEqualTo(4);
        verify(chatbotOrchestrationService, times(2)).startGreeting(any(Team.class));

        var repeated = matchingResponseService.accept(threePerson.memberIds().getLast(), threePerson.groupId());
        assertThat(repeated.teamId()).isEqualTo(threePersonTeamId);
        assertThat(teamRepository.count()).isEqualTo(2);
        verify(chatbotOrchestrationService, times(2)).startGreeting(any(Team.class));
    }

    @Test
    void teamCreationFailureRollsBackLastAcceptanceAndAllTeamWrites() {
        given(matchingTimePolicy.now()).willReturn(RESPONSE_TIME);
        given(matchingTimePolicy.isResultPublished(APPLICATION_DATE, RESPONSE_TIME))
                .willReturn(true);
        Proposal proposal = createProposal(3, 1);
        matchingResponseService.accept(proposal.memberIds().get(0), proposal.groupId());
        matchingResponseService.accept(proposal.memberIds().get(1), proposal.groupId());
        doThrow(new IllegalStateException("greeting failure"))
                .when(chatbotOrchestrationService)
                .startGreeting(any(Team.class));

        assertThatThrownBy(() ->
                        matchingResponseService.accept(proposal.memberIds().get(2), proposal.groupId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("greeting failure");

        MatchingGroup savedGroup = groupRepository.findById(proposal.groupId()).orElseThrow();
        MatchingGroupMember lastMember = groupMemberRepository
                .findByMatchingGroup_MatchingGroupIdAndMember_MemberId(
                        proposal.groupId(), proposal.memberIds().get(2))
                .orElseThrow();
        assertThat(savedGroup.getStatus()).isEqualTo(MatchingGroupStatus.PROPOSED);
        assertThat(savedGroup.getTeam()).isNull();
        assertThat(lastMember.getResponseStatus()).isEqualTo(MatchingGroupMemberStatus.PENDING);
        assertThat(applicationRepository.findAllById(proposal.applicationIds()))
                .allMatch(application -> application.getStatus() == MatchingApplicationStatus.PROPOSED);
        assertThat(teamRepository.count()).isZero();
        assertThat(teamMemberRepository.count()).isZero();
    }

    @Test
    void concurrentAcceptAndPassProduceExactlyOneResponseAndAtMostOnePenalty() throws Exception {
        given(matchingTimePolicy.now()).willReturn(RESPONSE_TIME);
        given(matchingTimePolicy.isResultPublished(APPLICATION_DATE, RESPONSE_TIME))
                .willReturn(true);
        Proposal proposal = createProposal(4, 1);
        Long memberId = proposal.memberIds().getFirst();
        Long applicationId = proposal.applicationIds().getFirst();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Throwable> accept = executor.submit(() -> executeConcurrently(
                    ready, start, () -> matchingResponseService.accept(memberId, proposal.groupId())));
            Future<Throwable> pass = executor.submit(() ->
                    executeConcurrently(ready, start, () -> matchingResponseService.pass(memberId, applicationId)));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Throwable> failures = Arrays.asList(accept.get(10, TimeUnit.SECONDS), pass.get(10, TimeUnit.SECONDS));
            assertThat(failures).filteredOn(failure -> failure != null).hasSize(1);
        } finally {
            executor.shutdownNow();
        }

        MatchingGroupMember savedMember = groupMemberRepository
                .findByMatchingGroup_MatchingGroupIdAndMember_MemberId(proposal.groupId(), memberId)
                .orElseThrow();
        long penaltyCount = collaborationPointHistoryRepository.findAll().stream()
                .filter(history -> history.getMember().getMemberId().equals(memberId))
                .filter(history -> history.getReasonCode() == CollaborationPointReason.MATCHING_PASS_PENALTY)
                .count();
        assertThat(savedMember.getResponseStatus())
                .isIn(MatchingGroupMemberStatus.ACCEPTED, MatchingGroupMemberStatus.PASSED);
        assertThat(penaltyCount).isEqualTo(savedMember.getResponseStatus() == MatchingGroupMemberStatus.PASSED ? 1 : 0);
    }

    private Throwable executeConcurrently(CountDownLatch ready, CountDownLatch start, ThrowingOperation operation) {
        ready.countDown();
        try {
            start.await(5, TimeUnit.SECONDS);
            operation.run();
            return null;
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    private void acceptAll(Proposal proposal) {
        for (Long memberId : proposal.memberIds()) {
            matchingResponseService.accept(memberId, proposal.groupId());
        }
    }

    private Proposal createProposal(int teamSize, int poolOrdinal) {
        MatchingBatch batch = batchRepository.save(MatchingBatch.builder()
                .applicationDate(APPLICATION_DATE)
                .category(InterestCategory.IT_AI_TECH)
                .poolOrdinal(poolOrdinal)
                .groupingMode(MatchingGroupingMode.CATEGORY_ONLY)
                .sourceQuartileFrom(poolOrdinal)
                .sourceQuartileTo(poolOrdinal)
                .status(MatchingBatchStatus.SUCCEEDED)
                .randomSeed(poolOrdinal)
                .build());
        MatchingGroup group = groupRepository.save(MatchingGroup.builder()
                .matchingBatch(batch)
                .category(InterestCategory.IT_AI_TECH)
                .skillGroup(poolOrdinal)
                .teamSize(teamSize)
                .matchingScore(new BigDecimal("80.00"))
                .status(MatchingGroupStatus.PROPOSED)
                .responseDeadlineAt(DEADLINE_AT)
                .build());
        List<Long> memberIds = new ArrayList<>();
        List<Long> applicationIds = new ArrayList<>();
        for (int index = 1; index <= teamSize; index++) {
            int uniqueIndex = poolOrdinal * 10 + index;
            Member member = memberRepository.save(Member.builder()
                    .email("matching-response-" + uniqueIndex + "@example.com")
                    .password("password")
                    .status(MemberStatus.ACTIVE)
                    .build());
            Profile profile = profileRepository.save(Profile.builder()
                    .member(member)
                    .nickname("member-" + uniqueIndex)
                    .schoolName("school")
                    .grade(3)
                    .major("major")
                    .gpa(4.0)
                    .gpaScale(4.5)
                    .interestCategories(List.of(InterestCategory.IT_AI_TECH))
                    .isPublic(true)
                    .build());
            MatchingApplication application = applicationRepository.save(application(batch, member, profile, index));
            groupMemberRepository.save(MatchingGroupMember.builder()
                    .matchingGroup(group)
                    .matchingApplication(application)
                    .member(member)
                    .responseStatus(MatchingGroupMemberStatus.PENDING)
                    .build());
            memberIds.add(member.getMemberId());
            applicationIds.add(application.getMatchingApplicationId());
        }
        return new Proposal(group.getMatchingGroupId(), List.copyOf(memberIds), List.copyOf(applicationIds));
    }

    private MatchingApplication application(MatchingBatch batch, Member member, Profile profile, int index) {
        BigDecimal score = BigDecimal.valueOf(50 + index).setScale(2);
        return MatchingApplication.builder()
                .member(member)
                .profile(profile)
                .matchingBatch(batch)
                .applicationDate(APPLICATION_DATE)
                .status(MatchingApplicationStatus.PROPOSED)
                .leaderPreference(LeaderPreference.NEUTRAL)
                .firstMatching(false)
                .contestCategory(InterestCategory.IT_AI_TECH)
                .gpaScore(score)
                .projectScore(score)
                .awardScore(score)
                .certificationScore(score)
                .collaborationScore(score)
                .skillScore(score)
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
                .extroversionType(ExtroversionType.A)
                .characterType(CharacterType.TRACK_RUNNER)
                .characterXScore(score)
                .characterYScore(score)
                .build();
    }

    private record Proposal(Long groupId, List<Long> memberIds, List<Long> applicationIds) {}

    @FunctionalInterface
    private interface ThrowingOperation {
        void run();
    }
}
