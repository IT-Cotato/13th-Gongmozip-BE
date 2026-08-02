package org.cotato.gongmozip.domains.matching.score;

import java.math.BigDecimal;
import java.util.List;
import org.cotato.gongmozip.domains.profile.entity.ProjectExperience;
import org.springframework.stereotype.Component;

@Component
public class MockProjectScoreProvider implements ProjectScoreProvider {

    private static final BigDecimal MOCK_PROJECT_SCORE = new BigDecimal("50.00");

    @Override
    public BigDecimal evaluate(List<ProjectExperience> projects) {
        // TODO: 프로젝트 AI 평가 구현체가 준비되면 이 Mock 빈을 실제 Provider로 교체한다.
        return projects.isEmpty() ? BigDecimal.ZERO.setScale(2) : MOCK_PROJECT_SCORE;
    }
}
