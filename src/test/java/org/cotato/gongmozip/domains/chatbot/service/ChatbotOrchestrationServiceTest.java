package org.cotato.gongmozip.domains.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.chat.enums.MessageType;
import org.cotato.gongmozip.domains.chat.service.ChatService;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.LeaderCandidacyStatus;
import org.cotato.gongmozip.domains.team.enums.LeaderSelectionMode;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.domains.team.exception.TeamException;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class ChatbotOrchestrationServiceTest {

    @Mock
    private ChatService chatService;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private ChatbotContestRecommendationAsyncService contestRecommendationAsyncService;

    @Mock
    private ChatbotLeaderNominationAsyncService leaderNominationAsyncService;

    @Mock
    private ChatbotMentionAsyncService chatbotMentionAsyncService;

    @InjectMocks
    private ChatbotOrchestrationService chatbotOrchestrationService;

    // advanceToContestSelecting이 커밋 이후에만 비동기 추천 호출을 트리거하도록
    // TransactionSynchronizationManager.registerSynchronization을 쓰기 때문에, 실제 트랜잭션
    // 없이 서비스 메서드를 직접 호출하는 단위 테스트에서도 등록이 되게 동기화 컨텍스트를 열어준다
    // (ProfileServiceTest와 동일한 패턴).
    @BeforeEach
    void setUpTransactionSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDownTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clear();
        }
    }

    @DisplayName("팀 생성 직후 인사 유도를 시작하면 상태가 GREETING이 되고 챗봇 메시지가 발행된다.")
    @Test
    void 팀_생성_직후_인사_유도를_시작하면_상태가_GREETING이_되고_챗봇_메시지가_발행된다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.MATCHED).build();

        // when
        chatbotOrchestrationService.startGreeting(team);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.GREETING);
        verify(chatService).postChatbotMessage(eq(team), anyString());
    }

    @DisplayName("GREETING 상태가 아니면 인사 기록을 하지 않는다.")
    @Test
    void GREETING_상태가_아니면_인사_기록을_하지_않는다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when
        chatbotOrchestrationService.recordGreetingAndAdvance(1L, 10L);

        // then
        verify(teamMemberRepository, never()).findByTeam_TeamIdAndMember_MemberId(any(), any());
        verify(chatService, never()).postChatbotMessage(any(), anyString());
    }

    @DisplayName("일부만 인사했으면 상태를 전이하지 않고 발신자의 인사만 기록한다.")
    @Test
    void 일부만_인사했으면_상태를_전이하지_않고_발신자의_인사만_기록한다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.GREETING).build();
        TeamMember sender = teamMemberOf(team, 10L, "김철수");
        TeamMember notGreetedYet = teamMemberOf(team, 20L, "이해은");

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(sender));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(sender, notGreetedYet));

        // when
        chatbotOrchestrationService.recordGreetingAndAdvance(1L, 10L);

        // then
        assertThat(sender.getGreetedAt()).isNotNull();
        assertThat(team.getStatus()).isEqualTo(TeamStatus.GREETING);
        verify(chatService, never()).postChatbotMessage(any(), anyString());
    }

    @DisplayName("전원이 인사를 마치면 팀장 선출 단계로 전이하고, 커밋 후 팀장 추천 호출이 비동기로 위임된다.")
    @Test
    void 전원이_인사를_마치면_팀장_선출_단계로_전이하고_팀장_추천_호출을_비동기로_위임한다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.GREETING).build();
        TeamMember alreadyGreeted = teamMemberOf(team, 20L, "이해은");
        alreadyGreeted.markGreeted(java.time.LocalDateTime.now());
        TeamMember lastToGreet = teamMemberOf(team, 10L, "김철수");

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(lastToGreet));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(alreadyGreeted, lastToGreet));

        // when
        chatbotOrchestrationService.recordGreetingAndAdvance(1L, 10L);

        // then
        assertThat(lastToGreet.getGreetedAt()).isNotNull();
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_SELECTING);
        verify(leaderNominationAsyncService, never()).recommendLeaderNomineesAsync(any());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(leaderNominationAsyncService).recommendLeaderNomineesAsync(1L);
    }

    @DisplayName("타임아웃이 지나면 일부만 인사했어도 강제로 팀장 선출 단계로 전이하고, 커밋 후 팀장 추천 호출이 비동기로 위임된다.")
    @Test
    void 타임아웃이_지나면_일부만_인사했어도_강제로_팀장_선출_단계로_전이한다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.GREETING).build();
        TeamMember greeted = teamMemberOf(team, 10L, "김철수");
        greeted.markGreeted(java.time.LocalDateTime.now());
        TeamMember neverGreeted = teamMemberOf(team, 20L, "이해은");

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(greeted, neverGreeted));

        // when
        chatbotOrchestrationService.forceAdvanceGreetingIfDue(1L);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_SELECTING);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(leaderNominationAsyncService).recommendLeaderNomineesAsync(1L);
    }

    @DisplayName("AUTO_ASSIGNED 팀은 인사 메시지 자체에 팀장 안내가 포함된다.")
    @Test
    void AUTO_ASSIGNED_팀은_인사_메시지에_팀장_안내가_포함된다() {
        // given
        Team team = Team.builder()
                .teamId(1L)
                .status(TeamStatus.MATCHED)
                .leaderSelectionMode(LeaderSelectionMode.AUTO_ASSIGNED)
                .build();
        TeamMember leader = teamMemberOf(team, 10L, "김민정");
        leader.assignAsLeader();
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(leader));

        // when
        chatbotOrchestrationService.startGreeting(team);

        // then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatService).postChatbotMessage(eq(team), messageCaptor.capture());
        assertThat(messageCaptor.getValue()).contains("김민정").contains("팀장");
    }

    @DisplayName("AUTO_ASSIGNED 팀은 전원 인사 완료 시 LEADER_SELECTING을 건너뛰고 바로 공모전 단계로 전이한다.")
    @Test
    void AUTO_ASSIGNED_팀은_전원_인사_완료_시_LEADER_SELECTING을_건너뛴다() {
        // given
        Team team = Team.builder()
                .teamId(1L)
                .status(TeamStatus.GREETING)
                .leaderSelectionMode(LeaderSelectionMode.AUTO_ASSIGNED)
                .preferredCategory(InterestCategory.IT_AI_TECH)
                .build();
        TeamMember leader = teamMemberOf(team, 10L, "김민정");
        leader.assignAsLeader();
        leader.markGreeted(java.time.LocalDateTime.now());
        TeamMember lastToGreet = teamMemberOf(team, 20L, "이해은");

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 20L)).willReturn(Optional.of(lastToGreet));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(leader, lastToGreet));

        // when
        chatbotOrchestrationService.recordGreetingAndAdvance(1L, 20L);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.CONTEST_SELECTING);
        ArgumentCaptor<String> metadataCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatService)
                .postChatbotCardMessage(
                        eq(team), eq(MessageType.LEADER_RESULT_CARD), anyString(), metadataCaptor.capture());
        assertThat(metadataCaptor.getValue()).contains("leaderTeamMemberId").contains("10");
        verify(chatService, never())
                .postChatbotCardMessage(any(), eq(MessageType.LEADER_NOMINATION_CARD), anyString(), anyString());
        verify(leaderNominationAsyncService, never()).recommendLeaderNomineesAsync(any());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(contestRecommendationAsyncService).recommendContestsAsync(1L, InterestCategory.IT_AI_TECH);
        verify(leaderNominationAsyncService, never()).recommendLeaderNomineesAsync(any());
    }

    @DisplayName("CANDIDATE_VOTE 팀은 팀장 여부 투표 없이 사전 후보 전원을 바로 투표 카드로 발행한다.")
    @Test
    void CANDIDATE_VOTE_팀은_후보_투표_카드를_바로_발행한다() {
        // given
        Team team = Team.builder()
                .teamId(1L)
                .status(TeamStatus.GREETING)
                .leaderSelectionMode(LeaderSelectionMode.CANDIDATE_VOTE)
                .build();
        TeamMember candidate1 = teamMemberOf(team, 10L, "김민정", true);
        TeamMember candidate2 = teamMemberOf(team, 20L, "이해은", true);
        candidate2.markGreeted(java.time.LocalDateTime.now());
        TeamMember nonCandidate = teamMemberOf(team, 30L, "박준수", false);
        nonCandidate.markGreeted(java.time.LocalDateTime.now());

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(candidate1));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(candidate1, candidate2, nonCandidate));

        // when
        chatbotOrchestrationService.recordGreetingAndAdvance(1L, 10L);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_SELECTING);
        assertThat(candidate1.getLeaderCandidacy()).isEqualTo(LeaderCandidacyStatus.WANTS);
        assertThat(candidate2.getLeaderCandidacy()).isEqualTo(LeaderCandidacyStatus.WANTS);
        assertThat(nonCandidate.getLeaderCandidacy()).isEqualTo(LeaderCandidacyStatus.DOES_NOT_WANT);
        ArgumentCaptor<String> metadataCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatService)
                .postChatbotCardMessage(
                        eq(team), eq(MessageType.LEADER_VOTE_CARD), anyString(), metadataCaptor.capture());
        assertThat(metadataCaptor.getValue()).contains("10").contains("20").doesNotContain("30");
        verify(chatService, never())
                .postChatbotCardMessage(any(), eq(MessageType.LEADER_NOMINATION_CARD), anyString(), anyString());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(leaderNominationAsyncService, never()).recommendLeaderNomineesAsync(any());
    }

    @DisplayName("이미 GREETING을 지나 다음 단계로 넘어간 팀이면 타임아웃 강제 전이를 하지 않는다.")
    @Test
    void 이미_GREETING을_지나_다음_단계로_넘어간_팀이면_타임아웃_강제_전이를_하지_않는다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when
        chatbotOrchestrationService.forceAdvanceGreetingIfDue(1L);

        // then
        verify(teamMemberRepository, never()).findByTeamIdAndStatus(any(), any());
        verify(chatService, never()).postChatbotCardMessage(any(), any(), anyString(), any());
    }

    @DisplayName("이미 인사한 팀원이 다시 메시지를 보내도 중복 처리되지 않는다.")
    @Test
    void 이미_인사한_팀원이_다시_메시지를_보내도_중복_처리되지_않는다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.GREETING).build();
        TeamMember sender = teamMemberOf(team, 10L, "김철수");
        sender.markGreeted(java.time.LocalDateTime.now().minusMinutes(1));

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(sender));

        // when
        chatbotOrchestrationService.recordGreetingAndAdvance(1L, 10L);

        // then
        verify(teamMemberRepository, never()).findByTeamIdAndStatus(any(), any());
        verify(chatService, never()).postChatbotMessage(any(), anyString());
    }

    @DisplayName("존재하지 않는 팀이면 예외가 발생한다.")
    @Test
    void 존재하지_않는_팀이면_예외가_발생한다() {
        // given
        given(teamRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatbotOrchestrationService.recordGreetingAndAdvance(999L, 1L))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.TEAM_NOT_FOUND);
    }

    @DisplayName("공모전 선정 단계 진입 시 상태/마감을 커밋한 뒤에만 AI 추천 호출을 비동기로 위임한다.")
    @Test
    void 공모전_선정_단계_진입_시_AI_추천_호출을_비동기로_위임한다() {
        // given
        Team team = Team.builder()
                .teamId(1L)
                .status(TeamStatus.LEADER_DECIDED)
                .preferredCategory(InterestCategory.IT_AI_TECH)
                .build();

        // when
        chatbotOrchestrationService.advanceToContestSelecting(team);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.CONTEST_SELECTING);
        assertThat(team.getContestCandidateDeadlineAt()).isNotNull();
        // 커밋(afterCommit) 전에는 아직 AI 추천을 호출하지 않는다 — 상태 전이만 동기로 반영된다.
        verify(contestRecommendationAsyncService, never()).recommendContestsAsync(any(), any());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(contestRecommendationAsyncService).recommendContestsAsync(1L, InterestCategory.IT_AI_TECH);
    }

    @DisplayName("공모전이 확정되면 팀이 IN_PROGRESS로 전이되고 활용 안내 카드도 함께 발행된다.")
    @Test
    void 공모전이_확정되면_팀이_IN_PROGRESS로_전이되고_활용_안내_카드도_함께_발행된다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.CONTEST_DECIDED).build();

        // when
        chatbotOrchestrationService.advanceToInProgress(team);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.IN_PROGRESS);
        verify(chatService).postChatbotMessage(eq(team), anyString());
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.CHATBOT_GUIDE_CARD), anyString(), anyString());
    }

    @DisplayName("리뷰가 완료되면 팀이 COMPLETED로 전이되고 마무리 메시지가 발행된다.")
    @Test
    void 리뷰가_완료되면_팀이_COMPLETED로_전이되고_마무리_메시지가_발행된다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.SUBMITTED).build();

        // when
        chatbotOrchestrationService.completeReview(team);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.COMPLETED);
        verify(chatService).postChatbotMessage(eq(team), anyString());
    }

    @DisplayName("@챗봇으로 말을 걸면 AI 호출이 비동기로 위임된다.")
    @Test
    void 챗봇으로_말을_걸면_AI_호출이_비동기로_위임된다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.IN_PROGRESS).build();
        team.setChatbotEnabled(true);
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when
        chatbotOrchestrationService.respondToMentionIfAny(1L, "@챗봇 우리 역할 분담 추천해줘");

        // then
        verify(chatbotMentionAsyncService).answerMentionAsync(1L, "우리 역할 분담 추천해줘");
        verify(chatService, never()).postChatbotMessage(any(), anyString());
    }

    @DisplayName("@챗봇으로 시작하지 않는 메시지는 무시한다.")
    @Test
    void 챗봇으로_시작하지_않는_메시지는_무시한다() {
        // when
        chatbotOrchestrationService.respondToMentionIfAny(1L, "안녕하세요 @챗봇");

        // then
        verify(teamRepository, never()).findById(any());
        verify(chatbotMentionAsyncService, never()).answerMentionAsync(any(), any());
    }

    @DisplayName("챗봇이 꺼져있으면 @챗봇 멘션에 응답하지 않는다.")
    @Test
    void 챗봇이_꺼져있으면_챗봇_멘션에_응답하지_않는다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.IN_PROGRESS).build();
        team.setChatbotEnabled(false);
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when
        chatbotOrchestrationService.respondToMentionIfAny(1L, "@챗봇 타임라인 추천해줘");

        // then
        verify(chatbotMentionAsyncService, never()).answerMentionAsync(any(), any());
    }

    private TeamMember teamMemberOf(Team team, Long memberId, String nickname) {
        return teamMemberOf(team, memberId, nickname, false);
    }

    private TeamMember teamMemberOf(Team team, Long memberId, String nickname, boolean isPreLeaderCandidate) {
        Member member = Member.builder().memberId(memberId).build();
        Profile profile = Profile.builder().nickname(nickname).build();
        return TeamMember.builder()
                .teamMemberId(memberId)
                .team(team)
                .member(member)
                .profile(profile)
                .status(TeamMemberStatus.ACTIVE)
                .isPreLeaderCandidate(isPreLeaderCandidate)
                .build();
    }
}
