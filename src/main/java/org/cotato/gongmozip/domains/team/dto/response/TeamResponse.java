package org.cotato.gongmozip.domains.team.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.MemberAvatarResponse;

public final class TeamResponse {

    private TeamResponse() {}

    public record ChatRoomSummaryResponse(
            Long teamId,
            String roomTitle,
            int participantCount,
            List<MemberAvatarResponse> avatars,
            String lastMessageContent,
            LocalDateTime lastMessageAt,
            long unreadCount) {}

    public record ChatRoomListResponse(List<ChatRoomSummaryResponse> rooms) {}

    public record TeamMemberSummaryResponse(
            Long teamMemberId,
            Long memberId,
            Long profileId,
            String nickname,
            String role,
            boolean isMe,
            MemberAvatarResponse avatar) {}

    public record TeamMembersResponse(
            List<TeamMemberSummaryResponse> members, boolean chatbotEnabled, int participantCount, String status) {}
}
