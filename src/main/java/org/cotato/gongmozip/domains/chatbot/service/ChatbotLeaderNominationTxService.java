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

    private static final String LEADER_SELECTION_PROMPT = "모두 인사를 마쳤네요! 이제 팀장을 선출해볼게요. 팀장이 되고 싶은 분은 투표해주세요.";

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
                .map(member -> member.getProfile().getNickname())
                .collect(Collectors.joining(", "));
        String content = recommendedNames.isBlank()
                ? LEADER_SELECTION_PROMPT
                : LEADER_SELECTION_PROMPT + "\n\nAI 추천: " + recommendedNames + "님이 팀장으로 잘 어울릴 것 같아요!";

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
