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
import org.cotato.gongmozip.domains.notification.service.NotificationService;
import org.cotato.gongmozip.domains.notification.service.PushNotificationService;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.exception.TeamException;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.cotato.gongmozip.global.push.PushPayload;
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
    // Figma에 명시된 푸시 제목이 없어 임시로 고정 문구를 쓴다 — 추후 조정 대상(docs/decisions/13-fcm-push.md).
    private static final String PUSH_TITLE = "공모집";

    private final MessageRepository messageRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final CharacterService characterService;
    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;

    @Transactional
    public MessageItemResponse sendMessage(Long teamId, Long senderMemberId, SendMessageRequest request) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        TeamMember sender = requireActiveMember(teamId, senderMemberId);

        Message saved = messageRepository.save(ChatConverter.toMemberMessage(team, sender, request.content()));
        notifyOtherMembersPush(team, sender, request.content());
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
     *
     * <p>일반 텍스트 프롬프트(인사 유도, 진행 안내, "@챗봇" 자유질의 응답 등)는 알림함/푸시 대상이
     * 아니다 — 실사용 중 알림함이 너무 시끄러워졌다(특히 "@챗봇" 질의응답은 질문할 때마다 팀 전체에
     * 알림이 갔음). {@link #postChatbotCardMessage}의 카드형 메시지만 알림 대상으로 좁혔다
     * (docs/decisions/11-notification.md, 2026-08-21).
     */
    @Transactional
    public void postChatbotMessage(Team team, String content) {
        if (!team.isChatbotEnabled()) {
            return;
        }
        Message saved = messageRepository.save(ChatConverter.toChatbotMessage(team, content));
        broadcast(team.getTeamId(), saved);
    }

    /** 팀장 투표 카드 등 metadata가 필요한 챗봇 카드형 메시지를 남길 때 사용한다. 알림함/푸시 대상이다. 동작은 {@link #postChatbotMessage}와 동일. */
    @Transactional
    public void postChatbotCardMessage(Team team, MessageType messageType, String content, String metadata) {
        postChatbotCardMessage(team, messageType, content, metadata, true);
    }

    /**
     * {@code notify=false}로 호출하면 채팅에는 남기되 알림함/푸시는 건너뛴다 — 같은 카드가 반복
     * 발행되는 재알림(예: {@code TeamScheduleService.sendSubmissionCheckReminderForTeam}, "진행완료"
     * 미응답 시 2시간마다 반복)처럼, 매번 알림함/푸시로 나가면 스팸이 되는 경우에 쓴다
     * (docs/decisions/11-notification.md).
     */
    @Transactional
    public void postChatbotCardMessage(
            Team team, MessageType messageType, String content, String metadata, boolean notify) {
        if (!team.isChatbotEnabled()) {
            return;
        }
        Message saved =
                messageRepository.save(ChatConverter.toChatbotCardMessage(team, messageType, content, metadata));
        if (notify && messageType != MessageType.CHATBOT_GUIDE_CARD) {
            notifyActiveMembers(team, content);
        }
        broadcast(team.getTeamId(), saved);
    }

    // 챗봇 카드형 메시지(CHATBOT_GUIDE_CARD 제외)는 알림함(CHATROOM 카테고리)에도 활성 팀원 수만큼
    // 쌓인다(docs/decisions/11-notification.md). 일반 텍스트 프롬프트(postChatbotMessage), 다른
    // 팀원이 보낸 일반 텍스트 메시지(sendMessage), SYSTEM_NOTICE(postSystemMessage, 나가기/챗봇
    // 토글 안내)는 알림함 대상이 아니다. CHATBOT_GUIDE_CARD("활용 예시")는 카드지만 정적 안내라
    // 알림 가치가 낮아 제외했다.
    //
    // broadcast()(WebSocket 전송, 되돌릴 수 없음)보다 먼저 호출한다 — 알림 저장이 실패해 트랜잭션이
    // 롤백되더라도, 아직 아무 것도 브로드캐스트되지 않은 상태라 정합성이 깨지지 않는다.
    private void notifyActiveMembers(Team team, String content) {
        List<Member> activeMembers =
                teamMemberRepository.findByTeamIdAndStatus(team.getTeamId(), TeamMemberStatus.ACTIVE).stream()
                        .map(TeamMember::getMember)
                        .toList();
        notificationService.notifyChatroomEvent(activeMembers, team.getTeamId(), content);
    }

    // 다른 팀원의 일반 채팅 메시지는 알림함엔 안 쌓이지만(위 notifyActiveMembers 주석 참고) OS 푸시는
    // 나가야 한다(docs/decisions/13-fcm-push.md) — 원 요구사항이 "챗봇이든, 팝업이든, 다른 사람의
    // 메세지든" 전부 푸시가 가길 원했기 때문. 발신자 본인은 제외한다.
    private void notifyOtherMembersPush(Team team, TeamMember sender, String content) {
        Long senderMemberId = sender.getMember().getMemberId();
        List<Member> otherMembers =
                teamMemberRepository.findByTeamIdAndStatus(team.getTeamId(), TeamMemberStatus.ACTIVE).stream()
                        .map(TeamMember::getMember)
                        .filter(member -> !member.getMemberId().equals(senderMemberId))
                        .toList();
        pushNotificationService.sendToMembers(
                otherMembers, new PushPayload(PUSH_TITLE, content, Map.of("teamId", String.valueOf(team.getTeamId()))));
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
