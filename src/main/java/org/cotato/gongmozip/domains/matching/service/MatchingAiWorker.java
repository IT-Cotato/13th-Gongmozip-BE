package org.cotato.gongmozip.domains.matching.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.LeaderCandidateResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.TitleDescriptionInfo;
import org.cotato.gongmozip.domains.matching.entity.LeaderRecommendation;
import org.cotato.gongmozip.domains.matching.repository.LeaderRecommendationRepository;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingAiWorker {

    private final MatchingTxService matchingTxService;
    private final LeaderRecommendationRepository leaderRecommendationRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Async("aiSummaryExecutor")
    public void generateMatchingReasonAsync(Long reasonId) {
        log.info("Starting async AI matching reason generation for reasonId: {}", reasonId);

        try {
            matchingTxService.startReasonProcessing(reasonId);
        } catch (Exception e) {
            log.error("Failed to start AI matching reason processing for reasonId: {}", reasonId, e);
            return;
        }

        try {
            // 3초 요약 처리 시뮬레이션
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("AI matching reason generation was interrupted", e);
            try {
                matchingTxService.failReason(reasonId, "Interrupted: " + e.getMessage());
            } catch (Exception failEx) {
                log.error("Failed to mark AI matching reason as FAILED for reasonId: {}", reasonId, failEx);
            }
            return;
        }

        try {
            String headline = "목표와 협업 방식이 잘 맞는 팀입니다.";
            String summary = "팀원들의 공모전 목표와 일정 관리 방식이 유사하여 안정적인 협업이 예상됩니다.";
            List<TitleDescriptionInfo> strengths = List.of(
                    new TitleDescriptionInfo("유사한 팀 목표", "팀원 대부분이 프로젝트 완성과 수상을 중요한 목표로 두고 있습니다."),
                    new TitleDescriptionInfo("안정적인 일정 관리", "팀원들이 정기적인 진행 상황 공유와 일정 준수를 중요하게 생각합니다."));
            List<String> commonPoints = List.of("정기적인 진행 상황 공유를 선호합니다.", "일정을 미리 정하고 준수하는 것을 중요하게 생각합니다.");
            List<TitleDescriptionInfo> complementaryPoints = List.of(
                    new TitleDescriptionInfo("외향성 상보성", "외향성이 높은 팀원과 낮은 팀원이 함께 있어 발표와 집중 작업 역할을 다양하게 나누기 좋습니다."));
            List<String> cautions = List.of("팀원별 선호 연락 시간이 다르므로 프로젝트 시작 시 소통 시간을 합의하는 것이 좋습니다.");

            matchingTxService.completeReason(
                    reasonId,
                    headline,
                    summary,
                    strengths,
                    commonPoints,
                    complementaryPoints,
                    cautions,
                    84,
                    90,
                    82,
                    78);
            log.info("Successfully completed AI matching reason generation for reasonId: {}", reasonId);
        } catch (Exception e) {
            log.error("Failed to generate/save AI matching reason for reasonId: {}", reasonId, e);
            try {
                matchingTxService.failReason(reasonId, "Exception: " + e.getMessage());
            } catch (Exception failEx) {
                log.error("Failed to mark AI matching reason as FAILED for reasonId: {}", reasonId, failEx);
            }
        }
    }

    @Async("aiSummaryExecutor")
    public void generateLeaderRecommendationAsync(Long recId) {
        log.info("Starting async AI leader recommendation generation for recId: {}", recId);

        try {
            matchingTxService.startLeaderRecProcessing(recId);
        } catch (Exception e) {
            log.error("Failed to start AI leader recommendation processing for recId: {}", recId, e);
            return;
        }

        try {
            // 3초 처리 시뮬레이션
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("AI leader recommendation generation was interrupted", e);
            try {
                matchingTxService.failLeaderRec(recId, "Interrupted: " + e.getMessage());
            } catch (Exception failEx) {
                log.error("Failed to mark AI leader recommendation as FAILED for recId: {}", recId, failEx);
            }
            return;
        }

        try {
            LeaderRecommendation rec = leaderRecommendationRepository
                    .findById(recId)
                    .orElseThrow(() -> new IllegalArgumentException("LeaderRecommendation not found: " + recId));
            Long teamId = rec.getTeam().getTeamId();

            List<TeamMember> activeMembers =
                    teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);
            if (activeMembers.isEmpty()) {
                try {
                    matchingTxService.failLeaderRec(recId, "No active members found in team: " + teamId);
                } catch (Exception failEx) {
                    log.error("Failed to mark AI leader recommendation as FAILED for recId: {}", recId, failEx);
                }
                return;
            }

            // 첫 번째 사람을 리더 후보로 추천
            TeamMember firstMember = activeMembers.get(0);
            Long recommendedMemberId = firstMember.getMember().getMemberId();
            String recommendedMemberNickname = firstMember.getProfile().getNickname();
            String recommendationReason = "높은 성실성과 원활한 의사소통 성향을 바탕으로 팀 일정을 안정적으로 관리할 가능성이 높습니다.";

            List<LeaderCandidateResponse> candidates = new ArrayList<>();
            candidates.add(new LeaderCandidateResponse(
                    recommendedMemberId, recommendedMemberNickname, 1, 87, "성실성과 의사소통 선호도가 높아 일정 조율에 적합합니다."));

            if (activeMembers.size() > 1) {
                TeamMember secondMember = activeMembers.get(1);
                candidates.add(new LeaderCandidateResponse(
                        secondMember.getMember().getMemberId(),
                        secondMember.getProfile().getNickname(),
                        2,
                        81,
                        "협업 경험과 외향성이 높아 팀원들의 의견을 이끄는 역할에 적합합니다."));
            }

            String teamSummary = "팀원들의 성실성이 전반적으로 높고 외향성 분포가 고르게 구성되어 있습니다.";
            String caution = "최종 팀장은 AI 결과가 아닌 팀원 간 합의를 통해 결정하는 것이 좋습니다.";

            matchingTxService.completeLeaderRec(
                    recId, recommendedMemberId, recommendationReason, candidates, teamSummary, caution);
            log.info("Successfully completed AI leader recommendation for recId: {}", recId);
        } catch (Exception e) {
            log.error("Failed to generate/save AI leader recommendation for recId: {}", recId, e);
            try {
                matchingTxService.failLeaderRec(recId, "Exception: " + e.getMessage());
            } catch (Exception failEx) {
                log.error("Failed to mark AI leader recommendation as FAILED for recId: {}", recId, failEx);
            }
        }
    }
}
