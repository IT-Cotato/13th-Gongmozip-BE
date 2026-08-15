package org.cotato.gongmozip.domains.team.converter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.MemberAvatarResponse;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.cotato.gongmozip.domains.team.dto.request.TeamRequest.TeamMemberInput;
import org.cotato.gongmozip.domains.team.dto.response.TeamResponse.ChatRoomListResponse;
import org.cotato.gongmozip.domains.team.dto.response.TeamResponse.ChatRoomSummaryResponse;
import org.cotato.gongmozip.domains.team.dto.response.TeamResponse.TeamMemberSummaryResponse;
import org.cotato.gongmozip.domains.team.dto.response.TeamResponse.TeamMembersResponse;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.LeaderSelectionMode;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.global.ai.dto.LeaderCandidateSnapshot;

public final class TeamConverter {

    private TeamConverter() {}

    public static Team toTeam(InterestCategory preferredCategory, LeaderSelectionMode leaderSelectionMode) {
        return Team.builder()
                .status(TeamStatus.MATCHED)
                .preferredCategory(preferredCategory)
                .leaderSelectionMode(leaderSelectionMode)
                .chatbotEnabled(true)
                .submitted(false)
                .build();
    }

    /**
     * 팀원들의 매칭 신청 시점 팀장 희망 여부(WANTS 응답 수)로 팀장 선출 경로를 판정한다.
     * (docs/decisions/02-leader-election.md 케이스①②③ 참고)
     */
    public static LeaderSelectionMode determineLeaderSelectionMode(List<TeamMemberInput> members) {
        long wantsCount = members.stream()
                .filter(input -> input.leaderPreference() == LeaderPreference.WANTS)
                .count();
        if (wantsCount == 1) {
            return LeaderSelectionMode.AUTO_ASSIGNED;
        }
        if (wantsCount >= 2) {
            return LeaderSelectionMode.CANDIDATE_VOTE;
        }
        return LeaderSelectionMode.OPEN_NOMINATION;
    }

    public static TeamMember toTeamMember(
            Team team,
            Member member,
            Profile profile,
            LeaderPreference leaderPreference,
            ExtroversionType extroversionType,
            BigDecimal extroversionScore,
            LocalDateTime joinedAt) {
        return TeamMember.builder()
                .team(team)
                .member(member)
                .profile(profile)
                .leaderPreference(leaderPreference)
                .extroversionType(extroversionType)
                .extroversionScore(extroversionScore)
                // 팀장 희망("네")을 답한 사람만 사전 후보로 스냅샷한다. CANDIDATE_VOTE 경로에서
                // 후보 지정에 쓰이고, AUTO_ASSIGNED에서는 유일한 WANTS 응답자를 가리킨다.
                .isPreLeaderCandidate(leaderPreference == LeaderPreference.WANTS)
                .joinedAt(joinedAt)
                .build();
    }

    /** 팀장 추천 규칙기반 알고리즘(AiClient)에 넘길 팀원 스냅샷으로 변환한다. */
    public static LeaderCandidateSnapshot toLeaderCandidateSnapshot(TeamMember teamMember) {
        return new LeaderCandidateSnapshot(
                teamMember.getTeamMemberId(),
                teamMember.getLeaderPreference(),
                teamMember.getExtroversionType(),
                teamMember.getExtroversionScore());
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
            List<MemberAvatarResponse> avatars,
            String lastMessageContent,
            LocalDateTime lastMessageAt,
            long unreadCount) {
        return new ChatRoomSummaryResponse(
                teamId, roomTitle, participantCount, avatars, lastMessageContent, lastMessageAt, unreadCount);
    }

    public static ChatRoomListResponse toChatRoomListResponse(List<ChatRoomSummaryResponse> rooms) {
        return new ChatRoomListResponse(rooms);
    }

    public static TeamMemberSummaryResponse toTeamMemberSummaryResponse(
            TeamMember teamMember, boolean isMe, MemberAvatarResponse avatar) {
        return new TeamMemberSummaryResponse(
                teamMember.getTeamMemberId(),
                teamMember.getMember().getMemberId(),
                teamMember.getProfile().getProfileId(),
                teamMember.getProfile().getNickname(),
                teamMember.getRole().name(),
                isMe,
                avatar);
    }

    public static TeamMembersResponse toTeamMembersResponse(
            List<TeamMemberSummaryResponse> members,
            boolean chatbotEnabled,
            TeamStatus status,
            LocalDateTime leaderCandidacyDeadlineAt,
            LocalDateTime leaderVoteDeadlineAt,
            LocalDateTime contestCandidateDeadlineAt) {
        return new TeamMembersResponse(
                members,
                chatbotEnabled,
                members.size(),
                status.name(),
                leaderCandidacyDeadlineAt,
                leaderVoteDeadlineAt,
                contestCandidateDeadlineAt);
    }
}
