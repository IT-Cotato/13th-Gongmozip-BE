package org.cotato.gongmozip.domains.notification.repository;

import java.util.List;
import org.cotato.gongmozip.domains.notification.entity.Notification;
import org.cotato.gongmozip.domains.notification.enums.NotificationCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 채팅 메시지 cursor 페이지네이션(MessageRepository)과 동일한 방식: cursor가 없으면 최신
    // 페이지를, 있으면 그보다 notificationId가 작은(더 오래된) 알림을 이어서 조회한다.
    @Query(
            """
            SELECT n FROM Notification n
            WHERE n.receiverMember.memberId = :memberId
            AND (:category IS NULL OR n.category = :category)
            AND (:cursor IS NULL OR n.notificationId < :cursor)
            ORDER BY n.notificationId DESC
            """)
    List<Notification> findByReceiverBeforeCursor(
            @Param("memberId") Long memberId,
            @Param("category") NotificationCategory category,
            @Param("cursor") Long cursor,
            Pageable pageable);

    boolean existsByReceiverMember_MemberIdAndReadFalse(Long memberId);

    // 알림함 진입 한 번으로 전체를 읽음 처리하므로 건별 조회 없이 일괄 UPDATE로 처리한다.
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.receiverMember.memberId = :memberId AND n.read = false")
    void markAllAsRead(@Param("memberId") Long memberId);
}
