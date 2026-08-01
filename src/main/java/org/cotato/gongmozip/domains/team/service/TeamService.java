package org.cotato.gongmozip.domains.team.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.MemberAvatarResponse;
import org.cotato.gongmozip.domains.character.service.CharacterService;
import org.cotato.gongmozip.domains.chat.entity.Message;
import org.cotato.gongmozip.domains.chat.repository.MessageRepository;
import org.cotato.gongmozip.domains.chat.service.ChatService;
import org.cotato.gongmozip.domains.chatbot.service.ChatbotOrchestrationService;
import org.cotato.gongmozip.domains.collaboration.enums.CollaborationPointReason;
import org.cotato.gongmozip.domains.collaboration.service.CollaborationPointService;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.exception.ProfileException;
import org.cotato.gongmozip.domains.profile.exception.codes.ProfileErrorCode;
import org.cotato.gongmozip.domains.profile.repository.ProfileRepository;
import org.cotato.gongmozip.domains.team.converter.TeamConverter;
import org.cotato.gongmozip.domains.team.dto.request.TeamRequest.TeamCreationRequest;
import org.cotato.gongmozip.domains.team.dto.request.TeamRequest.TeamMemberInput;
import org.cotato.gongmozip.domains.team.dto.response.TeamResponse.ChatRoomListResponse;
import org.cotato.gongmozip.domains.team.dto.response.TeamResponse.ChatRoomSummaryResponse;
import org.cotato.gongmozip.domains.team.dto.response.TeamResponse.TeamMemberSummaryResponse;
import org.cotato.gongmozip.domains.team.dto.response.TeamResponse.TeamMembersResponse;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.exception.TeamException;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MemberRepository memberRepository;
    private final ProfileRepository profileRepository;
    private final MessageRepository messageRepository;
    private final ChatService chatService;
    private final CollaborationPointService collaborationPointService;
    private final ChatbotOrchestrationService chatbotOrchestrationService;
    private final CharacterService characterService;

    /**
     * 매칭 도메인이 팀 그룹핑 결과를 확정한 뒤 호출하는 내부 계약.
     * HTTP로 노출되지 않는다 (docs/decisions/01-team.md 참고).
     */
    @Transactional
    public Team createTeam(TeamCreationRequest request) {
        List<TeamMemberInput> members = request.members();
        if (members == null || members.isEmpty()) {
            throw new TeamException(TeamErrorCode.EMPTY_TEAM_MEMBER_LIST);
        }
        long distinctMemberCount =
                members.stream().map(TeamMemberInput::memberId).distinct().count();
        if (distinctMemberCount != members.size()) {
            throw new TeamException(TeamErrorCode.DUPLICATE_TEAM_MEMBER_INPUT);
        }

        Team team = teamRepository.save(TeamConverter.toTeam(request.preferredCategory()));

        LocalDateTime joinedAt = LocalDateTime.now();
        for (TeamMemberInput input : members) {
            Member member = memberRepository
                    .findById(input.memberId())
                    .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
            Profile profile = profileRepository
                    .findById(input.profileId())
                    .orElseThrow(() -> new ProfileException(ProfileErrorCode.PROFILE_NOT_FOUND));
            teamMemberRepository.save(TeamConverter.toTeamMember(team, member, profile, joinedAt));
        }

        chatbotOrchestrationService.startGreeting(team);
        return team;
    }

    /**
     * 채팅방 목록 조회. 방 개수(N)만큼 반복 쿼리하던 걸(팀원 목록, 마지막 메시지) 배치 쿼리 2개로
     * 묶었다 — 안 읽은 메시지 개수만 팀마다 임계값(lastReadAt)이 달라 배치가 까다로워 그대로 뒀다
     * (가벼운 COUNT 쿼리라 N개 남아도 영향은 작음).
     */
    public ChatRoomListResponse getMyChatRooms(Long memberId) {
        List<TeamMember> myMemberships =
                teamMemberRepository.findByMemberIdAndStatus(memberId, TeamMemberStatus.ACTIVE);
        List<Long> teamIds =
                myMemberships.stream().map(tm -> tm.getTeam().getTeamId()).toList();
        if (teamIds.isEmpty()) {
            return TeamConverter.toChatRoomListResponse(List.of());
        }

        Map<Long, List<TeamMember>> activeMembersByTeamId =
                teamMemberRepository.findByTeamIdInAndStatus(teamIds, TeamMemberStatus.ACTIVE).stream()
                        .collect(Collectors.groupingBy(tm -> tm.getTeam().getTeamId()));
        Map<Long, Message> lastMessageByTeamId = messageRepository.findLatestMessagePerTeam(teamIds).stream()
                .collect(Collectors.toMap(
                        message -> message.getTeam().getTeamId(), message -> message, (first, second) -> first));
        // 아바타는 방마다 따로 조회하지 않고, 내가 속한 모든 방의 팀원을 한 번에 모아 배치 조회한다.
        Map<Long, MemberAvatarResponse> avatarsByMemberId =
                characterService.findAvatarsByMembers(activeMembersByTeamId.values().stream()
                        .flatMap(List::stream)
                        .map(TeamMember::getMember)
                        .distinct()
                        .toList());

        List<ChatRoomSummaryResponse> rooms = myMemberships.stream()
                .map(myMembership -> {
                    Long teamId = myMembership.getTeam().getTeamId();
                    List<TeamMember> activeMembers = activeMembersByTeamId.getOrDefault(teamId, List.of());
                    List<TeamMember> others = activeMembers.stream()
                            .filter(other -> !other.getMember().getMemberId().equals(memberId))
                            .toList();
                    String roomTitle = TeamConverter.buildRoomTitle(others);
                    List<MemberAvatarResponse> avatars = others.stream()
                            .map(other ->
                                    avatarsByMemberId.get(other.getMember().getMemberId()))
                            .filter(Objects::nonNull)
                            .toList();

                    Message lastMessage = lastMessageByTeamId.get(teamId);
                    LocalDateTime unreadSince = myMembership.getLastReadAt() != null
                            ? myMembership.getLastReadAt()
                            : myMembership.getJoinedAt();
                    long unreadCount = messageRepository.countByTeam_TeamIdAndCreatedAtAfter(teamId, unreadSince);

                    return TeamConverter.toChatRoomSummaryResponse(
                            teamId,
                            roomTitle,
                            activeMembers.size(),
                            avatars,
                            lastMessage != null ? lastMessage.getContent() : null,
                            lastMessage != null ? lastMessage.getCreatedAt() : null,
                            unreadCount);
                })
                .toList();

        return TeamConverter.toChatRoomListResponse(rooms);
    }

    @Transactional
    public void leaveTeam(Long teamId, Long memberId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        TeamMember teamMember = teamMemberRepository
                .findByTeam_TeamIdAndMember_MemberId(teamId, memberId)
                .filter(member -> member.getStatus() == TeamMemberStatus.ACTIVE)
                .orElseThrow(() -> new TeamException(TeamErrorCode.NOT_TEAM_MEMBER));

        teamMember.leave(LocalDateTime.now());
        collaborationPointService.awardPoint(teamMember.getMember(), team, CollaborationPointReason.LEAVE_PENALTY);
        chatService.postSystemMessage(team, teamMember.getProfile().getNickname() + "님이 채팅방을 나갔습니다.");
    }

    @Transactional
    public void toggleChatbot(Long teamId, Long memberId, boolean enabled) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        TeamMember teamMember = teamMemberRepository
                .findByTeam_TeamIdAndMember_MemberId(teamId, memberId)
                .filter(member -> member.getStatus() == TeamMemberStatus.ACTIVE)
                .orElseThrow(() -> new TeamException(TeamErrorCode.NOT_TEAM_MEMBER));

        team.setChatbotEnabled(enabled);
        String action = enabled ? "추가" : "제거";
        chatService.postSystemMessage(team, teamMember.getProfile().getNickname() + "님이 챗봇을 " + action + "했습니다.");
    }

    public TeamMembersResponse getTeamMembers(Long teamId, Long requesterMemberId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));

        teamMemberRepository
                .findByTeam_TeamIdAndMember_MemberId(teamId, requesterMemberId)
                .filter(teamMember -> teamMember.getStatus() == TeamMemberStatus.ACTIVE)
                .orElseThrow(() -> new TeamException(TeamErrorCode.NOT_TEAM_MEMBER));

        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);
        Map<Long, MemberAvatarResponse> avatarsByMemberId = characterService.findAvatarsByMembers(
                activeMembers.stream().map(TeamMember::getMember).toList());
        List<TeamMemberSummaryResponse> memberResponses = activeMembers.stream()
                .map(teamMember -> TeamConverter.toTeamMemberSummaryResponse(
                        teamMember,
                        teamMember.getMember().getMemberId().equals(requesterMemberId),
                        avatarsByMemberId.get(teamMember.getMember().getMemberId())))
                .toList();

        return TeamConverter.toTeamMembersResponse(memberResponses, team.isChatbotEnabled());
    }
}
