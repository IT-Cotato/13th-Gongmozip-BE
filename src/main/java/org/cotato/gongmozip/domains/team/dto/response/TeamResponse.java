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
            List<TeamMemberSummaryResponse> members,
            boolean chatbotEnabled,
            int participantCount,
            String status,
            // LEADER_SELECTING 중 "팀장 후보 등록(팀장 여부 투표)" 마감 시각. 후보 등록이 끝나기
            // 전까지만 의미가 있고, 그 외에는 null (2026-08-15 갱신 — 투표 마감과 분리).
            LocalDateTime leaderCandidacyDeadlineAt,
            // LEADER_SELECTING 중 "팀장 투표" 마감 시각. 후보 등록이 끝나 투표가 시작된 뒤에만
            // 세팅되고(동률 재투표 때마다 갱신), 그 외에는 null.
            LocalDateTime leaderVoteDeadlineAt,
            // CONTEST_SELECTING(공모전 후보/투표) 마감 시각. 그 외 상태에서는 null.
            LocalDateTime contestCandidateDeadlineAt) {}
}
