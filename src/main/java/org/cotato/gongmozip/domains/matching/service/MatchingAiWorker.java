package org.cotato.gongmozip.domains.matching.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.LeaderCandidateResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.TitleDescriptionInfo;
import org.cotato.gongmozip.domains.matching.entity.LeaderRecommendation;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroup;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroupMember;
import org.cotato.gongmozip.domains.matching.entity.MatchingReason;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.matching.repository.LeaderRecommendationRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupMemberRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingReasonRepository;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.global.ai.MockAiClient;
import org.cotato.gongmozip.global.ai.dto.LeaderCandidateSnapshot;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingAiWorker {

    private final MatchingTxService matchingTxService;
    private final LeaderRecommendationRepository leaderRecommendationRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MatchingReasonRepository matchingReasonRepository;
    private final MatchingGroupMemberRepository matchingGroupMemberRepository;
    private final org.cotato.gongmozip.global.ai.AiClient aiClient;

    @Async("aiSummaryExecutor")
    public void generateMatchingReasonAsync(Long reasonId) {
        log.info("Starting async matching reason generation for reasonId: {}", reasonId);

        try {
            matchingTxService.startReasonProcessing(reasonId);
        } catch (Exception e) {
            log.error("Failed to start matching reason processing for reasonId: {}", reasonId, e);
            return;
        }

        try {
            MatchingReason reason = matchingReasonRepository
                    .findById(reasonId)
                    .orElseThrow(() -> new IllegalArgumentException("MatchingReason not found: " + reasonId));
            MatchingGroup group = reason.getMatchingGroup();

            List<MatchingGroupMember> groupMembers = matchingGroupMemberRepository.findAllByMatchingGroup(group);
            if (groupMembers.isEmpty()) {
                throw new IllegalStateException("No members found in matching group: " + group.getMatchingGroupId());
            }

            List<MatchingApplication> apps = groupMembers.stream()
                    .map(MatchingGroupMember::getMatchingApplication)
                    .toList();

            // Calculate variance for 6 dimensions
            List<BigDecimal> goalValues = apps.stream()
                    .map(MatchingApplication::getGoalPreferenceScore)
                    .toList();
            List<BigDecimal> workStyleValues =
                    apps.stream().map(MatchingApplication::getWorkStyleScore).toList();
            List<BigDecimal> commValues = apps.stream()
                    .map(MatchingApplication::getCommunicationStyleScore)
                    .toList();
            List<BigDecimal> agreeValues = apps.stream()
                    .map(MatchingApplication::getAgreeablenessScore)
                    .toList();
            List<BigDecimal> conscientValues = apps.stream()
                    .map(MatchingApplication::getConscientiousnessScore)
                    .toList();
            List<BigDecimal> honestyValues = apps.stream()
                    .map(MatchingApplication::getHonestyHumilityScore)
                    .toList();

            BigDecimal goalVar = calculateVariance(goalValues);
            BigDecimal workStyleVar = calculateVariance(workStyleValues);
            BigDecimal commVar = calculateVariance(commValues);
            BigDecimal agreeVar = calculateVariance(agreeValues);
            BigDecimal conscientVar = calculateVariance(conscientValues);
            BigDecimal honestyVar = calculateVariance(honestyValues);

            class Dimension {
                final String name;
                final BigDecimal variance;
                final int priority;

                Dimension(String name, BigDecimal variance, int priority) {
                    this.name = name;
                    this.variance = variance;
                    this.priority = priority;
                }
            }

            List<Dimension> dimensions = new java.util.ArrayList<>();
            dimensions.add(new Dimension("Goal", goalVar, 2));
            dimensions.add(new Dimension("WorkStyle", workStyleVar, 3));
            dimensions.add(new Dimension("Communication", commVar, 4));
            dimensions.add(new Dimension("Agreeableness", agreeVar, 1));
            dimensions.add(new Dimension("Conscientiousness", conscientVar, 5));
            dimensions.add(new Dimension("HonestyHumility", honestyVar, 6));

            dimensions.sort((d1, d2) -> {
                int varCompare = d1.variance.compareTo(d2.variance);
                if (varCompare != 0) {
                    return varCompare;
                }
                return Integer.compare(d1.priority, d2.priority);
            });

            Dimension top1 = dimensions.get(0);
            Dimension top2 = dimensions.get(1);

            String headline = String.format(
                    "%s와 %s의 조화가 기대되는 팀입니다.", getKoreanDimensionName(top1.name), getKoreanDimensionName(top2.name));
            String summary = String.format(
                    "팀원들의 %s 및 %s 일치도가 매우 높습니다. 서로 같은 지향점을 두고 유사한 템포로 협업을 전개함으로써 마찰 없이 안정적인 협업 시너지를 낼 것으로 기대됩니다.",
                    getKoreanDimensionName(top1.name), getKoreanDimensionName(top2.name));

            List<TitleDescriptionInfo> strengths = new java.util.ArrayList<>();
            strengths.add(getStrength(top1.name));
            strengths.add(getStrength(top2.name));

            List<String> commonPoints = new java.util.ArrayList<>();
            commonPoints.add(getCommonPoint(top1.name));
            commonPoints.add(getCommonPoint(top2.name));

            List<TitleDescriptionInfo> complementaryPoints = new java.util.ArrayList<>();
            long eCount = apps.stream()
                    .filter(a ->
                            a.getExtroversionType() == org.cotato.gongmozip.domains.survey.enums.ExtroversionType.E)
                    .count();
            long iCount = apps.stream()
                    .filter(a ->
                            a.getExtroversionType() == org.cotato.gongmozip.domains.survey.enums.ExtroversionType.I)
                    .count();
            if (eCount > 0 && iCount > 0) {
                complementaryPoints.add(new TitleDescriptionInfo(
                        "외향성 상보성", "외향성과 내향성 성향의 팀원이 골고루 분포하여 발표, 네트워킹, 집중 구현 등 다양한 역할을 상호 보완적으로 분담할 수 있습니다."));
            } else {
                complementaryPoints.add(
                        new TitleDescriptionInfo("안정적 소통 균형", "팀원들의 외향성 분포가 평탄하여 튀는 사람 없이 편안하고 수평적인 의사소통 구도를 형성합니다."));
            }

            List<String> cautions = List.of(
                    "팀원별 연락 패턴(활동 시간대 등)에 맞춰 프로젝트 시작 시 주간 소통 규칙을 협의하는 것을 권장합니다.",
                    "진행 상황의 투명한 조율을 위해 주기적인 화상/대면 체크포인트를 마련하는 것이 좋습니다.");

            BigDecimal matchingScore = group.getMatchingScore() != null ? group.getMatchingScore() : BigDecimal.ZERO;
            BigDecimal goalScore =
                    group.getGoalSimilarityScore() != null ? group.getGoalSimilarityScore() : BigDecimal.ZERO;
            BigDecimal extroversionScore = group.getExtroversionComplementScore() != null
                    ? group.getExtroversionComplementScore()
                    : BigDecimal.ZERO;

            BigDecimal workStyleScore =
                    group.getWorkStyleSimilarityScore() != null ? group.getWorkStyleSimilarityScore() : BigDecimal.ZERO;
            BigDecimal communicationScore = group.getCommunicationSimilarityScore() != null
                    ? group.getCommunicationSimilarityScore()
                    : BigDecimal.ZERO;
            BigDecimal agreeablenessScore = group.getAgreeablenessSimilarityScore() != null
                    ? group.getAgreeablenessSimilarityScore()
                    : BigDecimal.ZERO;
            BigDecimal conscientiousnessScore = group.getConscientiousnessSimilarityScore() != null
                    ? group.getConscientiousnessSimilarityScore()
                    : BigDecimal.ZERO;
            BigDecimal honestyHumilityScore = group.getHonestyHumilitySimilarityScore() != null
                    ? group.getHonestyHumilitySimilarityScore()
                    : BigDecimal.ZERO;

            BigDecimal personalitySum = workStyleScore
                    .add(communicationScore)
                    .add(agreeablenessScore)
                    .add(conscientiousnessScore)
                    .add(honestyHumilityScore);
            BigDecimal personalityScoreOutof100 =
                    personalitySum.multiply(new BigDecimal("100")).divide(new BigDecimal("60"), MathContext.DECIMAL128);

            int totalInt = matchingScore.intValue();
            int goalInt = goalScore.multiply(new BigDecimal("10")).intValue();
            int personalityInt = personalityScoreOutof100.intValue();
            int extroversionInt =
                    extroversionScore.multiply(new BigDecimal("5")).intValue();

            totalInt = Math.max(0, Math.min(100, totalInt));
            goalInt = Math.max(0, Math.min(100, goalInt));
            personalityInt = Math.max(0, Math.min(100, personalityInt));
            extroversionInt = Math.max(0, Math.min(100, extroversionInt));

            matchingTxService.completeReason(
                    reasonId,
                    headline,
                    summary,
                    strengths,
                    commonPoints,
                    complementaryPoints,
                    cautions,
                    totalInt,
                    goalInt,
                    personalityInt,
                    extroversionInt);
            log.info("Successfully completed rule-based matching reason generation for reasonId: {}", reasonId);
        } catch (Exception e) {
            log.error("Failed to generate/save matching reason for reasonId: {}", reasonId, e);
            try {
                matchingTxService.failReason(reasonId, "Exception: " + e.getMessage());
            } catch (Exception failEx) {
                log.error("Failed to mark matching reason as FAILED for reasonId: {}", reasonId, failEx);
            }
        }
    }

    private BigDecimal calculateVariance(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal val : values) {
            sum = sum.add(val != null ? val : BigDecimal.ZERO);
        }
        BigDecimal mean = sum.divide(BigDecimal.valueOf(values.size()), MathContext.DECIMAL128);

        BigDecimal squaredDiffsSum = BigDecimal.ZERO;
        for (BigDecimal val : values) {
            BigDecimal diff = (val != null ? val : BigDecimal.ZERO).subtract(mean);
            squaredDiffsSum = squaredDiffsSum.add(diff.multiply(diff));
        }
        return squaredDiffsSum.divide(BigDecimal.valueOf(values.size()), MathContext.DECIMAL128);
    }

    private String getKoreanDimensionName(String name) {
        return switch (name) {
            case "Goal" -> "목표";
            case "WorkStyle" -> "업무 방식";
            case "Communication" -> "소통 방식";
            case "Agreeableness" -> "우호성";
            case "Conscientiousness" -> "성실성";
            case "HonestyHumility" -> "정직겸손성";
            default -> "성향";
        };
    }

    private TitleDescriptionInfo getStrength(String name) {
        return switch (name) {
            case "Goal" -> new TitleDescriptionInfo(
                    "일치된 프로젝트 목표", "공모전에 임하는 목표(경험 축적 vs 수상 목표 등)가 유사하여 일관성 있는 방향으로 추진할 수 있습니다.");
            case "WorkStyle" -> new TitleDescriptionInfo(
                    "조화로운 업무 스타일", "일정 조율과 업무 분담에 대한 기대치가 비슷하여 효율적이고 조화로운 협업을 보여줍니다.");
            case "Communication" -> new TitleDescriptionInfo(
                    "원활한 소통 템포", "소통 방식과 피드백 주기에 대한 성향이 유사하여 소통 오버헤드가 최소화됩니다.");
            case "Agreeableness" -> new TitleDescriptionInfo(
                    "상호 존중과 배려", "팀원들의 우호성이 높은 일치도를 보여 서로 존중하며 갈등 없이 원만하게 협업할 것입니다.");
            case "Conscientiousness" -> new TitleDescriptionInfo(
                    "체계적인 책임감", "맡은 역할의 계획성과 실행 수준이 고르게 일치하여 일정 지연 우려가 적은 안정적인 협업이 가능합니다.");
            case "HonestyHumility" -> new TitleDescriptionInfo(
                    "상호 투명성과 신뢰", "공정성과 투명한 협업을 중시하는 성향이 높은 일치도를 보이며 단단한 팀 신뢰를 구축합니다.");
            default -> new TitleDescriptionInfo("조화로운 성향", "팀원들의 성향이 고르게 일치하여 편안한 협업 환경을 형성합니다.");
        };
    }

    private String getCommonPoint(String name) {
        return switch (name) {
            case "Goal" -> "프로젝트에 임하는 목표와 기대 수준이 유사합니다.";
            case "WorkStyle" -> "계획적인 진행 상황 공유와 조율 방식을 선호합니다.";
            case "Communication" -> "선호하는 연락 채널과 피드백 속도가 비슷합니다.";
            case "Agreeableness" -> "서로의 의견을 경청하고 배려하는 분위기를 선호합니다.";
            case "Conscientiousness" -> "계획된 태스크를 정해진 시간 내에 수행하는 것을 지향합니다.";
            case "HonestyHumility" -> "공정하고 정직한 역할 수행과 신뢰 기반의 협업을 중시합니다.";
            default -> "서로 조화롭게 의견을 조율해 나갑니다.";
        };
    }

    @Async("aiSummaryExecutor")
    public void generateLeaderRecommendationAsync(Long recId) {
        log.info("Starting async AI leader recommendation generation for recId: {}", recId);

        try {
            matchingTxService.startLeaderRecProcessing(recId);
        } catch (Exception e) {
            log.error("Failed to start AI leader recommendation processing for recId: {}", recId, e);
            return;
        }

        try {
            LeaderRecommendation rec = leaderRecommendationRepository
                    .findById(recId)
                    .orElseThrow(() -> new IllegalArgumentException("LeaderRecommendation not found: " + recId));
            Long teamId = rec.getTeam().getTeamId();

            List<TeamMember> activeMembers =
                    teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);
            if (activeMembers.isEmpty()) {
                try {
                    matchingTxService.failLeaderRec(recId, "No active members found in team: " + teamId);
                } catch (Exception failEx) {
                    log.error("Failed to mark AI leader recommendation as FAILED for recId: {}", recId, failEx);
                }
                return;
            }

            List<LeaderCandidateSnapshot> snapshots = activeMembers.stream()
                    .map(m -> new LeaderCandidateSnapshot(
                            m.getTeamMemberId(),
                            m.getLeaderPreference(),
                            m.getExtroversionType(),
                            m.getExtroversionScore()))
                    .toList();

            List<Long> recommendedTeamMemberIds = aiClient.recommendLeaderCandidates(teamId, snapshots);
            if (recommendedTeamMemberIds.isEmpty()) {
                throw new IllegalStateException("AiClient recommended 0 candidates for team: " + teamId);
            }

            Long recommendedTeamMemberId = recommendedTeamMemberIds.get(0);
            TeamMember recommendedTeamMember = activeMembers.stream()
                    .filter(m -> m.getTeamMemberId().equals(recommendedTeamMemberId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Recommended team member not found in active members: " + recommendedTeamMemberId));
            Long recommendedMemberId = recommendedTeamMember.getMember().getMemberId();
            String recommendedMemberNickname =
                    recommendedTeamMember.getProfile().getNickname();
            String recommendationReason = String.format(
                    "%s님은 팀원들과 소통 성향이 우수하게 상호 보완되며, 팀장 희망 의사(%s)를 고려할 때 팀을 안정적으로 리드할 가능성이 높습니다.",
                    recommendedMemberNickname,
                    recommendedTeamMember.getLeaderPreference() == LeaderPreference.WANTS ? "선호" : "필요 시 수락");

            List<LeaderCandidateResponse> candidates = new ArrayList<>();
            MockAiClient mockAiClient = (MockAiClient) aiClient;

            for (int i = 0; i < recommendedTeamMemberIds.size(); i++) {
                Long tmId = recommendedTeamMemberIds.get(i);
                TeamMember member = activeMembers.stream()
                        .filter(m -> m.getTeamMemberId().equals(tmId))
                        .findFirst()
                        .orElseThrow();
                LeaderCandidateSnapshot candidateSnapshot = snapshots.stream()
                        .filter(s -> s.teamMemberId().equals(tmId))
                        .findFirst()
                        .orElseThrow();

                int rawScore = mockAiClient.finalScore(candidateSnapshot, snapshots);
                int displayScore = (int) (50.0 + (rawScore / 12.0) * 45.0);

                String reason = String.format(
                        "%s 성향과 의사소통 선호도를 바탕으로 팀 일정을 조율하기에 적합합니다.",
                        member.getExtroversionType() == org.cotato.gongmozip.domains.survey.enums.ExtroversionType.E
                                ? "외향적"
                                : "수평적");

                candidates.add(new LeaderCandidateResponse(
                        member.getMember().getMemberId(),
                        member.getProfile().getNickname(),
                        i + 1,
                        displayScore,
                        reason));
            }

            String teamSummary = "팀원들의 성격 유형과 일정 선호도가 비교적 조화롭게 분포되어 있습니다.";
            String caution = "최종 팀장은 AI 추천 결과를 참고하되, 팀원 간 합의를 통해 결정하는 것이 좋습니다.";

            matchingTxService.completeLeaderRec(
                    recId, recommendedMemberId, recommendationReason, candidates, teamSummary, caution);
            log.info("Successfully completed AI leader recommendation for recId: {}", recId);
        } catch (Exception e) {
            log.error("Failed to generate/save AI leader recommendation for recId: {}", recId, e);
            try {
                matchingTxService.failLeaderRec(recId, "Exception: " + e.getMessage());
            } catch (Exception failEx) {
                log.error("Failed to mark AI leader recommendation as FAILED for recId: {}", recId, failEx);
            }
        }
    }
}
