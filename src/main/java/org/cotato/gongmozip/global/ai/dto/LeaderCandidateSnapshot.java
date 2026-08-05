package org.cotato.gongmozip.global.ai.dto;

import java.math.BigDecimal;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;

/**
 * 팀장 추천 규칙기반 알고리즘(docs/decisions/02-leader-election.md)의 입력값. TeamMember의
 * 팀 생성 시점 스냅샷(leaderPreference/extroversionType/extroversionScore)을 그대로 옮겨온다.
 */
public record LeaderCandidateSnapshot(
        Long teamMemberId,
        LeaderPreference leaderPreference,
        ExtroversionType extroversionType,
        BigDecimal extroversionScore) {}
