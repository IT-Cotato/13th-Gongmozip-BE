package org.cotato.gongmozip.domains.team.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.chat.entity.Message;
import org.cotato.gongmozip.domains.chat.enums.MessageType;
import org.cotato.gongmozip.domains.chat.repository.MessageRepository;
import org.cotato.gongmozip.domains.chat.service.ChatService;
import org.cotato.gongmozip.domains.chatbot.service.ChatbotOrchestrationService;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.team.entity.LeaderVote;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.LeaderCandidacyStatus;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.enums.TeamRole;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.domains.team.exception.TeamException;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.domains.team.repository.LeaderVoteRepository;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.cotato.gongmozip.global.ai.AiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeaderElectionServiceTest {

    @Mock
    private ChatService chatService;

    @Mock
    private ChatbotOrchestrationService chatbotOrchestrationService;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private LeaderVoteRepository leaderVoteRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private AiClient aiClient;

    @InjectMocks
    private LeaderElectionService leaderElectionService;

    @DisplayName("LEADER_SELECTING 상태가 아니면 팀장 여부 투표에 실패한다.")
    @Test
    void LEADER_SELECTING_상태가_아니면_팀장_여부_투표에_실패한다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.GREETING).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when & then
        assertThatThrownBy(() -> leaderElectionService.submitCandidacy(1L, 10L, true))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.INVALID_TEAM_STATUS);
    }

    @DisplayName("이미 팀장 투표가 시작됐으면 여부 투표를 다시 할 수 없다.")
    @Test
    void 이미_팀장_투표가_시작됐으면_여부_투표를_다시_할_수_없다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(leaderVoteRepository.existsByTeam_TeamId(1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> leaderElectionService.submitCandidacy(1L, 10L, true))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.INVALID_TEAM_STATUS);
    }

    @DisplayName("일부만 응답했으면 다음 단계로 넘어가지 않는다.")
    @Test
    void 일부만_응답했으면_다음_단계로_넘어가지_않는다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember responder = teamMemberOf(team, 10L, "김철수");
        TeamMember notYet = teamMemberOf(team, 20L, "이해은");

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(leaderVoteRepository.existsByTeam_TeamId(1L)).willReturn(false);
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(responder));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(responder, notYet));

        // when
        leaderElectionService.submitCandidacy(1L, 10L, true);

        // then
        assertThat(responder.getLeaderCandidacy()).isEqualTo(LeaderCandidacyStatus.WANTS);
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_SELECTING);
        verify(chatService, never()).postChatbotMessage(any(), anyString());
        verify(chatService, never()).postChatbotCardMessage(any(), any(), anyString(), any());
    }

    @DisplayName("전원이 원하지 않으면 무작위로 임시 팀장이 지정된다.")
    @Test
    void 전원이_원하지_않으면_무작위로_임시_팀장이_지정된다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember a = teamMemberOf(team, 10L, "김철수");
        a.updateLeaderCandidacy(LeaderCandidacyStatus.DOES_NOT_WANT);
        TeamMember b = teamMemberOf(team, 20L, "이해은");

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(leaderVoteRepository.existsByTeam_TeamId(1L)).willReturn(false);
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 20L)).willReturn(Optional.of(b));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(a, b));

        // when
        leaderElectionService.submitCandidacy(1L, 20L, false);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_DECIDED);
        assertThat(List.of(a.getRole(), b.getRole())).contains(TeamRole.LEADER);
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.LEADER_RESULT_CARD), anyString(), anyString());
    }

    @DisplayName("후보가 1명뿐이면 투표 없이 바로 팀장으로 확정된다.")
    @Test
    void 후보가_1명뿐이면_투표_없이_바로_팀장으로_확정된다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember onlyCandidate = teamMemberOf(team, 10L, "김철수");
        TeamMember other = teamMemberOf(team, 20L, "이해은");
        other.updateLeaderCandidacy(LeaderCandidacyStatus.DOES_NOT_WANT);

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(leaderVoteRepository.existsByTeam_TeamId(1L)).willReturn(false);
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(onlyCandidate));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(onlyCandidate, other));

        // when
        leaderElectionService.submitCandidacy(1L, 10L, true);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_DECIDED);
        assertThat(onlyCandidate.getRole()).isEqualTo(TeamRole.LEADER);
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.LEADER_RESULT_CARD), anyString(), anyString());
        verify(chatService, never())
                .postChatbotCardMessage(any(), eq(MessageType.LEADER_VOTE_CARD), anyString(), any());
    }

    @DisplayName("후보가 2명 이상이면 팀장 투표 카드가 발행되고 아직 팀장은 정해지지 않는다.")
    @Test
    void 후보가_2명_이상이면_팀장_투표_카드가_발행되고_아직_팀장은_정해지지_않는다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember a = teamMemberOf(team, 10L, "김철수");
        a.updateLeaderCandidacy(LeaderCandidacyStatus.WANTS);
        TeamMember b = teamMemberOf(team, 20L, "이해은");

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(leaderVoteRepository.existsByTeam_TeamId(1L)).willReturn(false);
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 20L)).willReturn(Optional.of(b));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(a, b));

        // when
        leaderElectionService.submitCandidacy(1L, 20L, true);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_SELECTING);
        assertThat(a.getRole()).isEqualTo(TeamRole.MEMBER);
        assertThat(b.getRole()).isEqualTo(TeamRole.MEMBER);
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.LEADER_VOTE_CARD), anyString(), anyString());
    }

    @DisplayName("아직 후보가 정해지지 않았으면 투표할 수 없다.")
    @Test
    void 아직_후보가_정해지지_않았으면_투표할_수_없다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember voter = teamMemberOf(team, 10L, "김철수");
        TeamMember pending = teamMemberOf(team, 20L, "이해은");

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(voter));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(voter, pending));

        // when & then
        assertThatThrownBy(() -> leaderElectionService.castVote(1L, 10L, 20L))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.LEADER_CANDIDACY_PENDING);
    }

    @DisplayName("후보가 아닌 팀원에게 투표하면 실패한다.")
    @Test
    void 후보가_아닌_팀원에게_투표하면_실패한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember voter = teamMemberOf(team, 10L, "김철수");
        voter.updateLeaderCandidacy(LeaderCandidacyStatus.WANTS);
        TeamMember notACandidate = teamMemberOf(team, 20L, "이해은");
        notACandidate.updateLeaderCandidacy(LeaderCandidacyStatus.DOES_NOT_WANT);
        TeamMember alsoCandidate = teamMemberOf(team, 30L, "박준수");
        alsoCandidate.updateLeaderCandidacy(LeaderCandidacyStatus.WANTS);

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(voter));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(voter, notACandidate, alsoCandidate));
        given(leaderVoteRepository.findMaxRoundByTeamId(1L)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> leaderElectionService.castVote(1L, 10L, 20L))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.INVALID_LEADER_CANDIDATE);
    }

    @DisplayName("이미 이번 라운드에 투표했으면 다시 투표할 수 없다.")
    @Test
    void 이미_이번_라운드에_투표했으면_다시_투표할_수_없다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember voter = teamMemberOf(team, 10L, "김철수");
        voter.updateLeaderCandidacy(LeaderCandidacyStatus.WANTS);
        TeamMember candidate = teamMemberOf(team, 20L, "이해은");
        candidate.updateLeaderCandidacy(LeaderCandidacyStatus.WANTS);

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(voter));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(voter, candidate));
        given(leaderVoteRepository.findMaxRoundByTeamId(1L)).willReturn(null);
        given(leaderVoteRepository.existsByTeam_TeamIdAndVoterTeamMember_TeamMemberIdAndRound(1L, 10L, 1))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> leaderElectionService.castVote(1L, 10L, 20L))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.ALREADY_VOTED_LEADER);
    }

    @DisplayName("전원이 투표하고 단독 1위가 있으면 팀장으로 확정된다.")
    @Test
    void 전원이_투표하고_단독_1위가_있으면_팀장으로_확정된다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember voter1 = teamMemberOf(team, 10L, "김철수");
        voter1.updateLeaderCandidacy(LeaderCandidacyStatus.WANTS);
        TeamMember voter2 = teamMemberOf(team, 20L, "이해은");
        voter2.updateLeaderCandidacy(LeaderCandidacyStatus.WANTS);
        TeamMember voter3 = teamMemberOf(team, 30L, "박준수");
        voter3.updateLeaderCandidacy(LeaderCandidacyStatus.DOES_NOT_WANT);

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 30L)).willReturn(Optional.of(voter3));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(voter1, voter2, voter3));
        given(leaderVoteRepository.findMaxRoundByTeamId(1L)).willReturn(1);

        LeaderVote existing1 = LeaderVote.builder()
                .team(team)
                .voterTeamMember(voter1)
                .candidateTeamMember(voter1)
                .round(1)
                .build();
        LeaderVote existing2 = LeaderVote.builder()
                .team(team)
                .voterTeamMember(voter2)
                .candidateTeamMember(voter1)
                .round(1)
                .build();
        given(leaderVoteRepository.findByTeam_TeamIdAndRound(1L, 1))
                .willReturn(List.of(existing1, existing2))
                .willReturn(List.of(
                        existing1,
                        existing2,
                        LeaderVote.builder()
                                .team(team)
                                .voterTeamMember(voter3)
                                .candidateTeamMember(voter1)
                                .round(1)
                                .build()));
        given(leaderVoteRepository.existsByTeam_TeamIdAndVoterTeamMember_TeamMemberIdAndRound(1L, 30L, 1))
                .willReturn(false);

        // when
        leaderElectionService.castVote(1L, 30L, 10L);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_DECIDED);
        assertThat(voter1.getRole()).isEqualTo(TeamRole.LEADER);
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.LEADER_RESULT_CARD), anyString(), anyString());
    }

    @DisplayName("전원이 투표했는데 동률이면 재투표 안내 카드가 발행되고 팀장은 정해지지 않는다.")
    @Test
    void 전원이_투표했는데_동률이면_재투표_안내_카드가_발행되고_팀장은_정해지지_않는다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember voter1 = teamMemberOf(team, 10L, "김철수");
        voter1.updateLeaderCandidacy(LeaderCandidacyStatus.WANTS);
        TeamMember voter2 = teamMemberOf(team, 20L, "이해은");
        voter2.updateLeaderCandidacy(LeaderCandidacyStatus.WANTS);

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 20L)).willReturn(Optional.of(voter2));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(voter1, voter2));
        given(leaderVoteRepository.findMaxRoundByTeamId(1L)).willReturn(null);
        given(aiClient.recommendTiebreakLeader(any(), any(), any())).willReturn(10L);

        LeaderVote existing = LeaderVote.builder()
                .team(team)
                .voterTeamMember(voter1)
                .candidateTeamMember(voter1)
                .round(1)
                .build();
        given(leaderVoteRepository.findByTeam_TeamIdAndRound(1L, 1))
                .willReturn(List.of(
                        existing,
                        LeaderVote.builder()
                                .team(team)
                                .voterTeamMember(voter2)
                                .candidateTeamMember(voter2)
                                .round(1)
                                .build()));

        // when
        leaderElectionService.castVote(1L, 20L, 20L);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_SELECTING);
        assertThat(voter1.getRole()).isEqualTo(TeamRole.MEMBER);
        assertThat(voter2.getRole()).isEqualTo(TeamRole.MEMBER);
        ArgumentCaptor<String> metadataCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatService)
                .postChatbotCardMessage(
                        eq(team), eq(MessageType.LEADER_VOTE_CARD), anyString(), metadataCaptor.capture());
        assertThat(metadataCaptor.getValue()).contains("10").contains("20");
        assertThat(metadataCaptor.getValue()).contains("aiRecommendedTeamMemberId");
    }

    @DisplayName("재투표(2라운드)도 동률이면 더 재투표하지 않고 AI 추천 후보로 바로 확정된다.")
    @Test
    void 재투표도_동률이면_AI_추천으로_바로_확정된다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember voter1 = teamMemberOf(team, 10L, "김철수");
        voter1.updateLeaderCandidacy(LeaderCandidacyStatus.WANTS);
        TeamMember voter2 = teamMemberOf(team, 20L, "이해은");
        voter2.updateLeaderCandidacy(LeaderCandidacyStatus.WANTS);

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 20L)).willReturn(Optional.of(voter2));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(voter1, voter2));
        given(leaderVoteRepository.findMaxRoundByTeamId(1L)).willReturn(1);
        given(aiClient.recommendTiebreakLeader(any(), any(), any())).willReturn(10L);
        given(leaderVoteRepository.existsByTeam_TeamIdAndVoterTeamMember_TeamMemberIdAndRound(1L, 20L, 2))
                .willReturn(false);

        // 1라운드가 이미 동률로 끝난 상태 — 2라운드 eligible 후보도 그대로 10L/20L
        given(leaderVoteRepository.findByTeam_TeamIdAndRound(1L, 1))
                .willReturn(List.of(
                        LeaderVote.builder()
                                .team(team)
                                .voterTeamMember(voter1)
                                .candidateTeamMember(voter1)
                                .round(1)
                                .build(),
                        LeaderVote.builder()
                                .team(team)
                                .voterTeamMember(voter2)
                                .candidateTeamMember(voter2)
                                .round(1)
                                .build()));
        // 2라운드 재투표도 다시 동률
        given(leaderVoteRepository.findByTeam_TeamIdAndRound(1L, 2))
                .willReturn(List.of(
                        LeaderVote.builder()
                                .team(team)
                                .voterTeamMember(voter1)
                                .candidateTeamMember(voter1)
                                .round(2)
                                .build(),
                        LeaderVote.builder()
                                .team(team)
                                .voterTeamMember(voter2)
                                .candidateTeamMember(voter2)
                                .round(2)
                                .build()));

        // when
        leaderElectionService.castVote(1L, 20L, 20L);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_DECIDED);
        assertThat(voter1.getRole()).isEqualTo(TeamRole.LEADER);
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.LEADER_RESULT_CARD), anyString(), anyString());
        verify(chatService, never())
                .postChatbotCardMessage(any(), eq(MessageType.LEADER_VOTE_CARD), anyString(), anyString());
        verify(chatbotOrchestrationService).advanceToContestSelecting(team);
    }

    @DisplayName("LEADER_SELECTING 상태가 아니면 AI 추천 수락에 실패한다.")
    @Test
    void LEADER_SELECTING_상태가_아니면_AI_추천_수락에_실패한다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.GREETING).build();
        given(teamRepository.findByIdWithLock(1L)).willReturn(Optional.of(team));

        // when & then
        assertThatThrownBy(() -> leaderElectionService.acceptAiRecommendation(1L, 10L))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.INVALID_TEAM_STATUS);
    }

    @DisplayName("수락할 AI 추천이 없으면 실패한다.")
    @Test
    void 수락할_AI_추천이_없으면_실패한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember member = teamMemberOf(team, 10L, "김철수");
        given(teamRepository.findByIdWithLock(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(member));
        given(messageRepository.findFirstByTeam_TeamIdAndMessageTypeOrderByCreatedAtDesc(
                        1L, MessageType.LEADER_VOTE_CARD))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> leaderElectionService.acceptAiRecommendation(1L, 10L))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.NO_PENDING_AI_RECOMMENDATION);
    }

    @DisplayName("AI 추천을 수락하면 추천된 후보가 바로 팀장으로 확정된다.")
    @Test
    void AI_추천을_수락하면_추천된_후보가_바로_팀장으로_확정된다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember accepter = teamMemberOf(team, 20L, "이해은");
        TeamMember recommended = teamMemberOf(team, 10L, "김철수");
        Message voteCard = Message.builder()
                .messageType(MessageType.LEADER_VOTE_CARD)
                .metadata("{\"candidateTeamMemberIds\":[10,20],\"aiRecommendedTeamMemberId\":10}")
                .build();

        given(teamRepository.findByIdWithLock(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 20L)).willReturn(Optional.of(accepter));
        given(messageRepository.findFirstByTeam_TeamIdAndMessageTypeOrderByCreatedAtDesc(
                        1L, MessageType.LEADER_VOTE_CARD))
                .willReturn(Optional.of(voteCard));
        given(teamMemberRepository.findById(10L)).willReturn(Optional.of(recommended));

        // when
        leaderElectionService.acceptAiRecommendation(1L, 20L);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_DECIDED);
        assertThat(recommended.getRole()).isEqualTo(TeamRole.LEADER);
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.LEADER_RESULT_CARD), anyString(), anyString());
        verify(chatbotOrchestrationService).advanceToContestSelecting(team);
    }

    @DisplayName("추천된 후보가 이미 팀을 나갔으면 AI 추천 수락에 실패한다.")
    @Test
    void 추천된_후보가_이미_팀을_나갔으면_AI_추천_수락에_실패한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember accepter = teamMemberOf(team, 20L, "이해은");
        TeamMember left = TeamMember.builder()
                .teamMemberId(10L)
                .team(team)
                .member(Member.builder().memberId(10L).build())
                .profile(Profile.builder().nickname("김철수").build())
                .status(TeamMemberStatus.LEFT)
                .build();
        Message voteCard = Message.builder()
                .messageType(MessageType.LEADER_VOTE_CARD)
                .metadata("{\"candidateTeamMemberIds\":[10,20],\"aiRecommendedTeamMemberId\":10}")
                .build();

        given(teamRepository.findByIdWithLock(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 20L)).willReturn(Optional.of(accepter));
        given(messageRepository.findFirstByTeam_TeamIdAndMessageTypeOrderByCreatedAtDesc(
                        1L, MessageType.LEADER_VOTE_CARD))
                .willReturn(Optional.of(voteCard));
        given(teamMemberRepository.findById(10L)).willReturn(Optional.of(left));

        // when & then
        assertThatThrownBy(() -> leaderElectionService.acceptAiRecommendation(1L, 20L))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.INVALID_LEADER_CANDIDATE);
    }

    @DisplayName("동률 상태가 아니면 재투표 요청에 실패한다.")
    @Test
    void 동률_상태가_아니면_재투표_요청에_실패한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember member = teamMemberOf(team, 10L, "김철수");
        Message initialVoteCard = Message.builder()
                .messageType(MessageType.LEADER_VOTE_CARD)
                .metadata("{\"candidateTeamMemberIds\":[10,20]}")
                .build();
        given(teamRepository.findByIdWithLock(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(member));
        given(messageRepository.findFirstByTeam_TeamIdAndMessageTypeOrderByCreatedAtDesc(
                        1L, MessageType.LEADER_VOTE_CARD))
                .willReturn(Optional.of(initialVoteCard));

        // when & then
        assertThatThrownBy(() -> leaderElectionService.requestRevote(1L, 10L))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.NO_PENDING_AI_RECOMMENDATION);
    }

    @DisplayName("동률 상태에서 재투표를 요청하면 동률이었던 후보 목록으로 안내 카드가 다시 발행된다.")
    @Test
    void 동률_상태에서_재투표를_요청하면_안내_카드가_다시_발행된다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember requester = teamMemberOf(team, 20L, "이해은");
        Message tieVoteCard = Message.builder()
                .messageType(MessageType.LEADER_VOTE_CARD)
                .metadata("{\"candidateTeamMemberIds\":[10,20],\"aiRecommendedTeamMemberId\":10}")
                .build();
        given(teamRepository.findByIdWithLock(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 20L)).willReturn(Optional.of(requester));
        given(messageRepository.findFirstByTeam_TeamIdAndMessageTypeOrderByCreatedAtDesc(
                        1L, MessageType.LEADER_VOTE_CARD))
                .willReturn(Optional.of(tieVoteCard));

        // when
        leaderElectionService.requestRevote(1L, 20L);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_SELECTING);
        ArgumentCaptor<String> metadataCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatService)
                .postChatbotCardMessage(
                        eq(team), eq(MessageType.LEADER_VOTE_CARD), anyString(), metadataCaptor.capture());
        assertThat(metadataCaptor.getValue()).contains("10").contains("20").doesNotContain("aiRecommendedTeamMemberId");
    }

    @DisplayName("존재하지 않는 팀이면 예외가 발생한다.")
    @Test
    void 존재하지_않는_팀이면_예외가_발생한다() {
        // given
        given(teamRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> leaderElectionService.submitCandidacy(999L, 1L, true))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.TEAM_NOT_FOUND);
    }

    @DisplayName("나간 사람이 투표를 안 한 상태였고, 남은 인원 기준으로 이미 다 찼으면 즉시 개표한다.")
    @Test
    void 나간_사람이_투표를_안_한_상태였고_남은_인원_기준으로_이미_다_찼으면_즉시_개표한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember voter1 = teamMemberOf(team, 10L, "김철수");
        TeamMember voter2 = teamMemberOf(team, 20L, "이해은");

        given(leaderVoteRepository.existsByTeam_TeamId(1L)).willReturn(true);
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(voter1, voter2));
        given(leaderVoteRepository.findMaxRoundByTeamId(1L)).willReturn(1);
        given(leaderVoteRepository.existsByTeam_TeamIdAndVoterTeamMember_TeamMemberIdAndRound(1L, 30L, 1))
                .willReturn(false);
        given(leaderVoteRepository.findByTeam_TeamIdAndRound(1L, 1))
                .willReturn(List.of(
                        LeaderVote.builder()
                                .team(team)
                                .voterTeamMember(voter1)
                                .candidateTeamMember(voter1)
                                .round(1)
                                .build(),
                        LeaderVote.builder()
                                .team(team)
                                .voterTeamMember(voter2)
                                .candidateTeamMember(voter1)
                                .round(1)
                                .build()));

        // when
        leaderElectionService.recheckAfterMemberLeft(team, 30L);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_DECIDED);
        assertThat(voter1.getRole()).isEqualTo(TeamRole.LEADER);
    }

    @DisplayName("득표 1위 후보 본인이 나가면 크래시 없이 남은 활성 팀원 중 1명을 임시 팀장으로 지정한다.")
    @Test
    void 득표_1위_후보_본인이_나가면_크래시_없이_남은_활성_팀원_중_1명을_임시_팀장으로_지정한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember a = teamMemberOf(team, 10L, "김철수");
        TeamMember b = teamMemberOf(team, 20L, "이해은");
        TeamMember leavingCandidate = teamMemberOf(team, 30L, "박준수"); // 득표 1위였지만 나간 사람

        given(leaderVoteRepository.existsByTeam_TeamId(1L)).willReturn(true);
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(a, b));
        given(leaderVoteRepository.findMaxRoundByTeamId(1L)).willReturn(1);
        given(leaderVoteRepository.existsByTeam_TeamIdAndVoterTeamMember_TeamMemberIdAndRound(1L, 30L, 1))
                .willReturn(false);
        given(leaderVoteRepository.findByTeam_TeamIdAndRound(1L, 1))
                .willReturn(List.of(
                        LeaderVote.builder()
                                .team(team)
                                .voterTeamMember(a)
                                .candidateTeamMember(leavingCandidate)
                                .round(1)
                                .build(),
                        LeaderVote.builder()
                                .team(team)
                                .voterTeamMember(b)
                                .candidateTeamMember(leavingCandidate)
                                .round(1)
                                .build()));

        // when
        leaderElectionService.recheckAfterMemberLeft(team, 30L);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_DECIDED);
        assertThat(List.of(a.getRole(), b.getRole())).contains(TeamRole.LEADER);
    }

    @DisplayName("나간 사람이 이미 투표했었다면 재확인해도 다시 개표하지 않는다.")
    @Test
    void 나간_사람이_이미_투표했었다면_재확인해도_다시_개표하지_않는다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();

        given(leaderVoteRepository.existsByTeam_TeamId(1L)).willReturn(true);
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(teamMemberOf(team, 10L, "김철수")));
        given(leaderVoteRepository.findMaxRoundByTeamId(1L)).willReturn(1);
        given(leaderVoteRepository.existsByTeam_TeamIdAndVoterTeamMember_TeamMemberIdAndRound(1L, 30L, 1))
                .willReturn(true);

        // when
        leaderElectionService.recheckAfterMemberLeft(team, 30L);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_SELECTING);
        verify(leaderVoteRepository, never()).findByTeam_TeamIdAndRound(any(), anyInt());
    }

    @DisplayName("투표 시작 전(팀장 여부 응답 단계)에 나간 사람이 응답 전이었고 남은 인원이 모두 응답을 마쳤으면 다음 단계로 진행한다.")
    @Test
    void 투표_시작_전_나간_사람이_응답_전이었고_남은_인원이_모두_응답을_마쳤으면_다음_단계로_진행한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember a = teamMemberOf(team, 10L, "김철수");
        a.updateLeaderCandidacy(LeaderCandidacyStatus.WANTS);
        TeamMember b = teamMemberOf(team, 20L, "이해은");
        b.updateLeaderCandidacy(LeaderCandidacyStatus.WANTS);
        TeamMember leavingMember = teamMemberOf(team, 30L, "박준수"); // 기본값 UNDECIDED, 즉 응답 전

        given(leaderVoteRepository.existsByTeam_TeamId(1L)).willReturn(false);
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(a, b));
        given(teamMemberRepository.findById(30L)).willReturn(Optional.of(leavingMember));

        // when
        leaderElectionService.recheckAfterMemberLeft(team, 30L);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_SELECTING);
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.LEADER_VOTE_CARD), anyString(), anyString());
    }

    @DisplayName("투표 시작 전에 나간 사람이 이미 응답했었다면 재확인하지 않는다.")
    @Test
    void 투표_시작_전에_나간_사람이_이미_응답했었다면_재확인하지_않는다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember stillUndecided = teamMemberOf(team, 10L, "김철수");
        TeamMember leavingMember = teamMemberOf(team, 30L, "박준수");
        leavingMember.updateLeaderCandidacy(LeaderCandidacyStatus.DOES_NOT_WANT);

        given(leaderVoteRepository.existsByTeam_TeamId(1L)).willReturn(false);
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(stillUndecided));
        given(teamMemberRepository.findById(30L)).willReturn(Optional.of(leavingMember));

        // when
        leaderElectionService.recheckAfterMemberLeft(team, 30L);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_SELECTING);
        verify(chatService, never()).postChatbotMessage(any(), anyString());
        verify(chatService, never()).postChatbotCardMessage(any(), any(), anyString(), any());
    }

    @DisplayName("LEADER_SELECTING이 아니면 마감 처리를 하지 않는다.")
    @Test
    void LEADER_SELECTING이_아니면_마감_처리를_하지_않는다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.GREETING).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when
        leaderElectionService.resolveDeadlineIfDue(1L);

        // then
        verify(teamMemberRepository, never()).findByTeamIdAndStatus(any(), any());
        verify(chatService, never()).postChatbotCardMessage(any(), any(), anyString(), any());
    }

    @DisplayName("마감 시점에 후보 등록(팀장 여부 투표)이 안 끝났으면, 응답 안 한 사람은 아니요로 간주하고 확정한다.")
    @Test
    void 마감_시점에_응답_안_한_사람은_아니요로_간주되어_확정된다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember onlyCandidate = teamMemberOf(team, 10L, "김철수");
        onlyCandidate.updateLeaderCandidacy(LeaderCandidacyStatus.WANTS);
        TeamMember neverResponded = teamMemberOf(team, 20L, "이해은");

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(onlyCandidate, neverResponded));

        // when
        leaderElectionService.resolveDeadlineIfDue(1L);

        // then
        assertThat(neverResponded.getLeaderCandidacy()).isEqualTo(LeaderCandidacyStatus.DOES_NOT_WANT);
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_DECIDED);
        assertThat(onlyCandidate.getRole()).isEqualTo(TeamRole.LEADER);
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.LEADER_RESULT_CARD), anyString(), anyString());
    }

    @DisplayName("후보 등록은 끝났지만 아무도 투표하지 않은 채 마감되면 무작위로 임시 팀장을 지정한다.")
    @Test
    void 후보_등록은_끝났지만_투표가_없으면_무작위로_임시_팀장을_지정한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember candidate1 = teamMemberOf(team, 10L, "김철수");
        candidate1.updateLeaderCandidacy(LeaderCandidacyStatus.WANTS);
        TeamMember candidate2 = teamMemberOf(team, 20L, "이해은");
        candidate2.updateLeaderCandidacy(LeaderCandidacyStatus.WANTS);

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(candidate1, candidate2));
        given(leaderVoteRepository.findMaxRoundByTeamId(1L)).willReturn(null);
        given(leaderVoteRepository.findByTeam_TeamIdAndRound(1L, 1)).willReturn(List.of());

        // when
        leaderElectionService.resolveDeadlineIfDue(1L);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_DECIDED);
        assertThat(List.of(candidate1.getRole(), candidate2.getRole())).contains(TeamRole.LEADER);
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.LEADER_RESULT_CARD), anyString(), anyString());
    }

    @DisplayName("투표 없이 마감되면 '팀장 안 할래요'를 명시한 사람은 무작위 대상에서 제외된다.")
    @Test
    void 투표_없이_마감되면_DOES_NOT_WANT_팀원은_무작위_대상에서_제외된다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember onlyCandidate = teamMemberOf(team, 10L, "김철수");
        onlyCandidate.updateLeaderCandidacy(LeaderCandidacyStatus.WANTS);
        TeamMember declined1 = teamMemberOf(team, 20L, "이해은");
        declined1.updateLeaderCandidacy(LeaderCandidacyStatus.DOES_NOT_WANT);
        TeamMember declined2 = teamMemberOf(team, 30L, "박준수");
        declined2.updateLeaderCandidacy(LeaderCandidacyStatus.DOES_NOT_WANT);

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(onlyCandidate, declined1, declined2));
        given(leaderVoteRepository.findMaxRoundByTeamId(1L)).willReturn(null);
        given(leaderVoteRepository.findByTeam_TeamIdAndRound(1L, 1)).willReturn(List.of());

        // when
        leaderElectionService.resolveDeadlineIfDue(1L);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_DECIDED);
        assertThat(onlyCandidate.getRole()).isEqualTo(TeamRole.LEADER);
        assertThat(declined1.getRole()).isEqualTo(TeamRole.MEMBER);
        assertThat(declined2.getRole()).isEqualTo(TeamRole.MEMBER);
    }

    @DisplayName("후보 등록은 끝났고 마감 시점에 일부라도 투표가 있으면 있는 대로 개표한다.")
    @Test
    void 후보_등록은_끝났고_일부_투표가_있으면_있는_대로_개표한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        TeamMember candidate1 = teamMemberOf(team, 10L, "김철수");
        candidate1.updateLeaderCandidacy(LeaderCandidacyStatus.WANTS);
        TeamMember candidate2 = teamMemberOf(team, 20L, "이해은");
        candidate2.updateLeaderCandidacy(LeaderCandidacyStatus.WANTS);
        TeamMember candidate3 = teamMemberOf(team, 30L, "박준수");
        candidate3.updateLeaderCandidacy(LeaderCandidacyStatus.WANTS);

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(candidate1, candidate2, candidate3));
        given(leaderVoteRepository.findMaxRoundByTeamId(1L)).willReturn(null);
        given(leaderVoteRepository.findByTeam_TeamIdAndRound(1L, 1))
                .willReturn(List.of(LeaderVote.builder()
                        .team(team)
                        .voterTeamMember(candidate1)
                        .candidateTeamMember(candidate2)
                        .round(1)
                        .build()));

        // when
        leaderElectionService.resolveDeadlineIfDue(1L);

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.LEADER_DECIDED);
        assertThat(candidate2.getRole()).isEqualTo(TeamRole.LEADER);
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.LEADER_RESULT_CARD), anyString(), anyString());
    }

    private TeamMember teamMemberOf(Team team, Long memberId, String nickname) {
        Member member = Member.builder().memberId(memberId).build();
        Profile profile = Profile.builder().nickname(nickname).build();
        return TeamMember.builder()
                .teamMemberId(memberId)
                .team(team)
                .member(member)
                .profile(profile)
                .status(TeamMemberStatus.ACTIVE)
                .build();
    }
}
