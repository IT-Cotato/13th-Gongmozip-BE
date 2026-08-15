package org.cotato.gongmozip.domains.chatbot.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.global.ai.AiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatbotLeaderNominationAsyncServiceTest {

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private AiClient aiClient;

    @Mock
    private ChatbotLeaderNominationTxService txService;

    @InjectMocks
    private ChatbotLeaderNominationAsyncService service;

    @DisplayName("AI 추천 결과를 트랜잭션 서비스에 위임한다.")
    @Test
    void AI_추천_결과를_위임한다() {
        // given
        TeamMember member = teamMemberOf(10L);
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(member));
        given(aiClient.recommendLeaderCandidates(eq(1L), any())).willReturn(List.of(10L));

        // when
        service.recommendLeaderNomineesAsync(1L);

        // then
        verify(txService).announceLeaderNomination(1L, List.of(10L));
    }

    @DisplayName("AI 호출이 실패하면 추천 없음으로 폴백한다.")
    @Test
    void AI_호출이_실패하면_추천_없음으로_폴백한다() {
        // given
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of());
        given(aiClient.recommendLeaderCandidates(any(), any())).willThrow(new RuntimeException("AI Gateway timeout"));

        // when
        service.recommendLeaderNomineesAsync(1L);

        // then
        verify(txService).announceLeaderNomination(1L, List.of());
    }

    private TeamMember teamMemberOf(Long memberId) {
        return TeamMember.builder()
                .teamMemberId(memberId)
                .status(TeamMemberStatus.ACTIVE)
                .build();
    }
}
