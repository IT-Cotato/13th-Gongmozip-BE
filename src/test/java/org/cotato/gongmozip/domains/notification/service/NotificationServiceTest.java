package org.cotato.gongmozip.domains.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.notification.dto.response.NotificationResponse.NotificationListResponse;
import org.cotato.gongmozip.domains.notification.entity.Notification;
import org.cotato.gongmozip.domains.notification.enums.NotificationCategory;
import org.cotato.gongmozip.domains.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @DisplayName("챗봇 채팅방 이벤트는 수신자 수만큼 CHATROOM 알림 행을 한 번에 저장한다.")
    @Test
    void 챗봇_채팅방_이벤트는_수신자_수만큼_알림을_저장한다() {
        // given
        Member memberA = Member.builder().memberId(1L).build();
        Member memberB = Member.builder().memberId(2L).build();

        // when
        notificationService.notifyChatroomEvent(List.of(memberA, memberB), 100L, "팀장이 확정되었어요!");

        // then
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).allSatisfy(notification -> {
            assertThat(notification.getCategory()).isEqualTo(NotificationCategory.CHATROOM);
            assertThat(notification.getBody()).isEqualTo("팀장이 확정되었어요!");
            assertThat(notification.getRelatedTeamId()).isEqualTo(100L);
            assertThat(notification.isRead()).isFalse();
        });
        assertThat(saved.get(0).getReceiverMember()).isEqualTo(memberA);
        assertThat(saved.get(1).getReceiverMember()).isEqualTo(memberB);
    }

    @DisplayName("매칭 이벤트는 relatedTeamId 없이 MATCHING 카테고리 알림 한 건을 저장한다.")
    @Test
    void 매칭_이벤트는_relatedTeamId_없이_알림을_저장한다() {
        // given
        Member member = Member.builder().memberId(1L).build();

        // when
        notificationService.notifyMatchingEvent(member, "매칭 신청이 완료되었습니다.");

        // then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getReceiverMember()).isEqualTo(member);
        assertThat(saved.getCategory()).isEqualTo(NotificationCategory.MATCHING);
        assertThat(saved.getBody()).isEqualTo("매칭 신청이 완료되었습니다.");
        assertThat(saved.getRelatedTeamId()).isNull();
    }

    @DisplayName("페이지 크기보다 하나 더 많은 알림이 조회되면 hasNext가 true이고 페이지 크기만큼만 반환된다.")
    @Test
    void 페이지_크기보다_많이_남아있으면_hasNext가_true이다() {
        // given
        List<Notification> twentyOne = java.util.stream.IntStream.range(0, 21)
                .mapToObj(i -> Notification.builder()
                        .receiverMember(Member.builder().memberId(1L).build())
                        .category(NotificationCategory.OTHER)
                        .body("알림 " + i)
                        .build())
                .toList();
        given(notificationRepository.findByReceiverBeforeCursor(
                        org.mockito.ArgumentMatchers.eq(1L), any(), any(), any(Pageable.class)))
                .willReturn(twentyOne);

        // when
        NotificationListResponse response = notificationService.getNotifications(1L, null, null);

        // then
        assertThat(response.hasNext()).isTrue();
        assertThat(response.notifications()).hasSize(20);
    }

    @DisplayName("안읽은 알림 존재 여부와 전체 읽음 처리는 리포지토리에 그대로 위임한다.")
    @Test
    void 안읽음_존재여부와_전체읽음처리는_리포지토리에_위임한다() {
        given(notificationRepository.existsByReceiverMember_MemberIdAndReadFalse(1L))
                .willReturn(true);

        assertThat(notificationService.existsUnread(1L)).isTrue();

        notificationService.markAllAsRead(1L);
        verify(notificationRepository).markAllAsRead(1L);
    }
}
