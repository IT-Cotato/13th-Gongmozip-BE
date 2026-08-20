package org.cotato.gongmozip.domains.notification.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.notification.converter.NotificationConverter;
import org.cotato.gongmozip.domains.notification.dto.response.NotificationResponse.NotificationListResponse;
import org.cotato.gongmozip.domains.notification.entity.Notification;
import org.cotato.gongmozip.domains.notification.enums.NotificationCategory;
import org.cotato.gongmozip.domains.notification.repository.NotificationRepository;
import org.cotato.gongmozip.global.push.PushPayload;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림함(GET /api/notifications)에 쌓이는 알림의 조회·읽음 처리와, 다른 도메인이 알림을 적립할 때
 * 쓰는 진입점을 함께 제공한다. 챗봇 카드 알림은 ChatService(postChatbotMessage/postChatbotCardMessage),
 * 매칭 알림은 MatchingApplicationService(apply)와 스케줄러(MatchingResultNotificationJobs)가 호출한다.
 * 자세한 설계 배경은 docs/decisions/11-notification.md, OS 푸시 연동은 docs/decisions/13-fcm-push.md 참고.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    // Figma에 명시된 푸시 제목이 없어 임시로 고정 문구를 쓴다 — 추후 조정 대상(docs/decisions/13-fcm-push.md).
    private static final String PUSH_TITLE = "공모집";

    private final NotificationRepository notificationRepository;
    private final PushNotificationService pushNotificationService;

    /** 팀원 여러 명에게 동시에 남기는 챗봇 카드 알림(CHATROOM 카테고리) — 팀원 수만큼 알림 행을 생성하고, 같은 내용을 OS 푸시로도 보낸다. */
    @Transactional
    public void notifyChatroomEvent(List<Member> receivers, Long teamId, String body) {
        List<Notification> notifications = receivers.stream()
                .map(receiver ->
                        NotificationConverter.toNotification(receiver, NotificationCategory.CHATROOM, body, teamId))
                .toList();
        notificationRepository.saveAll(notifications);
        pushNotificationService.sendToMembers(
                receivers, new PushPayload(PUSH_TITLE, body, Map.of("teamId", String.valueOf(teamId))));
    }

    /** 매칭 신청 완료/결과 공개 등 MATCHING 카테고리 알림. relatedTeamId는 아직 팀이 없으므로 null. 같은 내용을 OS 푸시로도 보낸다. */
    @Transactional
    public void notifyMatchingEvent(Member receiver, String body) {
        notificationRepository.save(
                NotificationConverter.toNotification(receiver, NotificationCategory.MATCHING, body, null));
        pushNotificationService.sendToMembers(List.of(receiver), new PushPayload(PUSH_TITLE, body, Map.of()));
    }

    public NotificationListResponse getNotifications(Long memberId, NotificationCategory category, Long cursor) {
        List<Notification> fetched = notificationRepository.findByReceiverBeforeCursor(
                memberId, category, cursor, PageRequest.of(0, DEFAULT_PAGE_SIZE + 1));
        boolean hasNext = fetched.size() > DEFAULT_PAGE_SIZE;
        List<Notification> page = hasNext ? fetched.subList(0, DEFAULT_PAGE_SIZE) : fetched;
        return NotificationConverter.toListResponse(page, hasNext);
    }

    public boolean existsUnread(Long memberId) {
        return notificationRepository.existsByReceiverMember_MemberIdAndReadFalse(memberId);
    }

    /** 알림함 화면 진입 시 호출 — 카테고리 탭과 무관하게 전체를 읽음 처리한다(요구사항: 알림함에 한 번 들어가면 회색 표시가 전부 사라져야 함). */
    @Transactional
    public void markAllAsRead(Long memberId) {
        notificationRepository.markAllAsRead(memberId);
    }
}
