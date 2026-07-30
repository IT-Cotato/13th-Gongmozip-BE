package org.cotato.gongmozip.domains.team.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public final class TeamResponse {

    private TeamResponse() {}

    public record ChatRoomSummaryResponse(
            Long teamId,
            String roomTitle,
            int participantCount,
            String lastMessageContent,
            LocalDateTime lastMessageAt,
            long unreadCount) {}

    public record ChatRoomListResponse(List<ChatRoomSummaryResponse> rooms) {}

    public record TeamMemberSummaryResponse(
            Long teamMemberId, Long memberId, String nickname, String role, boolean isMe) {}

    public record TeamMembersResponse(
            List<TeamMemberSummaryResponse> members, boolean chatbotEnabled, int participantCount) {}
}
