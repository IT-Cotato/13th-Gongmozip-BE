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

    @Mock
    private org.cotato.gongmozip.domains.member.repository.MemberRepository memberRepository;

    private CollaborationPointService collaborationPointService;

    @BeforeEach
    void setUp() {
        collaborationPointService =
                new CollaborationPointService(collaborationPointHistoryRepository, memberRepository, FIXED_CLOCK);
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

    @DisplayName("회원 존재 시 getCollaborationDistance가 게이지 정보를 올바르게 반환한다.")
    @Test
    void 회원_존재_시_getCollaborationDistance가_게이지_정보를_올바르게_반환한다() {
        // given
        Long memberId = 1L;
        Member member =
                Member.builder().memberId(memberId).collaborationPoint(300).build();
        given(memberRepository.findById(memberId)).willReturn(java.util.Optional.of(member));

        // when
        org.cotato.gongmozip.domains.collaboration.dto.response.CollaborationResponse.CollaborationDistanceResponse
                response = collaborationPointService.getCollaborationDistance(memberId);

        // then
        assertThat(response.collaborationPoint()).isEqualTo(300);
        assertThat(response.maxCollaborationPoint()).isEqualTo(500);
        assertThat(response.gaugePercent()).isEqualTo(60.0);
    }

    @DisplayName("getCollaborationDistance 호출 시 회원을 찾지 못하면 MEMBER_NOT_FOUND 예외가 발생한다.")
    @Test
    void getCollaborationDistance_호출_시_회원_미존재_시_예외가_발생한다() {
        // given
        Long memberId = 1L;
        given(memberRepository.findById(memberId)).willReturn(java.util.Optional.empty());

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> collaborationPointService.getCollaborationDistance(memberId))
                .isInstanceOf(org.cotato.gongmozip.domains.member.exception.MemberException.class)
                .hasMessage(
                        org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode.MEMBER_NOT_FOUND
                                .getMessage());
    }

    @DisplayName("회원 존재 시 getCollaborationHistories가 페이징 처리된 변경 내역을 최신순으로 반환한다.")
    @Test
    void 회원_존재_시_getCollaborationHistories가_페이징_처리된_변경_내역을_반환한다() {
        // given
        Long memberId = 1L;
        Member member = Member.builder().memberId(memberId).build();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 5);

        CollaborationPointHistory history = CollaborationPointHistory.builder()
                .collaborationPointHistoryId(10L)
                .member(member)
                .delta(10)
                .reasonCode(CollaborationPointReason.REVIEW_WRITTEN)
                .build();

        org.springframework.data.domain.Page<CollaborationPointHistory> page =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Collections.singletonList(history), pageable, 1);

        given(memberRepository.findById(memberId)).willReturn(java.util.Optional.of(member));
        given(collaborationPointHistoryRepository.findAllByMemberOrderByCreatedAtDesc(member, pageable))
                .willReturn(page);

        // when
        org.cotato.gongmozip.domains.collaboration.dto.response.CollaborationResponse.CollaborationHistoryListResponse
                response = collaborationPointService.getCollaborationHistories(memberId, pageable);

        // then
        assertThat(response.histories()).hasSize(1);
        assertThat(response.histories().get(0).reason()).isEqualTo("팀원 리뷰 작성 완료");
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(5);
        assertThat(response.totalElements()).isEqualTo(1L);
        assertThat(response.hasNext()).isFalse();
    }

    @DisplayName("getCollaborationHistories 호출 시 회원을 찾지 못하면 MEMBER_NOT_FOUND 예외가 발생한다.")
    @Test
    void getCollaborationHistories_호출_시_회원_미존재_시_예외가_발생한다() {
        // given
        Long memberId = 1L;
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 5);
        given(memberRepository.findById(memberId)).willReturn(java.util.Optional.empty());

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> collaborationPointService.getCollaborationHistories(memberId, pageable))
                .isInstanceOf(org.cotato.gongmozip.domains.member.exception.MemberException.class)
                .hasMessage(
                        org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode.MEMBER_NOT_FOUND
                                .getMessage());
    }

    @DisplayName("변경 이력이 존재하지 않을 경우 빈 이력 목록이 페이징 결과와 함께 반환된다.")
    @Test
    void 이력이_존재하지_않을_경우_빈_이력_목록이_반환된다() {
        // given
        Long memberId = 1L;
        Member member = Member.builder().memberId(memberId).build();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 5);

        org.springframework.data.domain.Page<CollaborationPointHistory> emptyPage =
                new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList(), pageable, 0);

        given(memberRepository.findById(memberId)).willReturn(java.util.Optional.of(member));
        given(collaborationPointHistoryRepository.findAllByMemberOrderByCreatedAtDesc(member, pageable))
                .willReturn(emptyPage);

        // when
        org.cotato.gongmozip.domains.collaboration.dto.response.CollaborationResponse.CollaborationHistoryListResponse
                response = collaborationPointService.getCollaborationHistories(memberId, pageable);

        // then
        assertThat(response.histories()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0L);
        assertThat(response.totalPages()).isEqualTo(0);
    }
}
