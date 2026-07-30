package org.cotato.gongmozip.domains.contest.service;

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
import org.cotato.gongmozip.domains.chatbot.service.ChatbotOrchestrationService;
import org.cotato.gongmozip.domains.contest.dto.response.ContestResponse.ContestCandidateItemResponse;
import org.cotato.gongmozip.domains.contest.dto.response.ContestResponse.ContestCandidateListResponse;
import org.cotato.gongmozip.domains.contest.entity.Contest;
import org.cotato.gongmozip.domains.contest.entity.ContestCandidate;
import org.cotato.gongmozip.domains.contest.entity.ContestVote;
import org.cotato.gongmozip.domains.contest.exception.ContestException;
import org.cotato.gongmozip.domains.contest.exception.codes.ContestErrorCode;
import org.cotato.gongmozip.domains.contest.repository.ContestCandidateRepository;
import org.cotato.gongmozip.domains.contest.repository.ContestRepository;
import org.cotato.gongmozip.domains.contest.repository.ContestVoteRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
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
class ContestVotingServiceTest {

    @Mock
    private ChatService chatService;

    @Mock
    private ChatbotOrchestrationService chatbotOrchestrationService;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private ContestRepository contestRepository;

    @Mock
    private ContestCandidateRepository contestCandidateRepository;

    @Mock
    private ContestVoteRepository contestVoteRepository;

    @InjectMocks
    private ContestVotingService contestVotingService;

    @DisplayName("CONTEST_SELECTING 상태가 아니면 후보 추가에 실패한다.")
    @Test
    void CONTEST_SELECTING_상태가_아니면_후보_추가에_실패한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.LEADER_SELECTING).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when & then
        assertThatThrownBy(() -> contestVotingService.addCandidate(1L, 10L, 100L))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.INVALID_TEAM_STATUS);
    }

    @DisplayName("이미 후보로 추가된 공모전이면 실패한다.")
    @Test
    void 이미_후보로_추가된_공모전이면_실패한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.CONTEST_SELECTING).build();
        TeamMember member = teamMemberOf(team, 10L, "김철수");
        Contest contest = Contest.builder()
                .contestId(100L)
                .title("공모전")
                .status(org.cotato.gongmozip.domains.contest.enums.ContestStatus.OPEN)
                .applyEndAt(java.time.LocalDateTime.now().plusDays(7))
                .build();

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(member));
        given(contestRepository.findById(100L)).willReturn(Optional.of(contest));
        given(contestCandidateRepository.existsByTeam_TeamIdAndContest_ContestId(1L, 100L))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> contestVotingService.addCandidate(1L, 10L, 100L))
                .isInstanceOf(ContestException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContestErrorCode.DUPLICATE_CONTEST_CANDIDATE);
    }

    @DisplayName("후보 공모전을 정상적으로 추가한다.")
    @Test
    void 후보_공모전을_정상적으로_추가한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.CONTEST_SELECTING).build();
        TeamMember member = teamMemberOf(team, 10L, "김철수");
        Contest contest = Contest.builder()
                .contestId(100L)
                .title("공모전")
                .status(org.cotato.gongmozip.domains.contest.enums.ContestStatus.OPEN)
                .applyEndAt(java.time.LocalDateTime.now().plusDays(7))
                .build();

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(member));
        given(contestRepository.findById(100L)).willReturn(Optional.of(contest));
        given(contestCandidateRepository.existsByTeam_TeamIdAndContest_ContestId(1L, 100L))
                .willReturn(false);
        given(contestCandidateRepository.save(any(ContestCandidate.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        ContestCandidateItemResponse response = contestVotingService.addCandidate(1L, 10L, 100L);

        // then
        assertThat(response.contest().contestId()).isEqualTo(100L);
        verify(chatService).postSystemMessage(eq(team), anyString());
    }

    @DisplayName("존재하지 않는 후보를 삭제하면 실패한다.")
    @Test
    void 존재하지_않는_후보를_삭제하면_실패한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.CONTEST_SELECTING).build();
        TeamMember member = teamMemberOf(team, 10L, "김철수");
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(member));
        given(contestCandidateRepository.findByTeam_TeamIdAndContestCandidateId(1L, 999L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> contestVotingService.removeCandidate(1L, 10L, 999L))
                .isInstanceOf(ContestException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContestErrorCode.CONTEST_CANDIDATE_NOT_FOUND);
    }

    @DisplayName("후보 리스트를 조회한다.")
    @Test
    void 후보_리스트를_조회한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.CONTEST_SELECTING).build();
        TeamMember member = teamMemberOf(team, 10L, "김철수");
        Contest contest = Contest.builder()
                .contestId(100L)
                .title("공모전")
                .status(org.cotato.gongmozip.domains.contest.enums.ContestStatus.OPEN)
                .applyEndAt(java.time.LocalDateTime.now().plusDays(7))
                .build();
        ContestCandidate candidate = ContestCandidate.builder()
                .contestCandidateId(1L)
                .team(team)
                .contest(contest)
                .addedByTeamMember(member)
                .build();

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(member));
        given(contestCandidateRepository.findByTeamId(1L)).willReturn(List.of(candidate));

        // when
        ContestCandidateListResponse response = contestVotingService.getCandidates(1L, 10L);

        // then
        assertThat(response.candidates()).hasSize(1);
        assertThat(response.candidates().get(0).contest().contestId()).isEqualTo(100L);
    }

    @DisplayName("3개를 선택하면 투표에 실패한다.")
    @Test
    void 세개를_선택하면_투표에_실패한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.CONTEST_SELECTING).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L))
                .willReturn(Optional.of(teamMemberOf(team, 10L, "김철수")));

        // when & then
        assertThatThrownBy(() -> contestVotingService.submitVote(1L, 10L, List.of(1L, 2L, 3L)))
                .isInstanceOf(ContestException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContestErrorCode.INVALID_CONTEST_VOTE_SELECTION);
    }

    @DisplayName("후보에 없는 공모전에 투표하면 실패한다.")
    @Test
    void 후보에_없는_공모전에_투표하면_실패한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.CONTEST_SELECTING).build();
        TeamMember voter = teamMemberOf(team, 10L, "김철수");
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(voter));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(voter));
        given(contestVoteRepository.findMaxRoundByTeamId(1L)).willReturn(null);
        given(contestCandidateRepository.findByTeamId(1L)).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> contestVotingService.submitVote(1L, 10L, List.of(999L)))
                .isInstanceOf(ContestException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContestErrorCode.CONTEST_CANDIDATE_NOT_FOUND);
    }

    @DisplayName("이미 이번 라운드에 투표했으면 다시 투표할 수 없다.")
    @Test
    void 이미_이번_라운드에_투표했으면_다시_투표할_수_없다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.CONTEST_SELECTING).build();
        TeamMember voter = teamMemberOf(team, 10L, "김철수");
        Contest contest = Contest.builder()
                .contestId(100L)
                .title("공모전")
                .status(org.cotato.gongmozip.domains.contest.enums.ContestStatus.OPEN)
                .applyEndAt(java.time.LocalDateTime.now().plusDays(7))
                .build();
        ContestCandidate candidate = ContestCandidate.builder()
                .contestCandidateId(1L)
                .team(team)
                .contest(contest)
                .addedByTeamMember(voter)
                .build();

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(voter));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(voter));
        given(contestVoteRepository.findMaxRoundByTeamId(1L)).willReturn(null);
        given(contestCandidateRepository.findByTeamId(1L)).willReturn(List.of(candidate));
        given(contestVoteRepository.existsByTeam_TeamIdAndVoterTeamMember_TeamMemberIdAndRound(1L, 10L, 1))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> contestVotingService.submitVote(1L, 10L, List.of(1L)))
                .isInstanceOf(ContestException.class)
                .hasFieldOrPropertyWithValue("errorCode", ContestErrorCode.ALREADY_VOTED_CONTEST);
    }

    @DisplayName("전원이 투표하고 단독 1위가 있으면 공모전이 확정되고 IN_PROGRESS로 전이한다.")
    @Test
    void 전원이_투표하고_단독_1위가_있으면_공모전이_확정되고_IN_PROGRESS로_전이한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.CONTEST_SELECTING).build();
        ReflectionTestUtils.setField(
                team, "createdAt", java.time.LocalDateTime.now().minusDays(3));
        TeamMember voter1 = teamMemberOf(team, 10L, "김철수");
        TeamMember voter2 = teamMemberOf(team, 20L, "이해은");
        Contest contestA = Contest.builder()
                .contestId(100L)
                .title("A공모전")
                .applyEndAt(java.time.LocalDateTime.now().plusDays(7))
                .build();
        Contest contestB = Contest.builder().contestId(200L).title("B공모전").build();
        ContestCandidate candidateA = ContestCandidate.builder()
                .contestCandidateId(1L)
                .team(team)
                .contest(contestA)
                .addedByTeamMember(voter1)
                .build();
        ContestCandidate candidateB = ContestCandidate.builder()
                .contestCandidateId(2L)
                .team(team)
                .contest(contestB)
                .addedByTeamMember(voter1)
                .build();

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 20L)).willReturn(Optional.of(voter2));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(voter1, voter2));
        given(contestVoteRepository.findMaxRoundByTeamId(1L)).willReturn(null);
        given(contestCandidateRepository.findByTeamId(1L)).willReturn(List.of(candidateA, candidateB));
        given(contestVoteRepository.existsByTeam_TeamIdAndVoterTeamMember_TeamMemberIdAndRound(1L, 20L, 1))
                .willReturn(false);
        given(contestVoteRepository.countDistinctVotersByTeamIdAndRound(1L, 1)).willReturn(2L);
        given(contestVoteRepository.findByTeam_TeamIdAndRound(1L, 1))
                .willReturn(List.of(
                        ContestVote.builder()
                                .team(team)
                                .contestCandidate(candidateA)
                                .voterTeamMember(voter1)
                                .round(1)
                                .build(),
                        ContestVote.builder()
                                .team(team)
                                .contestCandidate(candidateA)
                                .voterTeamMember(voter2)
                                .round(1)
                                .build(),
                        ContestVote.builder()
                                .team(team)
                                .contestCandidate(candidateB)
                                .voterTeamMember(voter1)
                                .round(1)
                                .build()));

        // when
        contestVotingService.submitVote(1L, 20L, List.of(1L, 2L));

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.CONTEST_DECIDED);
        assertThat(team.getContest()).isEqualTo(contestA);
        verify(chatbotOrchestrationService).advanceToInProgress(team);
    }

    @DisplayName("동률이면 재투표 카드가 발행되고 아직 확정되지 않는다.")
    @Test
    void 동률이면_재투표_카드가_발행되고_아직_확정되지_않는다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.CONTEST_SELECTING).build();
        TeamMember voter1 = teamMemberOf(team, 10L, "김철수");
        Contest contestA = Contest.builder().contestId(100L).title("A공모전").build();
        Contest contestB = Contest.builder().contestId(200L).title("B공모전").build();
        ContestCandidate candidateA = ContestCandidate.builder()
                .contestCandidateId(1L)
                .team(team)
                .contest(contestA)
                .addedByTeamMember(voter1)
                .build();
        ContestCandidate candidateB = ContestCandidate.builder()
                .contestCandidateId(2L)
                .team(team)
                .contest(contestB)
                .addedByTeamMember(voter1)
                .build();

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(voter1));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(voter1));
        given(contestVoteRepository.findMaxRoundByTeamId(1L)).willReturn(null);
        given(contestCandidateRepository.findByTeamId(1L)).willReturn(List.of(candidateA, candidateB));
        given(contestVoteRepository.countDistinctVotersByTeamIdAndRound(1L, 1)).willReturn(1L);
        given(contestVoteRepository.findByTeam_TeamIdAndRound(1L, 1))
                .willReturn(List.of(
                        ContestVote.builder()
                                .team(team)
                                .contestCandidate(candidateA)
                                .voterTeamMember(voter1)
                                .round(1)
                                .build(),
                        ContestVote.builder()
                                .team(team)
                                .contestCandidate(candidateB)
                                .voterTeamMember(voter1)
                                .round(1)
                                .build()));

        // when
        contestVotingService.submitVote(1L, 10L, List.of(1L, 2L));

        // then
        assertThat(team.getStatus()).isEqualTo(TeamStatus.CONTEST_SELECTING);
        assertThat(team.getContest()).isNull();
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.CONTEST_VOTE_CARD), anyString(), anyString());
        verify(chatbotOrchestrationService, never()).advanceToInProgress(any());
    }

    @DisplayName("존재하지 않는 팀이면 예외가 발생한다.")
    @Test
    void 존재하지_않는_팀이면_예외가_발생한다() {
        // given
        given(teamRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> contestVotingService.getCandidates(999L, 1L))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.TEAM_NOT_FOUND);
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
