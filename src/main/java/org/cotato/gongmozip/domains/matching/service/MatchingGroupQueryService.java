package org.cotato.gongmozip.domains.matching.service;

import static org.cotato.gongmozip.domains.matching.dto.response.MatchingGroupResponse.GroupMemberResponse;
import static org.cotato.gongmozip.domains.matching.dto.response.MatchingGroupResponse.GroupResponsesResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroup;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroupMember;
import org.cotato.gongmozip.domains.matching.exception.MatchingException;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupMemberRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 그룹 구성원에게만 공개 이후의 응답 현황과 최종 Team 연결을 제공하는 읽기 전용 서비스다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchingGroupQueryService {

    private final MatchingGroupRepository matchingGroupRepository;
    private final MatchingGroupMemberRepository matchingGroupMemberRepository;
    private final MatchingTimePolicy matchingTimePolicy;

    public GroupResponsesResponse getResponses(Long memberId, Long groupId) {
        MatchingGroup group = matchingGroupRepository
                .findById(groupId)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.MATCHING_GROUP_NOT_FOUND));

        // 팀원 목록이나 프로필을 조회하기 전에 소속을 먼저 확인해, 타인이 그룹 ID만 알아도 구성원
        // 정보가 영속성 컨텍스트에 로딩되거나 응답으로 노출되는 일을 막는다.
        matchingGroupMemberRepository
                .findByMatchingGroup_MatchingGroupIdAndMember_MemberId(groupId, memberId)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.MATCHING_GROUP_ACCESS_DENIED));
        LocalDate applicationDate = resolveApplicationDate(group);
        LocalDateTime publishedAt = matchingTimePolicy.resultPublishAt(applicationDate);

        // 결과가 DB에 먼저 저장됐더라도 publishedAt 전에는 그룹원 닉네임과 응답 상태를 공개하지 않는다.
        if (!matchingTimePolicy.isResultPublished(applicationDate, matchingTimePolicy.now())) {
            throw new MatchingException(MatchingErrorCode.MATCHING_RESULT_NOT_PUBLISHED);
        }

        List<MatchingGroupMember> members = matchingGroupMemberRepository.findResultMembers(group);

        // 최초 제안 인원과 별도로 아직 Team 후보인 PENDING + ACCEPTED 인원을 보여준다.
        // 4인 제안에서 한 명이 패스했다면 proposedTeamSize=4, activeMemberCount=3이 된다.
        long activeMemberCount = members.stream()
                .filter(MatchingGroupMember::isActiveResponseTarget)
                .count();
        Long teamId = group.getTeam() == null ? null : group.getTeam().getTeamId();
        return new GroupResponsesResponse(
                group.getMatchingGroupId(),
                group.getStatus(),
                group.getTeamSize(),
                activeMemberCount,
                group.getConfirmedTeamSize(),
                publishedAt,
                group.getResponseDeadlineAt(),
                teamId,
                members.stream().map(member -> toMember(memberId, member)).toList());
    }

    private LocalDate resolveApplicationDate(MatchingGroup group) {
        if (group.getMatchingBatch() == null || group.getMatchingBatch().getApplicationDate() == null) {
            throw new MatchingException(MatchingErrorCode.MATCHING_RESULT_NOT_PUBLISHED);
        }
        return group.getMatchingBatch().getApplicationDate();
    }

    private GroupMemberResponse toMember(Long requesterMemberId, MatchingGroupMember groupMember) {
        return new GroupMemberResponse(
                groupMember.getMember().getMemberId(),
                groupMember.getMatchingApplication().getProfile().getProfileId(),
                groupMember.getMatchingApplication().getProfile().getNickname(),
                groupMember.getResponseStatus(),
                groupMember.getRespondedAt(),
                requesterMemberId.equals(groupMember.getMember().getMemberId()));
    }
}
