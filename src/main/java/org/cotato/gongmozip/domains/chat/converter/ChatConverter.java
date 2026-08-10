package org.cotato.gongmozip.domains.chat.converter;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.MemberAvatarResponse;
import org.cotato.gongmozip.domains.chat.dto.response.ChatResponse.MessageItemResponse;
import org.cotato.gongmozip.domains.chat.dto.response.ChatResponse.MessageListResponse;
import org.cotato.gongmozip.domains.chat.dto.response.ChatResponse.MessageUnreadUpdate;
import org.cotato.gongmozip.domains.chat.dto.response.ChatResponse.MessageUnreadUpdateResponse;
import org.cotato.gongmozip.domains.chat.entity.Message;
import org.cotato.gongmozip.domains.chat.enums.MessageSenderType;
import org.cotato.gongmozip.domains.chat.enums.MessageType;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;

public final class ChatConverter {

    private ChatConverter() {}

    public static Message toMemberMessage(Team team, TeamMember sender, String content) {
        return Message.builder()
                .team(team)
                .senderType(MessageSenderType.MEMBER)
                .senderTeamMember(sender)
                .messageType(MessageType.TEXT)
                .content(content)
                .build();
    }

    public static Message toSystemMessage(Team team, String content) {
        return Message.builder()
                .team(team)
                .senderType(MessageSenderType.SYSTEM)
                .messageType(MessageType.SYSTEM_NOTICE)
                .content(content)
                .build();
    }

    public static Message toChatbotMessage(Team team, String content) {
        return toChatbotCardMessage(team, MessageType.TEXT, content, null);
    }

    public static Message toChatbotCardMessage(Team team, MessageType messageType, String content, String metadata) {
        return Message.builder()
                .team(team)
                .senderType(MessageSenderType.CHATBOT)
                .messageType(messageType)
                .content(content)
                .metadata(metadata)
                .build();
    }

    public static MessageItemResponse toMessageItemResponse(
            Message message, MemberAvatarResponse senderAvatar, long unreadCount) {
        TeamMember sender = message.getSenderTeamMember();
        return new MessageItemResponse(
                message.getMessageId(),
                message.getSenderType().name(),
                sender != null ? sender.getTeamMemberId() : null,
                sender != null ? sender.getProfile().getNickname() : null,
                message.getMessageType().name(),
                message.getContent(),
                message.getMetadata(),
                message.getCreatedAt(),
                senderAvatar,
                unreadCount);
    }

    // 리포지토리는 messageId 내림차순으로 조회하므로 화면 표시 순서(오래된 순)로 뒤집는다.
    // 정렬 기준을 리포지토리 쿼리(findByTeamIdBeforeCursor)와 동일하게 messageId로 맞춰야
    // cursor 페이지 경계에서 순서가 어긋나지 않는다.
    public static MessageListResponse toMessageListResponse(
            List<Message> latestFirstMessages,
            Map<Long, MemberAvatarResponse> avatarsByMemberId,
            List<TeamMember> activeMembers,
            boolean hasNext) {
        List<MessageItemResponse> chronological = latestFirstMessages.stream()
                .sorted(Comparator.comparing(Message::getMessageId))
                .map(message -> {
                    TeamMember sender = message.getSenderTeamMember();
                    MemberAvatarResponse avatar = sender != null
                            ? avatarsByMemberId.get(sender.getMember().getMemberId())
                            : null;
                    return toMessageItemResponse(message, avatar, countUnreadMembers(message, activeMembers));
                })
                .toList();
        return new MessageListResponse(chronological, hasNext);
    }

    /**
     * 메시지 하나를 아직 안 읽은 활성 팀원 수. 보낸 사람 본인은 항상 제외하고(자기 메시지를
     * "안읽음"으로 세지 않음), 나머지 활성 팀원 중 lastReadAt(없으면 joinedAt)이 메시지
     * createdAt보다 이전인 사람만 센다 — 채팅방 목록 unreadCount와 동일한 기준.
     */
    public static long countUnreadMembers(Message message, List<TeamMember> activeMembers) {
        TeamMember sender = message.getSenderTeamMember();
        // teamMemberId는 영속화 전까지 null이라(테스트에서는 끝까지 null) 대신 항상 채워지는
        // Member.memberId로 비교한다 — 한 팀에 같은 회원의 ACTIVE TeamMember는 최대 1개라
        // (uq_team_members_team_member) 식별자로 써도 안전하다.
        Long senderMemberId = sender != null ? sender.getMember().getMemberId() : null;
        LocalDateTime createdAt = message.getCreatedAt();
        return activeMembers.stream()
                .filter(member -> senderMemberId == null
                        || !member.getMember().getMemberId().equals(senderMemberId))
                .filter(member -> {
                    LocalDateTime unreadSince =
                            member.getLastReadAt() != null ? member.getLastReadAt() : member.getJoinedAt();
                    return unreadSince.isBefore(createdAt);
                })
                .count();
    }

    public static MessageUnreadUpdateResponse toMessageUnreadUpdateResponse(
            List<Message> messages, List<TeamMember> activeMembers) {
        List<MessageUnreadUpdate> updates = messages.stream()
                .map(message ->
                        new MessageUnreadUpdate(message.getMessageId(), countUnreadMembers(message, activeMembers)))
                .toList();
        return new MessageUnreadUpdateResponse(updates);
    }
}
