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

    // "팀장 여부 투표"(candidacy) 진행 상황. ContestResponse.ContestVoteStatusResponse와 동일한
    // 목적(내가 이미 응답했는지로 프론트가 버튼을 비활성화) — 후보 목록 없이 응답 여부만
    // 다루므로 tally 리스트는 없다.
    public record LeaderCandidacyStatusResponse(
            int requiredResponderCount, long respondedCount, boolean myResponded, String myCandidacy) {}

    // 팀장 후보 투표 진행 상황. ContestResponse.ContestVoteStatusResponse와 동일한 모양 —
    // round는 동률 재투표마다 늘어난다.
    public record LeaderVoteStatusResponse(
            int round,
            int requiredVoterCount,
            long participatedVoterCount,
            boolean myVoted,
            List<LeaderVoteTallyItemResponse> results) {}

    // 후보 이름/아바타는 프론트가 이미 GET .../members로 갖고 있으므로 여기서는 중복 없이
    // teamMemberId + 득표수만 내려준다.
    public record LeaderVoteTallyItemResponse(Long candidateTeamMemberId, long voteCount) {}
}
