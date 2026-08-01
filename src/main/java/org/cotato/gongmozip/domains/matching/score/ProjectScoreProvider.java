package org.cotato.gongmozip.domains.matching.score;

import java.math.BigDecimal;
import java.util.List;
import org.cotato.gongmozip.domains.profile.entity.ProjectExperience;

public interface ProjectScoreProvider {

    // 프로젝트 목록을 0~100 역량 원점수로 평가한다 — 현재는 Mock, 이후 AI 구현체로 교체
    BigDecimal evaluate(List<ProjectExperience> projects);
}
