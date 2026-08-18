package org.cotato.gongmozip.domains.chatbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.chat.enums.MessageType;
import org.cotato.gongmozip.domains.chat.service.ChatService;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.exception.TeamException;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ChatbotLeaderNominationAsyncService}가 AI 응답을 받아온 뒤 {@code LEADER_NOMINATION_CARD}
 * 발행을 전담한다. 비동기 스레드에서 새로 호출되므로(원래 상태 전이 트랜잭션과 무관) 새 트랜잭션에서
 * 시작한다.
 */
@Service
@RequiredArgsConstructor
public class ChatbotLeaderNominationTxService {

    // AI 추천이 없을 때(추천 호출 실패 등)만 쓰는 대체 문구 — Figma 5.1.3.2는 항상 추천이 있는
    // 상태만 그려져 있어 이 케이스의 문구는 정의돼 있지 않다.
    private static final String LEADER_SELECTION_PROMPT = "이제, 팀장을 선출해볼게요. 팀장이 되고 싶은 분은 투표해주세요.";
    // Figma 5.1.3.2 "팀장 선출(아무도 사전 팀장 희망 하지 않은 경우)" 문구 그대로 (2026-08-18).
    private static final String LEADER_SELECTION_WITH_RECOMMENDATION_TEMPLATE =
            "이제, 팀장을 선출해볼게요. 매칭 전에 팀장을 지원해주신 분이 없으셔서 사용자 프로필 및 협업 유형"
                    + " 검사 결과 %s이 팀장을 잘하실 수 있을 거라 추천드립니다. 다른 분들도 모두 팀장을 하기"
                    + " 충분한 자질을 가지신 분들이니, 팀장 여부를 모두 투표해주세요.";

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ChatService chatService;
    // 이 프로젝트에는 Spring이 자동 구성한 ObjectMapper 빈이 없어 직접 생성한다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void announceLeaderNomination(Long teamId, List<Long> recommendedIds) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);
        String recommendedNames = activeMembers.stream()
                .filter(member -> recommendedIds.contains(member.getTeamMemberId()))
                .map(member -> member.getProfile().getNickname() + "님")
                .collect(Collectors.joining(" 혹은 "));
        String content = recommendedNames.isBlank()
                ? LEADER_SELECTION_PROMPT
                : String.format(LEADER_SELECTION_WITH_RECOMMENDATION_TEMPLATE, recommendedNames);

        chatService.postChatbotCardMessage(
                team, MessageType.LEADER_NOMINATION_CARD, content, toIdsMetadata(recommendedIds));
    }

    private String toIdsMetadata(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(Map.of("aiRecommendedTeamMemberIds", ids));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("AI 추천 메타데이터 직렬화에 실패했습니다.", e);
        }
    }
}
