package org.cotato.gongmozip.domains.chat.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.MemberAvatarResponse;
import org.cotato.gongmozip.domains.character.service.CharacterService;
import org.cotato.gongmozip.domains.chat.converter.ChatConverter;
import org.cotato.gongmozip.domains.chat.dto.request.ChatRequest.SendMessageRequest;
import org.cotato.gongmozip.domains.chat.dto.response.ChatResponse.MessageItemResponse;
import org.cotato.gongmozip.domains.chat.dto.response.ChatResponse.MessageListResponse;
import org.cotato.gongmozip.domains.chat.dto.response.ChatResponse.MessageUnreadUpdateResponse;
import org.cotato.gongmozip.domains.chat.entity.Message;
import org.cotato.gongmozip.domains.chat.enums.MessageType;
import org.cotato.gongmozip.domains.chat.repository.MessageRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
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
    private static final String TEAM_READ_UPDATES_TOPIC_SUFFIX = "/read-updates";

    private final MessageRepository messageRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final CharacterService characterService;

    @Transactional
    public MessageItemResponse sendMessage(Long teamId, Long senderMemberId, SendMessageRequest request) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        TeamMember sender = requireActiveMember(teamId, senderMemberId);

        Message saved = messageRepository.save(ChatConverter.toMemberMessage(team, sender, request.content()));
        return broadcast(teamId, saved);
    }

    /**
     * cursor가 null이면 최신 {@value #DEFAULT_MESSAGE_PAGE_SIZE}건을, cursor가 있으면(직전 응답의
     * 가장 오래된 메시지 messageId) 그보다 더 오래된 메시지를 이어서 조회한다. 다음 페이지 존재
     * 여부를 별도 COUNT 쿼리 없이 판단하려고 페이지 크기보다 1건 더 조회해 잘라낸다.
     */
    public MessageListResponse getMessages(Long teamId, Long requesterMemberId, Long cursor) {
        teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        requireActiveMember(teamId, requesterMemberId);

        List<Message> fetched = messageRepository.findByTeamIdBeforeCursor(
                teamId, cursor, PageRequest.of(0, DEFAULT_MESSAGE_PAGE_SIZE + 1));
        boolean hasNext = fetched.size() > DEFAULT_MESSAGE_PAGE_SIZE;
        List<Message> latestFirst = hasNext ? fetched.subList(0, DEFAULT_MESSAGE_PAGE_SIZE) : fetched;

        List<Member> senders = latestFirst.stream()
                .map(Message::getSenderTeamMember)
                .filter(Objects::nonNull)
                .map(TeamMember::getMember)
                .distinct()
                .toList();
        Map<Long, MemberAvatarResponse> avatarsByMemberId = characterService.findAvatarsByMembers(senders);
        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);

        return ChatConverter.toMessageListResponse(latestFirst, avatarsByMemberId, activeMembers, hasNext);
    }

    /**
     * 읽음 처리 시점까지 안읽음 상태였던(=이번에 새로 읽음 처리된) 메시지들의 안읽음 수를
     * 다시 계산해 실시간으로 갱신 브로드캐스트한다. 안읽음 수 자체는 실시간 반영이 필요 없다고
     * 봤던 기존 결정(docs/decisions/03-chat.md)과 달리, 메시지별 안읽음 수는 화면에 이미 떠있는
     * 숫자를 살아있는 값으로 유지해야 해서 이번엔 브로드캐스트를 붙인다.
     *
     * <p>영향받는 메시지는 최신 {@value #DEFAULT_MESSAGE_PAGE_SIZE}건으로 캡을 둔다 — cursor
     * 페이지네이션(이슈 #115)으로 화면이 그 너머까지 보일 수 있지만, 메시지별 안읽음 수는 한 번
     * 0에 도달하면 다시 바뀌지 않는 값이라 "활발히 보고 있을 만한 최근 구간"만 실시간으로 맞춰주면
     * 충분하다고 판단했다. 그보다 오래된 메시지는 {@code getMessages}가 요청마다 그 자리에서 다시
     * 계산해 내려주므로(그 페이지를 다시 불러오는 순간 항상 정확한 값), 실시간 push 없이도
     * eventually consistent하게 맞는 값을 보게 된다. 팀원이 몇 주씩 안 읽다가 한 번에 수천 건을
     * 읽는 극단적인 경우에 쿼리/브로드캐스트가 무제한으로 커지는 것도 이 캡으로 함께 막는다.
     */
    @Transactional
    public void markAsRead(Long teamId, Long memberId) {
        teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        TeamMember member = requireActiveMember(teamId, memberId);
        LocalDateTime unreadSince = member.getLastReadAt() != null ? member.getLastReadAt() : member.getJoinedAt();

        member.markRead(LocalDateTime.now());

        List<Message> latestFirst =
                messageRepository.findByTeamIdBeforeCursor(teamId, null, PageRequest.of(0, DEFAULT_MESSAGE_PAGE_SIZE));
        List<Message> newlyRead = latestFirst.stream()
                .filter(message -> message.getCreatedAt().isAfter(unreadSince))
                .toList();
        if (newlyRead.isEmpty()) {
            return;
        }
        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);
        MessageUnreadUpdateResponse update = ChatConverter.toMessageUnreadUpdateResponse(newlyRead, activeMembers);
        messagingTemplate.convertAndSend(TEAM_TOPIC_PREFIX + teamId + TEAM_READ_UPDATES_TOPIC_SUFFIX, update);
    }

    /** 채팅방 나가기/챗봇 on-off 등 다른 도메인 서비스가 시스템 안내 메시지를 남길 때 사용한다. */
    @Transactional
    public void postSystemMessage(Team team, String content) {
        Message saved = messageRepository.save(ChatConverter.toSystemMessage(team, content));
        broadcast(team.getTeamId(), saved);
    }

    /**
     * 챗봇 상태머신(ChatbotOrchestrationService)이 대화형 안내 메시지를 남길 때 사용한다.
     * {@code Team.chatbotEnabled}가 꺼져있으면(챗봇 삭제) 아무 메시지도 남기지 않는다 — 상태
     * 전이 자체는 그대로 진행되고, 그 사실을 알리는 챗봇 메시지만 조용히 생략된다.
     */
    @Transactional
    public void postChatbotMessage(Team team, String content) {
        if (!team.isChatbotEnabled()) {
            return;
        }
        Message saved = messageRepository.save(ChatConverter.toChatbotMessage(team, content));
        broadcast(team.getTeamId(), saved);
    }

    /** 팀장 투표 카드 등 metadata가 필요한 챗봇 카드형 메시지를 남길 때 사용한다. 동작은 {@link #postChatbotMessage}와 동일. */
    @Transactional
    public void postChatbotCardMessage(Team team, MessageType messageType, String content, String metadata) {
        if (!team.isChatbotEnabled()) {
            return;
        }
        Message saved =
                messageRepository.save(ChatConverter.toChatbotCardMessage(team, messageType, content, metadata));
        broadcast(team.getTeamId(), saved);
    }

    // 저장된 메시지를 구독 중인 클라이언트에게 실시간으로 내려준다.
    private MessageItemResponse broadcast(Long teamId, Message saved) {
        TeamMember sender = saved.getSenderTeamMember();
        MemberAvatarResponse avatar = sender != null
                ? characterService
                        .findAvatarsByMembers(List.of(sender.getMember()))
                        .get(sender.getMember().getMemberId())
                : null;
        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);
        long unreadCount = ChatConverter.countUnreadMembers(saved, activeMembers);

        MessageItemResponse response = ChatConverter.toMessageItemResponse(saved, avatar, unreadCount);
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
