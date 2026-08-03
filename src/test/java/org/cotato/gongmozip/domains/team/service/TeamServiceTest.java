package org.cotato.gongmozip.domains.team.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.MemberAvatarResponse;
import org.cotato.gongmozip.domains.character.enums.CharacterPalette;
import org.cotato.gongmozip.domains.character.service.CharacterService;
import org.cotato.gongmozip.domains.chat.entity.Message;
import org.cotato.gongmozip.domains.chat.repository.MessageRepository;
import org.cotato.gongmozip.domains.chat.service.ChatService;
import org.cotato.gongmozip.domains.chatbot.service.ChatbotOrchestrationService;
import org.cotato.gongmozip.domains.collaboration.enums.CollaborationPointReason;
import org.cotato.gongmozip.domains.collaboration.service.CollaborationPointService;
import org.cotato.gongmozip.domains.contest.service.ContestVotingService;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.profile.repository.ProfileRepository;
import org.cotato.gongmozip.domains.review.service.ReviewService;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
import org.cotato.gongmozip.domains.team.dto.request.TeamRequest.TeamCreationRequest;
import org.cotato.gongmozip.domains.team.dto.request.TeamRequest.TeamMemberInput;
import org.cotato.gongmozip.domains.team.dto.response.TeamResponse.ChatRoomListResponse;
import org.cotato.gongmozip.domains.team.dto.response.TeamResponse.ChatRoomSummaryResponse;
import org.cotato.gongmozip.domains.team.dto.response.TeamResponse.TeamMembersResponse;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.ChatRoomSortType;
import org.cotato.gongmozip.domains.team.enums.LeaderSelectionMode;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.enums.TeamRole;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.domains.team.exception.TeamException;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatService chatService;

    @Mock
    private CollaborationPointService collaborationPointService;

    @Mock
    private ChatbotOrchestrationService chatbotOrchestrationService;

    @Mock
    private CharacterService characterService;

    @Mock
    private LeaderElectionService leaderElectionService;

    @Mock
    private ContestVotingService contestVotingService;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private TeamService teamService;

    @DisplayName("정상적인 입력으로 팀을 생성하면 팀원 수만큼 TeamMember가 저장된다.")
    @Test
    void 정상적인_입력으로_팀을_생성하면_팀원_수만큼_TeamMember가_저장된다() {
        // given
        Member member1 = Member.builder().memberId(1L).email("a@gongmozip.com").build();
        Member member2 = Member.builder().memberId(2L).email("b@gongmozip.com").build();
        Profile profile1 = Profile.builder().profileId(10L).nickname("김민정").build();
        Profile profile2 = Profile.builder().profileId(20L).nickname("이해은").build();

        TeamCreationRequest request = new TeamCreationRequest(
                List.of(new TeamMemberInput(1L, 10L), new TeamMemberInput(2L, 20L)), InterestCategory.IT_AI_TECH);

        given(teamRepository.save(any(Team.class))).willAnswer(inv -> inv.getArgument(0));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member1));
        given(memberRepository.findById(2L)).willReturn(Optional.of(member2));
        given(profileRepository.findById(10L)).willReturn(Optional.of(profile1));
        given(profileRepository.findById(20L)).willReturn(Optional.of(profile2));
        given(teamMemberRepository.save(any(TeamMember.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        Team team = teamService.createTeam(request);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.MATCHED);
        assertThat(team.getLeaderSelectionMode()).isEqualTo(LeaderSelectionMode.OPEN_NOMINATION);
        assertThat(team.isChatbotEnabled()).isTrue();
        verify(teamMemberRepository, times(2)).save(any(TeamMember.class));
        verify(chatbotOrchestrationService).startGreeting(team);
    }

    @DisplayName("팀원이 없으면 팀 생성에 실패한다.")
    @Test
    void 팀원이_없으면_팀_생성에_실패한다() {
        // given
        TeamCreationRequest request = new TeamCreationRequest(List.of(), InterestCategory.IT_AI_TECH);

        // when & then
        assertThatThrownBy(() -> teamService.createTeam(request))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.EMPTY_TEAM_MEMBER_LIST);
    }

    @DisplayName("중복된 회원이 포함되면 팀 생성에 실패한다.")
    @Test
    void 중복된_회원이_포함되면_팀_생성에_실패한다() {
        // given
        TeamCreationRequest request = new TeamCreationRequest(
                List.of(new TeamMemberInput(1L, 10L), new TeamMemberInput(1L, 11L)), InterestCategory.IT_AI_TECH);

        // when & then
        assertThatThrownBy(() -> teamService.createTeam(request))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.DUPLICATE_TEAM_MEMBER_INPUT);
    }

    @DisplayName("내 채팅방 목록을 조회하면 본인을 제외한 팀원 닉네임으로 방 제목이 구성된다.")
    @Test
    void 내_채팅방_목록을_조회하면_본인을_제외한_팀원_닉네임으로_방_제목이_구성된다() {
        // given
        Team team = Team.builder().teamId(100L).build();
        TeamMember me = teamMemberOf(team, 1L, "나");
        TeamMember other1 = teamMemberOf(team, 2L, "김민정");
        TeamMember other2 = teamMemberOf(team, 3L, "이해은");

        given(teamMemberRepository.findByMemberIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(me));
        given(teamMemberRepository.findByTeamIdInAndStatus(List.of(100L), TeamMemberStatus.ACTIVE))
                .willReturn(List.of(me, other1, other2));
        given(messageRepository.findLatestMessagePerTeam(List.of(100L))).willReturn(List.of());
        given(messageRepository.countByTeam_TeamIdAndCreatedAtAfter(any(Long.class), any()))
                .willReturn(0L);
        given(characterService.findAvatarsByMembers(any())).willReturn(Map.of());

        // when
        ChatRoomListResponse response = teamService.getMyChatRooms(1L, ChatRoomSortType.LATEST);

        // then
        assertThat(response.rooms()).hasSize(1);
        assertThat(response.rooms().get(0).roomTitle()).isEqualTo("김민정, 이해은");
        assertThat(response.rooms().get(0).participantCount()).isEqualTo(3);
    }

    @DisplayName("채팅방 목록의 아바타는 본인을 제외한 팀원 것만, 캐릭터가 없는 팀원은 빠진 채 채워진다.")
    @Test
    void 채팅방_목록의_아바타는_본인을_제외한_팀원_것만_채워진다() {
        // given
        Team team = Team.builder().teamId(100L).build();
        TeamMember me = teamMemberOf(team, 1L, "나");
        TeamMember other1 = teamMemberOf(team, 2L, "김민정");
        TeamMember other2 = teamMemberOf(team, 3L, "이해은");
        MemberAvatarResponse avatar =
                new MemberAvatarResponse(2L, CharacterType.TRACK_RUNNER, CharacterPalette.SOLID_PINK, "#FFE9E7", null);

        given(teamMemberRepository.findByMemberIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(me));
        given(teamMemberRepository.findByTeamIdInAndStatus(List.of(100L), TeamMemberStatus.ACTIVE))
                .willReturn(List.of(me, other1, other2));
        given(messageRepository.findLatestMessagePerTeam(List.of(100L))).willReturn(List.of());
        given(messageRepository.countByTeam_TeamIdAndCreatedAtAfter(any(Long.class), any()))
                .willReturn(0L);
        // 3L(이해은)은 협업 유형 검사를 안 했다고 가정 — 결과 Map에 없음
        given(characterService.findAvatarsByMembers(any())).willReturn(Map.of(2L, avatar));

        // when
        ChatRoomListResponse response = teamService.getMyChatRooms(1L, ChatRoomSortType.LATEST);

        // then
        assertThat(response.rooms().get(0).avatars()).containsExactly(avatar);
    }

    @DisplayName("최신 메시지 순으로 조회하면 마지막 메시지가 더 최근인 방이 먼저 온다.")
    @Test
    void 최신_메시지_순으로_조회하면_마지막_메시지가_더_최근인_방이_먼저_온다() {
        // given
        Team teamA = Team.builder().teamId(100L).build();
        Team teamB = Team.builder().teamId(200L).build();
        TeamMember meInA = teamMemberOf(teamA, 1L, "나");
        TeamMember meInB = teamMemberOf(teamB, 1L, "나");

        given(teamMemberRepository.findByMemberIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(meInA, meInB));
        given(teamMemberRepository.findByTeamIdInAndStatus(List.of(100L, 200L), TeamMemberStatus.ACTIVE))
                .willReturn(List.of(meInA, meInB));

        Message olderMessage = Message.builder().team(teamA).content("오래된 메시지").build();
        ReflectionTestUtils.setField(
                olderMessage, "createdAt", LocalDateTime.now().minusHours(2));
        Message newerMessage = Message.builder().team(teamB).content("최근 메시지").build();
        ReflectionTestUtils.setField(newerMessage, "createdAt", LocalDateTime.now());

        given(messageRepository.findLatestMessagePerTeam(List.of(100L, 200L)))
                .willReturn(List.of(olderMessage, newerMessage));
        given(messageRepository.countByTeam_TeamIdAndCreatedAtAfter(any(Long.class), any()))
                .willReturn(0L);
        given(characterService.findAvatarsByMembers(any())).willReturn(Map.of());

        // when
        ChatRoomListResponse response = teamService.getMyChatRooms(1L, ChatRoomSortType.LATEST);

        // then
        assertThat(response.rooms()).extracting(ChatRoomSummaryResponse::teamId).containsExactly(200L, 100L);
    }

    @DisplayName("안읽은 메시지 순으로 조회하면, 개수와 무관하게 안읽은 방이 먼저 모이고(그 안에서는 최신 메시지 순), 다 읽은 방이 그다음(역시 최신 메시지 순)으로 온다.")
    @Test
    void 안읽은_메시지_순으로_조회하면_안읽은_방이_먼저_모이고_그_안에서는_최신_메시지_순으로_온다() {
        // given
        Team teamA = Team.builder().teamId(100L).build(); // 안읽음 1건, 오래된 메시지
        Team teamB = Team.builder().teamId(200L).build(); // 다 읽음, 가장 최신 메시지
        Team teamC = Team.builder().teamId(300L).build(); // 안읽음 3건, 안읽은 방 중 가장 최신
        TeamMember meInA = teamMemberOf(teamA, 1L, "나");
        TeamMember meInB = teamMemberOf(teamB, 1L, "나");
        TeamMember meInC = teamMemberOf(teamC, 1L, "나");

        given(teamMemberRepository.findByMemberIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(meInA, meInB, meInC));
        given(teamMemberRepository.findByTeamIdInAndStatus(List.of(100L, 200L, 300L), TeamMemberStatus.ACTIVE))
                .willReturn(List.of(meInA, meInB, meInC));

        Message messageA = Message.builder().team(teamA).content("A").build();
        ReflectionTestUtils.setField(messageA, "createdAt", LocalDateTime.now().minusHours(2));
        Message messageB = Message.builder().team(teamB).content("B").build();
        ReflectionTestUtils.setField(messageB, "createdAt", LocalDateTime.now());
        Message messageC = Message.builder().team(teamC).content("C").build();
        ReflectionTestUtils.setField(messageC, "createdAt", LocalDateTime.now().minusMinutes(30));

        given(messageRepository.findLatestMessagePerTeam(List.of(100L, 200L, 300L)))
                .willReturn(List.of(messageA, messageB, messageC));
        given(messageRepository.countByTeam_TeamIdAndCreatedAtAfter(eq(100L), any()))
                .willReturn(1L);
        given(messageRepository.countByTeam_TeamIdAndCreatedAtAfter(eq(200L), any()))
                .willReturn(0L);
        given(messageRepository.countByTeam_TeamIdAndCreatedAtAfter(eq(300L), any()))
                .willReturn(3L);
        given(characterService.findAvatarsByMembers(any())).willReturn(Map.of());

        // when
        ChatRoomListResponse response = teamService.getMyChatRooms(1L, ChatRoomSortType.UNREAD);

        // then
        // B가 전체적으로 메시지는 가장 최신이지만 다 읽은 방이라 맨 뒤로 밀린다.
        assertThat(response.rooms()).extracting(ChatRoomSummaryResponse::teamId).containsExactly(300L, 100L, 200L);
    }

    @DisplayName("채팅방을 나가면 팀원 상태가 LEFT로 바뀌고 시스템 메시지가 발행된다.")
    @Test
    void 채팅방을_나가면_팀원_상태가_LEFT로_바뀌고_시스템_메시지가_발행된다() {
        // given
        Team team = Team.builder().teamId(100L).status(TeamStatus.IN_PROGRESS).build();
        TeamMember me = teamMemberOf(team, 1L, "김철수");
        given(teamRepository.findById(100L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(100L, 1L))
                .willReturn(Optional.of(me));

        // when
        teamService.leaveTeam(100L, 1L);

        // then
        assertThat(me.getStatus()).isEqualTo(TeamMemberStatus.LEFT);
        verify(chatService).postSystemMessage(any(Team.class), any(String.class));
        verify(collaborationPointService).awardPoint(me.getMember(), team, CollaborationPointReason.LEAVE_PENALTY);
        verify(leaderElectionService, org.mockito.Mockito.never()).recheckAfterMemberLeft(any(), any());
        verify(contestVotingService, org.mockito.Mockito.never()).recheckAfterMemberLeft(any(), any());
        verify(reviewService, org.mockito.Mockito.never()).recheckAfterMemberLeft(any());
    }

    @DisplayName("팀장 투표 중 나가면 남은 인원 기준으로 재확인한다.")
    @Test
    void 팀장_투표_중_나가면_남은_인원_기준으로_재확인한다() {
        // given
        Team team =
                Team.builder().teamId(100L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember me = teamMemberOf(team, 1L, "김철수");
        given(teamRepository.findById(100L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(100L, 1L))
                .willReturn(Optional.of(me));

        // when
        teamService.leaveTeam(100L, 1L);

        // then
        verify(leaderElectionService).recheckAfterMemberLeft(team, me.getTeamMemberId());
        verify(contestVotingService, org.mockito.Mockito.never()).recheckAfterMemberLeft(any(), any());
        verify(reviewService, org.mockito.Mockito.never()).recheckAfterMemberLeft(any());
    }

    @DisplayName("공모전 투표 중 나가면 남은 인원 기준으로 재확인한다.")
    @Test
    void 공모전_투표_중_나가면_남은_인원_기준으로_재확인한다() {
        // given
        Team team =
                Team.builder().teamId(100L).status(TeamStatus.CONTEST_SELECTING).build();
        TeamMember me = teamMemberOf(team, 1L, "김철수");
        given(teamRepository.findById(100L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(100L, 1L))
                .willReturn(Optional.of(me));

        // when
        teamService.leaveTeam(100L, 1L);

        // then
        verify(contestVotingService).recheckAfterMemberLeft(team, me.getTeamMemberId());
        verify(leaderElectionService, org.mockito.Mockito.never()).recheckAfterMemberLeft(any(), any());
        verify(reviewService, org.mockito.Mockito.never()).recheckAfterMemberLeft(any());
    }

    @DisplayName("리뷰 작성 중 나가면 남은 인원 기준으로 완료 여부를 재확인한다.")
    @Test
    void 리뷰_작성_중_나가면_남은_인원_기준으로_완료_여부를_재확인한다() {
        // given
        Team team = Team.builder().teamId(100L).status(TeamStatus.SUBMITTED).build();
        TeamMember me = teamMemberOf(team, 1L, "김철수");
        given(teamRepository.findById(100L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(100L, 1L))
                .willReturn(Optional.of(me));

        // when
        teamService.leaveTeam(100L, 1L);

        // then
        verify(reviewService).recheckAfterMemberLeft(team);
        verify(leaderElectionService, org.mockito.Mockito.never()).recheckAfterMemberLeft(any(), any());
        verify(contestVotingService, org.mockito.Mockito.never()).recheckAfterMemberLeft(any(), any());
    }

    @DisplayName("챗봇을 추가/삭제하면 팀의 chatbotEnabled 값이 바뀌고 시스템 메시지가 발행된다.")
    @Test
    void 챗봇을_추가_삭제하면_팀의_chatbotEnabled_값이_바뀌고_시스템_메시지가_발행된다() {
        // given
        Team team = Team.builder().teamId(100L).chatbotEnabled(true).build();
        TeamMember me = teamMemberOf(team, 1L, "김철수");
        given(teamRepository.findById(100L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(100L, 1L))
                .willReturn(Optional.of(me));

        // when
        teamService.toggleChatbot(100L, 1L, false);

        // then
        assertThat(team.isChatbotEnabled()).isFalse();
        verify(chatService).postSystemMessage(any(Team.class), any(String.class));
    }

    @DisplayName("대화상대를 조회하면 챗봇 활성화 여부와 함께 팀원 목록을 반환한다.")
    @Test
    void 대화상대를_조회하면_챗봇_활성화_여부와_함께_팀원_목록을_반환한다() {
        // given
        Team team = Team.builder()
                .teamId(100L)
                .chatbotEnabled(true)
                .status(TeamStatus.IN_PROGRESS)
                .build();
        TeamMember me = teamMemberOf(team, 1L, "나");
        TeamMember other = teamMemberOf(team, 2L, "김민정");

        given(teamRepository.findById(100L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(100L, 1L))
                .willReturn(Optional.of(me));
        given(teamMemberRepository.findByTeamIdAndStatus(100L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(me, other));
        given(characterService.findAvatarsByMembers(any())).willReturn(Map.of());

        // when
        TeamMembersResponse response = teamService.getTeamMembers(100L, 1L);

        // then
        assertThat(response.chatbotEnabled()).isTrue();
        assertThat(response.status()).isEqualTo("IN_PROGRESS");
        assertThat(response.participantCount()).isEqualTo(2);
        assertThat(response.members()).anySatisfy(m -> assertThat(m.isMe()).isTrue());
        assertThat(response.members())
                .filteredOn(m -> m.memberId().equals(2L))
                .singleElement()
                .satisfies(m -> assertThat(m.profileId()).isEqualTo(200L));
    }

    @DisplayName("대화상대 조회 시 캐릭터가 있는 팀원은 avatar가 채워지고, 없는 팀원은 null이다.")
    @Test
    void 대화상대_조회_시_캐릭터가_있는_팀원은_avatar가_채워진다() {
        // given
        Team team = Team.builder()
                .teamId(100L)
                .chatbotEnabled(true)
                .status(TeamStatus.IN_PROGRESS)
                .build();
        TeamMember me = teamMemberOf(team, 1L, "나");
        TeamMember other = teamMemberOf(team, 2L, "김민정");
        MemberAvatarResponse avatar =
                new MemberAvatarResponse(1L, CharacterType.LEAD_RUNNER, CharacterPalette.DEFAULT, null, null);

        given(teamRepository.findById(100L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(100L, 1L))
                .willReturn(Optional.of(me));
        given(teamMemberRepository.findByTeamIdAndStatus(100L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(me, other));
        given(characterService.findAvatarsByMembers(any())).willReturn(Map.of(1L, avatar));

        // when
        TeamMembersResponse response = teamService.getTeamMembers(100L, 1L);

        // then
        assertThat(response.members())
                .filteredOn(m -> m.memberId().equals(1L))
                .singleElement()
                .satisfies(m -> assertThat(m.avatar()).isEqualTo(avatar));
        assertThat(response.members())
                .filteredOn(m -> m.memberId().equals(2L))
                .singleElement()
                .satisfies(m -> assertThat(m.avatar()).isNull());
    }

    @DisplayName("팀 소속이 아닌 회원이 대화상대를 조회하면 예외가 발생한다.")
    @Test
    void 팀_소속이_아닌_회원이_대화상대를_조회하면_예외가_발생한다() {
        // given
        Team team = Team.builder().teamId(100L).build();
        given(teamRepository.findById(100L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(100L, 999L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> teamService.getTeamMembers(100L, 999L))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.NOT_TEAM_MEMBER);
    }

    @DisplayName("존재하지 않는 팀을 조회하면 예외가 발생한다.")
    @Test
    void 존재하지_않는_팀을_조회하면_예외가_발생한다() {
        // given
        given(teamRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> teamService.getTeamMembers(999L, 1L))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.TEAM_NOT_FOUND);
    }

    private TeamMember teamMemberOf(Team team, Long memberId, String nickname) {
        Member member = Member.builder().memberId(memberId).build();
        Profile profile =
                Profile.builder().profileId(memberId * 100).nickname(nickname).build();
        return TeamMember.builder()
                .team(team)
                .member(member)
                .profile(profile)
                .role(TeamRole.MEMBER)
                .status(TeamMemberStatus.ACTIVE)
                .build();
    }
}
