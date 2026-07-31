package org.cotato.gongmozip.domains.chat.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.chat.converter.ChatConverter;
import org.cotato.gongmozip.domains.chat.dto.request.ChatRequest.SendMessageRequest;
import org.cotato.gongmozip.domains.chat.dto.response.ChatResponse.MessageItemResponse;
import org.cotato.gongmozip.domains.chat.dto.response.ChatResponse.MessageListResponse;
import org.cotato.gongmozip.domains.chat.entity.Message;
import org.cotato.gongmozip.domains.chat.enums.MessageType;
import org.cotato.gongmozip.domains.chat.repository.MessageRepository;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.exception.TeamException;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Team이 곧 채팅방이므로(docs/decisions/00-architecture.md), 팀 소속 여부 관련 예외는
 * 별도 ChatErrorCode를 두지 않고 TeamErrorCode를 그대로 사용한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private static final int DEFAULT_MESSAGE_PAGE_SIZE = 50;
    private static final String TEAM_TOPIC_PREFIX = "/topic/teams/";

    private final MessageRepository messageRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public MessageItemResponse sendMessage(Long teamId, Long senderMemberId, SendMessageRequest request) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        TeamMember sender = requireActiveMember(teamId, senderMemberId);

        Message saved = messageRepository.save(ChatConverter.toMemberMessage(team, sender, request.content()));
        return broadcast(teamId, saved);
    }

    public MessageListResponse getMessages(Long teamId, Long requesterMemberId) {
        teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        requireActiveMember(teamId, requesterMemberId);

        List<Message> latestFirst = messageRepository.findByTeam_TeamIdOrderByCreatedAtDesc(
                teamId, PageRequest.of(0, DEFAULT_MESSAGE_PAGE_SIZE));
        return ChatConverter.toMessageListResponse(latestFirst);
    }

    @Transactional
    public void markAsRead(Long teamId, Long memberId) {
        teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        TeamMember member = requireActiveMember(teamId, memberId);
        member.markRead(LocalDateTime.now());
    }

    /** 채팅방 나가기/챗봇 on-off 등 다른 도메인 서비스가 시스템 안내 메시지를 남길 때 사용한다. */
    @Transactional
    public void postSystemMessage(Team team, String content) {
        Message saved = messageRepository.save(ChatConverter.toSystemMessage(team, content));
        broadcast(team.getTeamId(), saved);
    }

    /** 챗봇 상태머신(ChatbotOrchestrationService)이 대화형 안내 메시지를 남길 때 사용한다. */
    @Transactional
    public void postChatbotMessage(Team team, String content) {
        Message saved = messageRepository.save(ChatConverter.toChatbotMessage(team, content));
        broadcast(team.getTeamId(), saved);
    }

    /** 팀장 투표 카드 등 metadata가 필요한 챗봇 카드형 메시지를 남길 때 사용한다. */
    @Transactional
    public void postChatbotCardMessage(Team team, MessageType messageType, String content, String metadata) {
        Message saved =
                messageRepository.save(ChatConverter.toChatbotCardMessage(team, messageType, content, metadata));
        broadcast(team.getTeamId(), saved);
    }

    // 저장된 메시지를 구독 중인 클라이언트에게 실시간으로 내려준다.
    private MessageItemResponse broadcast(Long teamId, Message saved) {
        MessageItemResponse response = ChatConverter.toMessageItemResponse(saved);
        messagingTemplate.convertAndSend(TEAM_TOPIC_PREFIX + teamId, response);
        return response;
    }

    private TeamMember requireActiveMember(Long teamId, Long memberId) {
        return teamMemberRepository
                .findByTeam_TeamIdAndMember_MemberId(teamId, memberId)
                .filter(teamMember -> teamMember.getStatus() == TeamMemberStatus.ACTIVE)
                .orElseThrow(() -> new TeamException(TeamErrorCode.NOT_TEAM_MEMBER));
    }
}
