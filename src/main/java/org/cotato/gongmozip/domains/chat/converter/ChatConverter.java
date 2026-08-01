package org.cotato.gongmozip.domains.chat.converter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.MemberAvatarResponse;
import org.cotato.gongmozip.domains.chat.dto.response.ChatResponse.MessageItemResponse;
import org.cotato.gongmozip.domains.chat.dto.response.ChatResponse.MessageListResponse;
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

    public static MessageItemResponse toMessageItemResponse(Message message, MemberAvatarResponse senderAvatar) {
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
                senderAvatar);
    }

    // 리포지토리는 최신순(DESC)으로 조회하므로 화면 표시 순서(오래된 순)로 뒤집는다.
    public static MessageListResponse toMessageListResponse(
            List<Message> latestFirstMessages, Map<Long, MemberAvatarResponse> avatarsByMemberId) {
        List<MessageItemResponse> chronological = latestFirstMessages.stream()
                .sorted(Comparator.comparing(Message::getCreatedAt))
                .map(message -> {
                    TeamMember sender = message.getSenderTeamMember();
                    MemberAvatarResponse avatar = sender != null
                            ? avatarsByMemberId.get(sender.getMember().getMemberId())
                            : null;
                    return toMessageItemResponse(message, avatar);
                })
                .toList();
        return new MessageListResponse(chronological);
    }
}
