package org.cotato.gongmozip.domains.matching.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description =
                "매칭 신청 상태: WAITING=매칭풀 대기, CANCELED=14시 전 무료 취소, PASSED=14시 이후 패스, MATCHING=매칭 계산 중, PROPOSED=매칭 결과 제안, MATCHED=매칭 확정, FAILED=팀 구성 실패")
public enum MatchingApplicationStatus {
    // 매칭 신청이 완료되어 당일 매칭풀에서 팀 구성을 기다리는 상태
    WAITING,

    // 오후 2시 이전에 무료로 신청을 철회한 상태 (협업거리 차감 없음)
    CANCELED,

    // 오후 2시 이후 당일 자정 전에 신청을 철회한 상태 (협업거리 패널티 적용)
    PASSED,

    // 스케줄러가 신청을 가져가 실제 팀 조합을 계산하고 있는 상태
    // TODO: 매칭 알고리즘/스케줄러 구현 시 상태 전이를 연결한다.
    MATCHING,

    // 팀 조합이 생성되어 사용자들의 매칭 수락 또는 패스 응답을 기다리는 상태
    // TODO: 매칭 결과 및 의사결정 API 구현 시 상태 전이를 연결한다.
    PROPOSED,

    // 팀원들의 수락 절차가 끝나 최종 팀 구성이 확정된 상태
    // TODO: 매칭 확정 및 팀 생성 로직 구현 시 상태 전이를 연결한다.
    MATCHED,

    // 인원 부족 등의 이유로 당일 팀을 구성하지 못해 다음날 매칭풀로 넘어가야 하는 상태
    // TODO: 실패 처리 및 다음날 재신청 대상 관리 로직 구현 시 상태 전이를 연결한다.
    FAILED
}
