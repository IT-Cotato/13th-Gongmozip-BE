package org.cotato.gongmozip.domains.matching.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupMemberStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupStatus;

public final class MatchingGroupResponse {

    private MatchingGroupResponse() {}

    public record GroupResponsesResponse(
            Long matchingGroupId,
            MatchingGroupStatus groupStatus,
            int proposedTeamSize,
            long activeMemberCount,
            Integer confirmedTeamSize,
            LocalDateTime publishedAt,
            LocalDateTime responseDeadlineAt,
            Long teamId,
            List<GroupMemberResponse> members) {

        public GroupResponsesResponse {
            members = members == null ? List.of() : List.copyOf(members);
        }
    }

    public record GroupMemberResponse(
            Long memberId,
            Long profileId,
            String nickname,
            MatchingGroupMemberStatus responseStatus,
            LocalDateTime respondedAt,
            boolean me) {}

    public record AcceptResponse(
            Long matchingGroupId,
            MatchingGroupStatus groupStatus,
            MatchingGroupMemberStatus myResponseStatus,
            Integer confirmedTeamSize,
            Long teamId) {}
}
