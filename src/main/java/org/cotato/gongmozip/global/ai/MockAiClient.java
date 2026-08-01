package org.cotato.gongmozip.global.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.global.ai.dto.ProjectEvaluationResult;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MockAiClient implements AiClient {

    private static final int MAX_LEADER_RECOMMENDATIONS = 2;
    private static final int MAX_CONTEST_RECOMMENDATIONS = 3;
    private static final Random RANDOM = new Random();

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
    public List<Long> recommendLeaderCandidates(List<Long> activeTeamMemberIds) {
        log.info("AI leader candidate recommendation simulation for {} members", activeTeamMemberIds.size());
        return shuffledSample(activeTeamMemberIds, MAX_LEADER_RECOMMENDATIONS);
    }

    @Override
    public Long recommendTiebreakLeader(List<Long> tiedCandidateTeamMemberIds) {
        log.info("AI tiebreak recommendation simulation among {} candidates", tiedCandidateTeamMemberIds.size());
        return tiedCandidateTeamMemberIds.get(RANDOM.nextInt(tiedCandidateTeamMemberIds.size()));
    }

    @Override
    public List<Long> recommendContests(InterestCategory category, List<Long> openContestIds) {
        log.info("AI contest recommendation simulation for category: {}", category);
        return shuffledSample(openContestIds, MAX_CONTEST_RECOMMENDATIONS);
    }

    @Override
    public String answerTeamQuestion(String question) {
        log.info("AI team question simulation for: {}", question);

        if (question == null || question.isBlank()) {
            return "궁금한 점을 말씀해주시면 도와드릴게요! 예) @챗봇 우리 역할 분담 추천해줘";
        }

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

    private List<Long> shuffledSample(List<Long> ids, int limit) {
        List<Long> shuffled = new ArrayList<>(ids);
        Collections.shuffle(shuffled, RANDOM);
        return shuffled.stream().limit(limit).toList();
    }
}
