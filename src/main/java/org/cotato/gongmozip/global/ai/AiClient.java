package org.cotato.gongmozip.global.ai;

import java.util.List;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.global.ai.dto.LeaderCandidateSnapshot;
import org.cotato.gongmozip.global.ai.dto.ProjectEvaluationResult;

public interface AiClient {

    String generateSummary(String projectName, String role, String description);

    ProjectEvaluationResult evaluateProject(String projectName, String role, String description);

    /**
     * 팀장 여부 투표를 시작할 때 정보성으로 보여줄 팀장 후보 추천 (최대 2명).
     * 규칙기반 점수 산출은 docs/decisions/02-leader-election.md 참고.
     */
    List<Long> recommendLeaderCandidates(Long teamId, List<LeaderCandidateSnapshot> activeMembers);

    /** 팀장 투표가 동률일 때, 동률 후보 중 추천할 후보 1명. */
    Long recommendTiebreakLeader(
            Long teamId, List<LeaderCandidateSnapshot> activeMembers, List<Long> tiedCandidateTeamMemberIds);

    /** 팀 카테고리 기준으로 추천할 공모전 (최대 3개). */
    List<Long> recommendContests(InterestCategory category, List<Long> openContestIds);

    /** 채팅방에서 "@챗봇"으로 말을 걸었을 때 자유 질의에 답한다. */
    String answerTeamQuestion(String question);
}
