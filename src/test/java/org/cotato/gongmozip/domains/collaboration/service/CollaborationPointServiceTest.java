package org.cotato.gongmozip.domains.collaboration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.cotato.gongmozip.domains.collaboration.entity.CollaborationPointHistory;
import org.cotato.gongmozip.domains.collaboration.enums.CollaborationPointReason;
import org.cotato.gongmozip.domains.collaboration.repository.CollaborationPointHistoryRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollaborationPointServiceTest {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-31T04:00:00Z"), KOREA_ZONE);

    @Mock
    private CollaborationPointHistoryRepository collaborationPointHistoryRepository;

    private CollaborationPointService collaborationPointService;

    @BeforeEach
    void setUp() {
        collaborationPointService = new CollaborationPointService(collaborationPointHistoryRepository, FIXED_CLOCK);
    }

    @DisplayName("포인트를 적립하면 회원의 누적 포인트가 증가하고 히스토리가 저장된다.")
    @Test
    void 포인트를_적립하면_회원의_누적_포인트가_증가하고_히스토리가_저장된다() {
        // given
        Member member = Member.builder().memberId(1L).collaborationPoint(20).build();
        Team team = Team.builder().teamId(100L).build();
        given(collaborationPointHistoryRepository.save(any(CollaborationPointHistory.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        collaborationPointService.awardPoint(member, team, CollaborationPointReason.PROGRESS_CHECK_RESPONSE);

        // then
        assertThat(member.getCollaborationPoint()).isEqualTo(25);
    }

    @DisplayName("차감 사유로 적립하면 누적 포인트가 0 미만으로 내려가지 않는다.")
    @Test
    void 차감_사유로_적립하면_누적_포인트가_0_미만으로_내려가지_않는다() {
        // given
        Member member = Member.builder().memberId(1L).collaborationPoint(5).build();
        Team team = Team.builder().teamId(100L).build();
        given(collaborationPointHistoryRepository.save(any(CollaborationPointHistory.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        collaborationPointService.awardPoint(member, team, CollaborationPointReason.LEAVE_PENALTY);

        // then
        assertThat(member.getCollaborationPoint()).isEqualTo(0);
        ArgumentCaptor<CollaborationPointHistory> historyCaptor =
                ArgumentCaptor.forClass(CollaborationPointHistory.class);
        verify(collaborationPointHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getDelta()).isEqualTo(-5);
    }

    @DisplayName("최대치를 초과해서 적립되지 않는다.")
    @Test
    void 최대치를_초과해서_적립되지_않는다() {
        // given
        Member member = Member.builder().memberId(1L).collaborationPoint(495).build();
        Team team = Team.builder().teamId(100L).build();
        given(collaborationPointHistoryRepository.save(any(CollaborationPointHistory.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        collaborationPointService.awardPoint(member, team, CollaborationPointReason.PROJECT_COMPLETE_LEADER);

        // then
        assertThat(member.getCollaborationPoint()).isEqualTo(Member.MAX_COLLABORATION_POINT);
    }

    @DisplayName("최근 2주 감점이 50m에 도달하면 매칭 참여가 7일간 제한된다.")
    @Test
    void 최근_감점이_50에_도달하면_매칭이_제한된다() {
        // given
        Member member = Member.builder().memberId(1L).collaborationPoint(100).build();
        given(collaborationPointHistoryRepository.sumLossSince(any(Member.class), any(LocalDateTime.class)))
                .willReturn(40L);
        given(collaborationPointHistoryRepository.save(any(CollaborationPointHistory.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        collaborationPointService.changePoint(member, null, CollaborationPointReason.MATCHING_PASS_PENALTY, -10);

        // then
        assertThat(member.getMatchingBlockedUntil()).isEqualTo(LocalDateTime.of(2026, 8, 7, 13, 0));
        assertThat(member.getCollaborationPoint()).isEqualTo(90);
    }
}
