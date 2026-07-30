package org.cotato.gongmozip.global.ai;

import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.global.ai.dto.ProjectEvaluationResult;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MockAiClient implements AiClient {

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
}
