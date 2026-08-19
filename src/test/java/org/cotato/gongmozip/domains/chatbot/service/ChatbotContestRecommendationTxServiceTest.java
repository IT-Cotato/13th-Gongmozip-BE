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
import org.cotato.gongmozip.domains.contest.entity.Contest;
import org.cotato.gongmozip.domains.contest.entity.ContestCandidate;
import org.cotato.gongmozip.domains.contest.repository.ContestCandidateRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatbotContestRecommendationTxServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private ContestCandidateRepository contestCandidateRepository;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatbotContestRecommendationTxService txService;

    @DisplayName("추천 공모전을 후보로 등록하고 추천 카드를 발행한다.")
    @Test
    void 추천_공모전을_후보로_등록하고_카드를_발행한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.CONTEST_SELECTING).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        TeamMember leader = teamMemberOf(team, 10L, "김민정");
        leader.assignAsLeader();
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(leader));
        Contest contest = Contest.builder()
                .contestId(100L)
                .title("공모전")
                .category(InterestCategory.IT_AI_TECH)
                .build();
        given(contestCandidateRepository.existsByTeam_TeamIdAndContest_ContestId(1L, 100L))
                .willReturn(false);

        // when
        txService.registerCandidatesAndAnnounce(1L, List.of(contest), List.of(100L), false);

        // then
        verify(contestCandidateRepository).save(any(ContestCandidate.class));
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.CONTEST_RECOMMEND_CARD), anyString(), anyString());
    }

    @DisplayName("이미 후보로 등록된 추천 공모전은 중복 등록하지 않는다.")
    @Test
    void 이미_후보로_등록된_추천_공모전은_중복_등록하지_않는다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.CONTEST_SELECTING).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        TeamMember leader = teamMemberOf(team, 10L, "김민정");
        leader.assignAsLeader();
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(leader));
        Contest contest = Contest.builder()
                .contestId(100L)
                .title("공모전")
                .category(InterestCategory.IT_AI_TECH)
                .build();
        given(contestCandidateRepository.existsByTeam_TeamIdAndContest_ContestId(1L, 100L))
                .willReturn(true);

        // when
        txService.registerCandidatesAndAnnounce(1L, List.of(contest), List.of(100L), false);

        // then
        verify(contestCandidateRepository, never()).save(any());
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.CONTEST_RECOMMEND_CARD), anyString(), anyString());
    }

    @DisplayName("팀장이 없으면(이론상 발생하지 않아야 함) 후보를 등록하지 않고 카드만 발행한다.")
    @Test
    void 팀장이_없으면_후보를_등록하지_않는다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.CONTEST_SELECTING).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of());
        Contest contest = Contest.builder()
                .contestId(100L)
                .title("공모전")
                .category(InterestCategory.IT_AI_TECH)
                .build();

        // when
        txService.registerCandidatesAndAnnounce(1L, List.of(contest), List.of(100L), false);

        // then
        verify(contestCandidateRepository, never()).save(any());
        verify(chatService)
                .postChatbotCardMessage(eq(team), eq(MessageType.CONTEST_RECOMMEND_CARD), anyString(), anyString());
    }

    @DisplayName("AUTO_ASSIGNED로 전이된 팀은 '자기소개를 마쳤다면'으로, 선출을 거친 팀은 '팀장 선출까지 마쳤다면'으로 안내한다.")
    @Test
    void 팀장_선출_경로에_따라_안내_문구가_달라진다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.CONTEST_SELECTING).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        TeamMember leader = teamMemberOf(team, 10L, "김민정");
        leader.assignAsLeader();
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(leader));
        Contest contest = Contest.builder()
                .contestId(100L)
                .title("공모전")
                .category(InterestCategory.IT_AI_TECH)
                .build();
        given(contestCandidateRepository.existsByTeam_TeamIdAndContest_ContestId(1L, 100L))
                .willReturn(false);

        // when
        txService.registerCandidatesAndAnnounce(1L, List.of(contest), List.of(100L), true);

        // then
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatService)
                .postChatbotCardMessage(
                        eq(team), eq(MessageType.CONTEST_RECOMMEND_CARD), contentCaptor.capture(), anyString());
        assertThat(contentCaptor.getValue()).startsWith("자기소개를 마쳤다면");
    }

    @DisplayName("일반 안내 메시지를 발행한다.")
    @Test
    void 일반_안내_메시지를_발행한다() {
        // given
        Team team =
                Team.builder().teamId(1L).status(TeamStatus.CONTEST_SELECTING).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when
        txService.announcePlainPrompt(1L, false);

        // then
        verify(chatService).postChatbotMessage(eq(team), anyString());
    }

    @DisplayName("존재하지 않는 팀이면 예외가 발생한다.")
    @Test
    void 존재하지_않는_팀이면_예외가_발생한다() {
        // given
        given(teamRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> txService.announcePlainPrompt(999L, false))
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
