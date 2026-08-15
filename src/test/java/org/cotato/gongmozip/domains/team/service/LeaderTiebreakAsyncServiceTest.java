package org.cotato.gongmozip.domains.team.service;

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
class LeaderTiebreakAsyncServiceTest {

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private AiClient aiClient;

    @Mock
    private LeaderTiebreakTxService txService;

    @InjectMocks
    private LeaderTiebreakAsyncService service;

    @DisplayName("AI 추천 결과를 트랜잭션 서비스에 위임한다.")
    @Test
    void AI_추천_결과를_위임한다() {
        // given
        TeamMember member = teamMemberOf(10L);
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(member));
        given(aiClient.recommendTiebreakLeader(eq(1L), any(), eq(List.of(10L, 20L))))
                .willReturn(10L);

        // when
        service.resolveTiebreakAsync(1L, 1, List.of(10L, 20L));

        // then
        verify(txService).applyTiebreakResult(1L, 1, List.of(10L, 20L), 10L);
    }

    @DisplayName("AI 호출이 실패하면 추천 없음(null)으로 폴백한다.")
    @Test
    void AI_호출이_실패하면_추천_없음으로_폴백한다() {
        // given
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of());
        given(aiClient.recommendTiebreakLeader(any(), any(), any()))
                .willThrow(new RuntimeException("AI Gateway timeout"));

        // when
        service.resolveTiebreakAsync(1L, 2, List.of(10L, 20L));

        // then
        verify(txService).applyTiebreakResult(1L, 2, List.of(10L, 20L), null);
    }

    private TeamMember teamMemberOf(Long memberId) {
        return TeamMember.builder()
                .teamMemberId(memberId)
                .status(TeamMemberStatus.ACTIVE)
                .build();
    }
}
