package org.cotato.gongmozip.domains.matching.score;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingCandidate;
import org.cotato.gongmozip.domains.matching.algorithm.model.result.TeamCompatibilityScore;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.springframework.stereotype.Component;

/**
 * 팀 후보 평가 공식을 탐색 알고리즘과 분리해 모든 알고리즘이 동일한 100점 기준을 사용하도록 만들었다.
 * 리더 구성, 성향 유사도, 외향성 보완 점수를 세부 항목별로 계산해 결과 설명에 필요한 근거도 함께 반환한다.
 */
@Component
@RequiredArgsConstructor
public class TeamCompatibilityCalculator {

    private final SimilarityScorer similarityScorer;

    public TeamCompatibilityScore calculate(List<MatchingCandidate> team) {
        validateTeam(team);

        // 정책별 최대 점수의 합은 100점이며, 각 항목은 저장 가능한 소수 둘째 자리로 정규화한다.
        BigDecimal leader = leaderHarmony(team);
        BigDecimal goal = similarity(team, MatchingCandidate::goalPreferenceScore, 10);
        BigDecimal workStyle = similarity(team, MatchingCandidate::workStyleScore, 10);
        BigDecimal communication = similarity(team, MatchingCandidate::communicationStyleScore, 10);
        BigDecimal agreeableness = similarity(team, MatchingCandidate::agreeablenessScore, 20);
        BigDecimal conscientiousness = similarity(team, MatchingCandidate::conscientiousnessScore, 10);
        BigDecimal honestyHumility = similarity(team, MatchingCandidate::honestyHumilityScore, 10);
        BigDecimal extroversion = extroversionComplement(team);
        BigDecimal total = List.of(
                        leader,
                        goal,
                        workStyle,
                        communication,
                        agreeableness,
                        conscientiousness,
                        honestyHumility,
                        extroversion)
                .stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        if (total.compareTo(BigDecimal.ZERO) < 0 || total.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalStateException("팀 호환도 총점은 0에서 100 사이여야 합니다.");
        }
        return new TeamCompatibilityScore(
                leader,
                goal,
                workStyle,
                communication,
                agreeableness,
                conscientiousness,
                honestyHumility,
                extroversion,
                total);
    }

    private BigDecimal leaderHarmony(List<MatchingCandidate> team) {
        // 명확한 팀장 희망자가 한 명일 때 최고점이며, 팀 크기별 정책표로 모호하거나 충돌하는 구성을 감점한다.
        long wants = team.stream()
                .filter(candidate -> candidate.leaderPreference() == LeaderPreference.WANTS)
                .count();
        long neutral = team.stream()
                .filter(candidate -> candidate.leaderPreference() == LeaderPreference.NEUTRAL)
                .count();
        int score = team.size() == 4 ? leaderHarmonyForFour(wants, neutral) : leaderHarmonyForThree(wants, neutral);
        return BigDecimal.valueOf(score).setScale(2);
    }

    private int leaderHarmonyForFour(long wants, long neutral) {
        if (wants == 1) return 10;
        if (wants == 2) return 7;
        if (wants == 3) return 3;
        if (wants == 4) return 0;
        return switch ((int) neutral) {
            case 4 -> 8;
            case 3 -> 7;
            case 2 -> 6;
            case 1 -> 5;
            case 0 -> 0;
            default -> throw new IllegalStateException("예상하지 못한 팀장 중립 인원수입니다: " + neutral);
        };
    }

    private int leaderHarmonyForThree(long wants, long neutral) {
        if (wants == 1) return 10;
        if (wants == 2) return 5;
        if (wants == 3) return 0;
        return switch ((int) neutral) {
            case 3 -> 8;
            case 2 -> 7;
            case 1 -> 5;
            case 0 -> 0;
            default -> throw new IllegalStateException("예상하지 못한 팀장 중립 인원수입니다: " + neutral);
        };
    }

    private BigDecimal similarity(
            List<MatchingCandidate> team, Function<MatchingCandidate, BigDecimal> extractor, int maximumScore) {
        return similarityScorer.score(team.stream().map(extractor).toList(), maximumScore);
    }

    private BigDecimal extroversionComplement(List<MatchingCandidate> team) {
        // 외향·중간·내향 인원 분포를 팀 크기별 정책표에 대입해 상호 보완 정도를 평가한다.
        long e = count(team, ExtroversionType.E);
        long a = count(team, ExtroversionType.A);
        long i = count(team, ExtroversionType.I);
        int score = team.size() == 4 ? extroversionForFour(e, a, i) : extroversionForThree(e, a, i);
        return BigDecimal.valueOf(score).setScale(2);
    }

    private int extroversionForFour(long e, long a, long i) {
        return switch (e + "-" + a + "-" + i) {
            case "1-2-1" -> 20;
            case "1-1-2", "2-1-1" -> 18;
            case "0-4-0" -> 17;
            case "1-3-0", "0-3-1" -> 16;
            case "2-0-2" -> 15;
            case "1-0-3" -> 14;
            case "2-2-0", "0-2-2" -> 13;
            case "3-1-0", "3-0-1" -> 10;
            case "0-1-3" -> 9;
            case "4-0-0", "0-0-4" -> 5;
            default -> throw new IllegalStateException("알 수 없는 외향성 분포입니다: " + e + "/" + a + "/" + i);
        };
    }

    private int extroversionForThree(long e, long a, long i) {
        return switch (e + "-" + a + "-" + i) {
            case "1-1-1" -> 20;
            case "0-3-0" -> 18;
            case "1-2-0" -> 16;
            case "0-2-1" -> 15;
            case "1-0-2" -> 14;
            case "2-1-0" -> 11;
            case "0-1-2" -> 10;
            case "2-0-1" -> 8;
            case "0-0-3" -> 5;
            case "3-0-0" -> 3;
            default -> throw new IllegalStateException("알 수 없는 외향성 분포입니다: " + e + "/" + a + "/" + i);
        };
    }

    private long count(List<MatchingCandidate> team, ExtroversionType type) {
        return team.stream()
                .filter(candidate -> candidate.extroversionType() == type)
                .count();
    }

    private void validateTeam(List<MatchingCandidate> team) {
        // 점수표가 정의된 3·4인 팀만 허용하고 같은 신청자가 중복 포함되는 계산 오류를 차단한다.
        if (team == null || team.size() < 3 || team.size() > 4) {
            throw new IllegalArgumentException("팀 호환도는 3명 또는 4명의 후보로 계산해야 합니다.");
        }
        Set<Long> ids = new HashSet<>();
        for (MatchingCandidate candidate : team) {
            if (candidate == null || !ids.add(candidate.applicationId())) {
                throw new IllegalArgumentException("팀에 null 또는 중복 후보를 포함할 수 없습니다.");
            }
        }
    }
}
