package org.cotato.gongmozip.domains.matching.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.matching.entity.LeaderRecommendation;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroup;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroupMember;
import org.cotato.gongmozip.domains.matching.entity.MatchingReason;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.matching.enums.MatchingAiStatus;
import org.cotato.gongmozip.domains.matching.repository.LeaderRecommendationRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupMemberRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingReasonRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.global.ai.AiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchingAiWorkerTest {

    @InjectMocks
    private MatchingAiWorker matchingAiWorker;

    @Mock
    private MatchingTxService matchingTxService;

    @Mock
    private LeaderRecommendationRepository leaderRecommendationRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private MatchingReasonRepository matchingReasonRepository;

    @Mock
    private MatchingGroupMemberRepository matchingGroupMemberRepository;

    @Mock
    private AiClient aiClient;

    @Test
    @DisplayName("팀원 성향 점수의 분산을 구하여 상위 일치 성향 Top 2를 식별하고 매칭 사유와 계산된 점수를 저장한다")
    void generateMatchingReasonAsync_Success() {
        // given
        Long reasonId = 1L;
        MatchingGroup group = MatchingGroup.builder()
                .matchingGroupId(10L)
                .matchingScore(new BigDecimal("84.50"))
                .goalSimilarityScore(new BigDecimal("8.50"))
                .workStyleSimilarityScore(new BigDecimal("7.50"))
                .communicationSimilarityScore(new BigDecimal("8.00"))
                .agreeablenessSimilarityScore(new BigDecimal("16.00"))
                .conscientiousnessSimilarityScore(new BigDecimal("8.20"))
                .honestyHumilitySimilarityScore(new BigDecimal("7.80"))
                .extroversionComplementScore(new BigDecimal("15.00"))
                .build();

        MatchingReason reason = MatchingReason.builder()
                .matchingReasonId(reasonId)
                .matchingGroup(group)
                .status(MatchingAiStatus.PENDING)
                .build();

        MatchingApplication app1 = MatchingApplication.builder()
                .matchingApplicationId(101L)
                .goalPreferenceScore(new BigDecimal("4.0"))
                .workStyleScore(new BigDecimal("3.0"))
                .communicationStyleScore(new BigDecimal("4.0"))
                .agreeablenessScore(new BigDecimal("5.0"))
                .conscientiousnessScore(new BigDecimal("4.0"))
                .honestyHumilityScore(new BigDecimal("4.0"))
                .extroversionType(ExtroversionType.E)
                .build();

        MatchingApplication app2 = MatchingApplication.builder()
                .matchingApplicationId(102L)
                .goalPreferenceScore(new BigDecimal("4.0"))
                .workStyleScore(new BigDecimal("3.0"))
                .communicationStyleScore(new BigDecimal("4.0"))
                .agreeablenessScore(new BigDecimal("5.0"))
                .conscientiousnessScore(new BigDecimal("4.0"))
                .honestyHumilityScore(new BigDecimal("4.0"))
                .extroversionType(ExtroversionType.I)
                .build();

        MatchingGroupMember member1 = MatchingGroupMember.builder()
                .matchingGroupMemberId(201L)
                .matchingGroup(group)
                .matchingApplication(app1)
                .build();

        MatchingGroupMember member2 = MatchingGroupMember.builder()
                .matchingGroupMemberId(202L)
                .matchingGroup(group)
                .matchingApplication(app2)
                .build();

        given(matchingReasonRepository.findById(reasonId)).willReturn(Optional.of(reason));
        given(matchingGroupMemberRepository.findAllByMatchingGroup(group)).willReturn(List.of(member1, member2));

        // when
        matchingAiWorker.generateMatchingReasonAsync(reasonId);

        // then
        then(matchingTxService).should().startReasonProcessing(reasonId);
        then(matchingTxService)
                .should()
                .completeReason(
                        eq(reasonId),
                        any(String.class),
                        any(String.class),
                        anyList(),
                        anyList(),
                        anyList(),
                        anyList(),
                        eq(84),
                        eq(85),
                        eq(79),
                        eq(75));
    }

    @Test
    @DisplayName("규칙 기반으로 리더 후보를 추천하고 최종 가점 포함 점수와 사유를 생성하여 저장한다")
    void generateLeaderRecommendationAsync_Success() {
        // given
        Long recId = 1L;
        Team team = Team.builder().teamId(10L).build();
        LeaderRecommendation rec = LeaderRecommendation.builder()
                .leaderRecommendationId(recId)
                .team(team)
                .status(MatchingAiStatus.PENDING)
                .build();

        Member m1 = Member.builder().memberId(1001L).build();
        Member m2 = Member.builder().memberId(1002L).build();

        Profile p1 = Profile.builder().profileId(501L).nickname("길동").build();
        Profile p2 = Profile.builder().profileId(502L).nickname("꺽정").build();

        TeamMember tm1 = TeamMember.builder()
                .teamMemberId(2001L)
                .team(team)
                .member(m1)
                .profile(p1)
                .leaderPreference(LeaderPreference.WANTS)
                .extroversionType(ExtroversionType.E)
                .extroversionScore(new BigDecimal("4.5"))
                .build();

        TeamMember tm2 = TeamMember.builder()
                .teamMemberId(2002L)
                .team(team)
                .member(m2)
                .profile(p2)
                .leaderPreference(LeaderPreference.NEUTRAL)
                .extroversionType(ExtroversionType.I)
                .extroversionScore(new BigDecimal("3.0"))
                .build();

        given(leaderRecommendationRepository.findById(recId)).willReturn(Optional.of(rec));
        given(teamMemberRepository.findByTeamIdAndStatus(team.getTeamId(), TeamMemberStatus.ACTIVE))
                .willReturn(List.of(tm1, tm2));
        given(aiClient.recommendLeaderCandidates(eq(team.getTeamId()), anyList()))
                .willReturn(List.of(2001L, 2002L));
        given(aiClient.finalScore(any(), anyList())).willReturn(10);

        // when
        matchingAiWorker.generateLeaderRecommendationAsync(recId);

        // then
        then(matchingTxService).should().startLeaderRecProcessing(recId);
        then(matchingTxService)
                .should()
                .completeLeaderRec(
                        eq(recId), eq(1001L), any(String.class), anyList(), any(String.class), any(String.class));
    }
}
