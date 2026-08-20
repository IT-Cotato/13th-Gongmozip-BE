package org.cotato.gongmozip.domains.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.notification.enums.NotificationCategory;
import org.cotato.gongmozip.global.entity.BaseEntity;

/**
 * 알림함(GET /api/notifications)에 쌓이는 알림 한 건. 수신자별로 한 행씩 저장한다 —
 * 채팅방 챗봇 카드처럼 여러 팀원이 동시에 받는 알림도 팀원 수만큼 행을 만들어, 각자의
 * 읽음 상태를 독립적으로 관리한다(팀 채팅의 lastReadAt 커서 방식과 달리, 알림함은 팀
 * 단위가 아닌 회원 단위 전역 피드라 커서로는 "읽음"을 표현할 수 없다).
 *
 * <p>다른 팀원이 보낸 일반 채팅 텍스트 메시지는 여기 저장하지 않는다(docs/decisions/11-notification.md) —
 * 챗봇이 채팅방에 남기는 카드형 안내(CHATROOM), 매칭 신청/결과(MATCHING), 그 외 공지성 팝업(OTHER)만 대상.
 */
@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id", nullable = false, updatable = false)
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_member_id", nullable = false)
    private Member receiverMember;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private NotificationCategory category;

    @Column(name = "body", nullable = false, length = 500)
    private String body;

    // CHATROOM 알림을 탭했을 때 이동할 채팅방. MATCHING/OTHER는 별도 상세 화면이 없어 null.
    @Column(name = "related_team_id")
    private Long relatedTeamId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    public void markRead() {
        this.read = true;
    }
}
