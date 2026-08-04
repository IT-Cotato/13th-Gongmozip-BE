package org.cotato.gongmozip.domains.matching.algorithm;

import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingPoolInput;
import org.cotato.gongmozip.domains.matching.algorithm.model.result.MatchingPlan;

/** 풀 준비·결과 저장 계층이 구체적인 탐색 방식에 의존하지 않도록 매칭 알고리즘의 공통 계약을 정의한다. */
public interface TeamMatchingAlgorithm {

    /** 검증이 끝난 하나의 풀 입력을 받아 팀과 미배정자를 모두 포함한 완전한 계획을 만든다. */
    MatchingPlan match(MatchingPoolInput input);
}
