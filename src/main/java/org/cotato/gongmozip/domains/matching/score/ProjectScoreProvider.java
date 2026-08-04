package org.cotato.gongmozip.domains.matching.score;

import java.math.BigDecimal;
import java.util.List;
import org.cotato.gongmozip.domains.profile.entity.ProjectExperience;

/** 프로젝트 평가 저장 방식과 역량 점수 계산기를 분리해 신청 서비스가 구체적인 평가 출처에 의존하지 않게 한다. */
public interface ProjectScoreProvider {

    // 프로젝트별 저장 평가를 집계해 0~100 범위의 역량 원점수를 반환한다.
    BigDecimal evaluate(List<ProjectExperience> projects);

    // 신청 전 사전 조회에서 같은 평가 준비 조건을 예외 없이 확인한다. 구현체가 검증을 생략하지 못하도록 필수 계약으로 둔다.
    boolean isReady(List<ProjectExperience> projects);
}
