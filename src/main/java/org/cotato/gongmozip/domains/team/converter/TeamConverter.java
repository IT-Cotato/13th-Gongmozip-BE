package org.cotato.gongmozip.domains.team.converter;

import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.team.dto.response.TeamResponse.ChatRoomListResponse;
import org.cotato.gongmozip.domains.team.dto.response.TeamResponse.ChatRoomSummaryResponse;
import org.cotato.gongmozip.domains.team.dto.response.TeamResponse.TeamMemberSummaryResponse;
import org.cotato.gongmozip.domains.team.dto.response.TeamResponse.TeamMembersResponse;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.LeaderSelectionMode;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;

public final class TeamConverter {

    private TeamConverter() {}

    public static Team toTeam(InterestCategory preferredCategory) {
        return Team.builder()
                .status(TeamStatus.MATCHED)
                .preferredCategory(preferredCategory)
                // 팀장 희망 점수 데이터가 아직 없어 항상 OPEN_NOMINATION으로 고정한다.
                // docs/decisions/02-leader-election.md 참고.
                .leaderSelectionMode(LeaderSelectionMode.OPEN_NOMINATION)
                .chatbotEnabled(true)
                .submitted(false)
                .build();
    }

    public static TeamMember toTeamMember(Team team, Member member, Profile profile, LocalDateTime joinedAt) {
        return TeamMember.builder()
                .team(team)
                .member(member)
                .profile(profile)
                // 사전 팀장 후보 스냅샷도 데이터 부재로 항상 false로 고정한다.
                .isPreLeaderCandidate(false)
                .joinedAt(joinedAt)
                .build();
    }

    public static String buildRoomTitle(List<TeamMember> othersExcludingViewer) {
        return othersExcludingViewer.stream()
                .map(tm -> tm.getProfile().getNickname())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    public static ChatRoomSummaryResponse toChatRoomSummaryResponse(
            Long teamId,
            String roomTitle,
            int participantCount,
            String lastMessageContent,
            LocalDateTime lastMessageAt,
            long unreadCount) {
        return new ChatRoomSummaryResponse(
                teamId, roomTitle, participantCount, lastMessageContent, lastMessageAt, unreadCount);
    }

    public static ChatRoomListResponse toChatRoomListResponse(List<ChatRoomSummaryResponse> rooms) {
        return new ChatRoomListResponse(rooms);
    }

    public static TeamMemberSummaryResponse toTeamMemberSummaryResponse(TeamMember teamMember, boolean isMe) {
        return new TeamMemberSummaryResponse(
                teamMember.getTeamMemberId(),
                teamMember.getMember().getMemberId(),
                teamMember.getProfile().getNickname(),
                teamMember.getRole().name(),
                isMe);
    }

    public static TeamMembersResponse toTeamMembersResponse(
            List<TeamMemberSummaryResponse> members, boolean chatbotEnabled) {
        return new TeamMembersResponse(members, chatbotEnabled, members.size());
    }
}
