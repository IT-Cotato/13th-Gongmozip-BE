package org.cotato.gongmozip.global.ai;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.cotato.gongmozip.global.ai.dto.LeaderCandidateSnapshot;
import org.cotato.gongmozip.global.ai.dto.ProjectEvaluationResult;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockAiClient implements AiClient {

    private static final int MAX_LEADER_RECOMMENDATIONS = 2;
    private static final int MAX_CONTEST_RECOMMENDATIONS = 3;
    private static final int NEUTRAL_BONUS = 2;
    private static final int NO_MAJORITY_SCORE = 7;
    private static final String ANSWER_TEAM_QUESTION_SYSTEM_PROMPT =
            "너는 대학생 공모전 팀 프로젝트를 돕는 챗봇이야. 팀원의 질문에 2~4문장으로, 바로 실행할 수 있는 " + "조언 위주로 한국어 반말 없이 정중하게 답해줘. 질문: ";

    private final GeminiClient geminiClient;

    @Override
    public String generateSummary(String projectName, String role, String description) {
        try {
            log.info("AI summary generation simulation start for project: {}", projectName);
            // 3초간 비동기 요약 처리를 시뮬레이션
            Thread.sleep(3000);
            log.info("AI summary generation simulation completed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("AI summary generation was interrupted", e);
        }
        return String.format("프로젝트 %s에서 %s 역할을 맡아 기획 및 개발을 주도적으로 수행하였습니다.", projectName, role);
    }

    @Override
    public ProjectEvaluationResult evaluateProject(String projectName, String role, String description) {
        try {
            log.info("AI project evaluation simulation start for project: {}", projectName);
            // 3초간 비동기 역량 평가 처리를 시뮬레이션
            Thread.sleep(3000);
            log.info("AI project evaluation simulation completed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("AI project evaluation was interrupted", e);
        }

        String feedback = String.format("프로젝트 %s에서 %s 역할을 주도적으로 수행하여 문제해결력과 도전정신이 우수한 것으로 평가되었습니다.", projectName, role);

        return new ProjectEvaluationResult(85, feedback);
    }

    @Override
    public List<Long> recommendLeaderCandidates(Long teamId, List<LeaderCandidateSnapshot> activeMembers) {
        log.info("Rule-based leader candidate recommendation for team {} ({} members)", teamId, activeMembers.size());
        Comparator<LeaderCandidateSnapshot> ranking = rankingComparator(teamId, activeMembers);
        return activeMembers.stream()
                .sorted(ranking)
                .limit(MAX_LEADER_RECOMMENDATIONS)
                .map(LeaderCandidateSnapshot::teamMemberId)
                .toList();
    }

    @Override
    public Long recommendTiebreakLeader(
            Long teamId, List<LeaderCandidateSnapshot> activeMembers, List<Long> tiedCandidateTeamMemberIds) {
        log.info(
                "Rule-based tiebreak recommendation for team {} among {} candidates",
                teamId,
                tiedCandidateTeamMemberIds.size());
        Set<Long> tiedIds = new HashSet<>(tiedCandidateTeamMemberIds);
        Comparator<LeaderCandidateSnapshot> ranking = rankingComparator(teamId, activeMembers);
        return activeMembers.stream()
                .filter(member -> tiedIds.contains(member.teamMemberId()))
                .min(ranking)
                .map(LeaderCandidateSnapshot::teamMemberId)
                .orElse(null);
    }

    /**
     * 팀장 추천 규칙기반 정렬 순서(최고 순으로 먼저 오도록 오름차순 comparator를 만든다).
     * docs/decisions/02-leader-election.md "팀장 추천 및 추천 이유" 표 기준:
     * 1) 최종점수(적합도+가점) 내림차순
     * 2) 동률 시 팀장 희망 우선순위(WANTS&gt;NEUTRAL&gt;DOES_NOT_WANT) 내림차순
     * 3) 그래도 동률이면 외향성 원점수 내림차순
     * 4) 그래도 동률이면 팀ID 시드 고정 랜덤
     */
    private Comparator<LeaderCandidateSnapshot> rankingComparator(
            Long teamId, List<LeaderCandidateSnapshot> allMembers) {
        Comparator<LeaderCandidateSnapshot> byFinalScore =
                Comparator.comparingInt((LeaderCandidateSnapshot member) -> finalScore(member, allMembers));
        Comparator<LeaderCandidateSnapshot> byLeaderPreference = Comparator.comparingInt(
                (LeaderCandidateSnapshot member) -> member.leaderPreference().getEffectiveLeaderUnits());
        Comparator<LeaderCandidateSnapshot> byExtroversionScore =
                Comparator.comparing(LeaderCandidateSnapshot::extroversionScore);
        Comparator<LeaderCandidateSnapshot> bySeededRandom =
                Comparator.comparingLong(member -> seededRandomKey(teamId, member.teamMemberId()));

        return byFinalScore
                .reversed()
                .thenComparing(byLeaderPreference.reversed())
                .thenComparing(byExtroversionScore.reversed())
                .thenComparing(bySeededRandom);
    }

    // 최종점수(후보) = 적합도 점수 + (희망의사=="필요하면"이면 가점 2점, 아니면 0점)
    int finalScore(LeaderCandidateSnapshot candidate, List<LeaderCandidateSnapshot> allMembers) {
        List<ExtroversionType> remainingTypes = allMembers.stream()
                .filter(member -> !member.teamMemberId().equals(candidate.teamMemberId()))
                .map(LeaderCandidateSnapshot::extroversionType)
                .toList();
        int compatibility = compatibilityScore(candidate.extroversionType(), remainingTypes);
        int neutralBonus = candidate.leaderPreference() == LeaderPreference.NEUTRAL ? NEUTRAL_BONUS : 0;
        return compatibility + neutralBonus;
    }

    // "팀장 후보 유형 × 잔여 팀원 다수 유형" 적합도 점수 표. 잔여 팀원 중 다수 유형이 유일하게
    // 정해지지 않으면(동률) 후보 유형과 무관하게 7점(A 다수와 동일 취급)을 준다.
    int compatibilityScore(ExtroversionType candidateType, List<ExtroversionType> remainingTypes) {
        ExtroversionType majority = majorityType(remainingTypes);
        if (majority == null) {
            return NO_MAJORITY_SCORE;
        }
        return switch (candidateType) {
            case E -> switch (majority) {
                case I -> 10;
                case A -> 7;
                case E -> 3;
            };
            case A -> 6;
            case I -> switch (majority) {
                case E -> 5;
                case A, I -> 2;
            };
        };
    }

    // 최빈 유형이 유일할 때만 반환하고, 동률이면 null(=다수 원칙 미적용)을 반환한다.
    ExtroversionType majorityType(List<ExtroversionType> types) {
        Map<ExtroversionType, Long> counts =
                types.stream().collect(Collectors.groupingBy(type -> type, Collectors.counting()));
        long max = counts.values().stream().max(Long::compareTo).orElse(0L);
        List<ExtroversionType> topTypes = counts.entrySet().stream()
                .filter(e -> e.getValue() == max)
                .map(Map.Entry::getKey)
                .toList();
        return topTypes.size() == 1 ? topTypes.get(0) : null;
    }

    // 팀ID + 팀원ID 조합으로 시드를 고정해, 같은 팀·같은 팀원에 대해서는 항상 같은 값을 반환한다
    // (팀장 추천 동률 처리 4순위: "팀ID 시드 고정 랜덤").
    private long seededRandomKey(Long teamId, Long teamMemberId) {
        return new Random(teamId * 31 + teamMemberId).nextLong();
    }

    // 카테고리 내 마감이 가장 많이 남은 순서대로 최대 3개를 추천한다. 호출부
    // (ChatbotOrchestrationService)가 이미 마감 내림차순으로 정렬해서 넘겨주므로, 여기서는
    // 그 순서를 그대로 유지한 채 상위 N개만 자른다(더 이상 셔플하지 않음).
    @Override
    public List<Long> recommendContests(
            InterestCategory category, List<Long> openContestIds, List<String> completedContestTitles) {
        log.info(
                "Rule-based contest recommendation (deadline desc) for category: {} with completed contests: {}",
                category,
                completedContestTitles);
        return openContestIds.stream().limit(MAX_CONTEST_RECOMMENDATIONS).toList();
    }

    @Override
    public String answerTeamQuestion(String question) {
        if (question == null || question.isBlank()) {
            return "궁금한 점을 말씀해주시면 도와드릴게요! 예) @챗봇 우리 역할 분담 추천해줘";
        }

        if (geminiClient.isEnabled()) {
            try {
                return geminiClient.generateContent(ANSWER_TEAM_QUESTION_SYSTEM_PROMPT + question);
            } catch (Exception e) {
                log.warn("Gemini API 호출에 실패해 키워드 기반 응답으로 대체합니다.", e);
            }
        }

        return answerWithKeywordFallback(question);
    }

    // Gemini 키가 없거나(로컬/테스트) 호출이 실패했을 때 쓰는 키워드 기반 응답.
    private String answerWithKeywordFallback(String question) {
        if (question.contains("역할")) {
            return "역할 분담 추천이에요:\n"
                    + "- 기획/PM: 아이디어 정리와 일정 관리\n"
                    + "- 디자인: 화면/자료 디자인\n"
                    + "- 개발: 기능 구현\n"
                    + "팀원 수와 강점에 맞게 나눠보세요!";
        }

        if (question.contains("타임라인") || question.contains("일정")) {
            return "타임라인 추천이에요:\n"
                    + "1주차: 아이디어 구체화\n"
                    + "2~3주차: 제작/개발\n"
                    + "4주차: 마무리 및 제출 준비\n"
                    + "공모전 마감일 기준으로 역산해서 조정해보세요!";
        }

        return "아직 그 질문에는 구체적으로 답하기 어려워요. " + "'역할 분담'이나 '타임라인'처럼 구체적으로 물어봐주세요!";
    }
}
