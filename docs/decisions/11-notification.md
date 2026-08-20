# 11. 알림 (알림함 · 인앱 실시간 배너 · OS 푸시)

## 배경/목적

기존에는 "앱 내 알림과 알림 전달 인프라는 포함하지 않는다"([10-matching-algorithm-detail.md](./10-matching-algorithm-detail.md))로
스코프 밖이었으나, 이번에 그 알림 인프라를 처음 만든다. 요구사항은 세 갈래로 나뉜다.

| 채널 | 포함 대상 | 저장 여부 | 언제 뜨는가 |
|---|---|---|---|
| ① 알림함(`GET /api/notifications`) | 챗봇 카드 메시지, 매칭 신청완료/결과공개 | DB 저장, 회원별 읽음 상태 관리 | 알림함 화면 진입 시 목록 |
| ② 인앱 실시간 배너 | 전체(①에 더해 다른 팀원의 일반 채팅 메시지도 포함) | 저장 안 함 | 앱이 foreground일 때만, 보고 있는 채팅방은 스킵 |
| ③ OS 푸시(폰 알림창) | ②와 동일(전체) | 구독 토큰만 저장 | 앱이 background/종료 상태에서도 |

**이번 PR(Phase 1)은 ①만 구현한다.** ②③은 프론트가 FCM을 얹어야 실제로 동작하므로 Phase 3~4로 미뤘다 —
자세한 내용은 [12-frontend-notification-integration.md](./12-frontend-notification-integration.md)의 "지금 백엔드가
제공하는 것 / 아직 없는 것" 참고. **iOS 홈 화면 추가(PWA 설치) 유도 UI도 프론트에 해당 화면이 아직 없어
Phase 5로 미루고 여기 기록만 남긴다** — iOS Safari는 웹앱이 홈 화면에 설치된 상태가 아니면 OS 푸시 자체가
뜨지 않으므로, ③을 만들 때는 설치 유도 화면이 사실상 필수다.

## 엔티티 · 필드 정의

### Notification

| 필드 | 설명 |
|---|---|
| receiverMember | FK, 알림 수신자 |
| category | `OTHER`(기타/광고성 팝업) / `MATCHING`(신청완료·결과공개) / `CHATROOM`(챗봇 카드) — 프론트 알림함 탭(전체/기타/매칭/채팅방)과 1:1 대응 |
| body | 알림 본문 텍스트. 백엔드가 문구를 소유한다(챗봇 프롬프트를 하드코딩하는 기존 `ChatbotOrchestrationService` 관례와 동일) |
| relatedTeamId | CHATROOM 알림 탭 시 이동할 채팅방. MATCHING/OTHER는 상세 화면이 없어 null |
| isRead | 회원별 읽음 상태 |

> **수신자별로 한 행씩 저장한다.** 팀 채팅의 `TeamMember.lastReadAt` 커서 방식과 달리, 알림함은 팀 단위가
> 아니라 회원 단위 전역 피드라 커서로는 "읽음"을 표현할 수 없다 — 챗봇 카드 하나가 팀원 4명에게 전달되면
> Notification 행도 4개 생긴다.

> **title/metadata 필드를 두지 않았다.** 처음엔 chat의 `Message.metadata`(JSON 문자열로 카드가 참조하는
> id를 담는 방식)를 그대로 가져올까 고민했는데, 알림함은 목록 미리보기 텍스트만 있으면 되고 상세 화면으로
> 진입할 때 필요한 데이터는 `relatedTeamId`(CHATROOM)만으로 충분하다고 판단했다. MATCHING 알림을 탭했을 때
> 오늘의 매칭 결과 화면으로 보내는 것도 별도 id 없이 고정 경로로 가능하다. 나중에 알림 종류가 늘어나
> 딥링크에 더 많은 정보가 필요해지면 그때 JSON 컬럼을 추가한다(YAGNI).

## 카테고리별 생성 트리거

### CHATROOM — `ChatService.postChatbotMessage` / `postChatbotCardMessage`

챗봇이 채팅방에 메시지를 남기는 두 진입점(`ChatbotOrchestrationService`가 호출)에 중앙으로 걸었다 —
`Team.chatbotEnabled=false`면 애초에 메시지 자체가 생략되므로 알림도 자연히 생략된다. **다른 팀원이 보낸
일반 텍스트(`sendMessage`)와 `SYSTEM_NOTICE`(`postSystemMessage`, 나가기/챗봇 토글 안내)는 알림함 대상이
아니다** — 요구사항이 "챗봇이 보내는 채팅/팝업"과 매칭 알림만 알림함에 쌓이길 원했기 때문. 알림 본문은
채팅에 실제로 남는 `content`를 그대로 재사용한다(챗봇 카드마다 다른 문구를 알림용으로 다시 하드코딩하지
않기 위해).

### MATCHING — 신청 완료

`MatchingApplicationService.apply()`가 `matchingApplicationRepository.save(application)` 직후 호출.
본문은 고정 문구("매칭 신청이 완료되었습니다.").

### MATCHING — 결과 공개

매칭 결과는 [10-matching-algorithm-detail.md](./10-matching-algorithm-detail.md)/`MatchingResultQueryService`
설계상 **이벤트가 아니라 시간 게이트(`MatchingTimePolicy.isResultPublished`, 기본 16시)로 조회 시점에
계산되는 값**이라, 채팅처럼 "결과가 확정되는 순간" 자연스럽게 걸 수 있는 훅이 없다. 그래서 새 스케줄러
(`MatchingResultNotificationJobs`, `@Scheduled(cron = "0 0 16 * * *")` + ShedLock, `MatchingSchedulerJobs`의
14시 배치 트리거와 동일한 패턴)를 만들어 결과 공개 시각에 그날 신청 중 확정 결과가 나온 사람들에게
일괄 알림을 남긴다.

> **cron이 `matching.result-publish-time` 프로퍼티(application.yml, 기본 16:00)와 별도로 하드코딩돼
> 있다.** Spring `@Scheduled(cron=...)`에는 `LocalTime` 프로퍼티를 직접 꽂을 수 없다 — 기존
> `MatchingSchedulerJobs`가 14시 매칭 배치를 `APPLICATION_DEADLINE` 상수와 별개로 하드코딩 cron
> `"0 0 14 * * *"`로 트리거하는 것과 같은 제약이다. **운영 중 `MATCHING_RESULT_PUBLISH_TIME` 환경변수를
> 바꾸면 이 cron도 반드시 함께 바꿔야 한다** — 안 바꾸면 결과는 정상적으로 새 시각에 공개되는데 알림만
> 예전 16시에 (또는 엉뚱한 시각에) 날아가는 불일치가 생긴다.

> **알림 대상 신청 상태를 `PROPOSED`/`MATCHED`/`REASSIGN_PENDING`/`FAILED`로 골랐다.**
> `MatchingResultQueryService.toResult()`를 보면 16시 이후 이 네 상태만 "확정된 결과"로 상세 화면을
> 보여주고, `WAITING`/`MATCHING`은 아직 계산 중(PROCESSING), `CANCELED`/`PASSED`는 본인이 이미 철회한
> 신청이라 새삼 "결과가 공개됐다"고 알릴 대상이 아니라고 판단했다. **다만 이 매핑은 매칭 도메인 코드를
> 읽고 추론한 것이라 확신도가 100%는 아니다** — 특히 `PASSED`(오후 2시 이후 철회)가 그룹에 이미 속해있던
> 경우 결과 화면에 뭔가 보여주는 분기가 있어(`toResult` 82~93행), 이들에게도 "결과가 공개됐다"는 알림이
> 필요한지는 매칭 도메인 담당자 확인이 필요하다. 실사용 중 대상이 이상하면
> `MatchingApplicationService.RESULT_PUBLISHED_NOTIFIABLE_STATUSES`만 고치면 된다.

## API

- `GET /api/notifications?category=&cursor=` — category 생략 시 전체, cursor는 채팅 메시지 목록
  (`MessageRepository.findByTeamIdBeforeCursor`)과 동일한 방식(직전 응답의 가장 오래된 `notificationId`를
  다시 넘기면 이어서 조회). 페이지 크기 20.
- `GET /api/notifications/unread-exists` — 홈 화면 종 아이콘 빨간 배지 표시 여부.
- `PATCH /api/notifications/read-all` — **카테고리 탭과 무관하게 전체를 읽음 처리한다.** 요구사항이
  "알림창에 한 번 들어가면 회색 표시가 전부 사라져야 한다"였기 때문에, 알림함 화면에 진입하는 시점(탭
  전환이 아니라)에 한 번 호출하면 된다. 건별 UPDATE 대신 `@Modifying` 벌크 쿼리로 처리해 N+1을 피했다.

## 결정사항

- **알림 생성은 트랜잭션 경계를 새로 만들지 않고 호출부(ChatService/MatchingApplicationService)의 기존
  트랜잭션에 얹었다.** 챗봇 메시지 저장·매칭 신청 저장과 알림 적립이 원자적으로 같이 실패/성공한다 —
  메시지는 남았는데 알림만 빠지는 정합성 문제를 피할 수 있다. 알림 저장 실패가 메시지/신청 자체를
  롤백시킬 만큼 치명적인지는 추후 재검토 여지가 있다(현재는 "알림 유실보다 정합성"을 우선함).
- 알림 도메인은 `member`/`team`을 참조하지만 `chat`/`matching`이 `notification`을 참조하는 단방향
  의존성만 만들었다 — `report` 도메인이 `member`/`team`을 참조하는 기존 구조와 동일한 패턴.

## 구현 현황 (Phase 1, 2026-08-20)

- 엔티티: `domains/notification/entity/Notification.java`, `enums/NotificationCategory.java`
- 마이그레이션: `V41__create_notifications.sql`
- 리포지토리: `domains/notification/repository/NotificationRepository.java` — cursor 조회, `existsBy...ReadFalse`,
  `markAllAsRead`(벌크 UPDATE)
- 서비스: `domains/notification/service/NotificationService.java` — `notifyChatroomEvent`(팀원 리스트 →
  saveAll), `notifyMatchingEvent`(단건), `getNotifications`, `existsUnread`, `markAllAsRead`
- 컨트롤러: `domains/notification/controller/NotificationController.java` — 위 API 3종
- 훅: `ChatService.postChatbotMessage`/`postChatbotCardMessage`(CHATROOM),
  `MatchingApplicationService.apply`(MATCHING 신청완료), `MatchingApplicationService.notifyTodayResultPublished`
  + `domains/scheduler/MatchingResultNotificationJobs`(MATCHING 결과공개)
- 테스트: `NotificationServiceTest`(신규), `ChatServiceTest`/`MatchingApplicationServiceTest`에 알림 생성
  검증 케이스 추가

### 코드리뷰 findings 반영 (2026-08-20)

PR #199 리뷰에서 나온 11개 findings 중 정확성 상위 3개를 같은 PR에 추가 커밋으로 반영했다.

- **`Notification.body` VARCHAR(500) → TEXT (`V42__widen_notification_body.sql`)**: 챗봇 자유질의(`@챗봇`)
  응답은 길이 제한이 없는데(`Message.content`는 TEXT) `body`가 500자로 잘려있어, 500자를 넘으면 INSERT
  실패로 트랜잭션이 롤백됐다. 문제는 `broadcast()`(WebSocket 전송, 되돌릴 수 없음)가 알림 저장보다 먼저
  실행돼서, 롤백돼도 이미 브로드캐스트된 메시지는 되돌아오지 않았다 — 컬럼을 TEXT로 넓히는 것과 함께
  `ChatService.postChatbotMessage`/`postChatbotCardMessage`에서 `notifyActiveMembers`(알림 저장)를
  `broadcast()`보다 먼저 호출하도록 순서를 바꿨다. 이제 알림 저장이 어떤 이유로든 실패해도, 아직 아무 것도
  브로드캐스트되지 않은 채로 트랜잭션이 롤백된다.
- **`MatchingApplicationRepository.findAllByApplicationDateAndStatusInWithMember`에 `member.status = ACTIVE`
  조건 추가**: `MemberWithdrawService`가 `MATCHED`/`FAILED` 상태의 탈퇴는 막지 않아, 결과 확정 직후
  탈퇴한 회원에게도 알림 행이 생기던 문제를 `findUnpreparedWaitingWithLock`과 동일한 방어 패턴으로 막았다.
- **`category` 잘못된 값 → 400**: `NotificationErrorCode.INVALID_CATEGORY`/`NotificationException` 추가,
  컨트롤러가 `NotificationCategory` 대신 `String`으로 받아 `NotificationConverter.toNotificationCategory`
  (report 도메인의 `ReportConverter.toReportReason`과 동일한 패턴)에서 직접 검증하도록 바꿨다. Spring의
  기본 enum 바인딩(`MethodArgumentTypeMismatchException`)에 맡기면 전역 예외 처리기가 못 잡아 500이
  나가던 문제였다.

나머지 findings(결과공개 `PASSED` 상태 제외, cron 하드코딩 드리프트, 스케줄러 레이스, 중복 쿼리, 인덱스
미커버, N+1 저장, `markRead()` 죽은 코드)는 이번엔 반영하지 않았다 — PR #199 "To Reviewer" 체크리스트에
남아있다.

## 미정 / 추후 확인 필요

- **결과 공개 알림 대상 상태(`PASSED` 포함 여부)** — 위 "결정사항" 단락 참고, 매칭 도메인 담당자 확인 필요.
  아직 미반영(PR #199 참고).
- 알림 삭제/보관 정책 없음 — 무한히 쌓인다. 트래픽이 늘면 오래된 read=true 알림을 주기적으로 정리하는
  배치가 필요할 수 있다.
- Phase 2(프론트 실데이터 연동), Phase 3~4(FCM 인앱 배너 + OS 푸시), Phase 5(iOS PWA 설치 유도)는
  [12-frontend-notification-integration.md](./12-frontend-notification-integration.md) 참고.

## 관련 화면

알림 화면(벨 아이콘, 탭: 전체/기타/매칭/채팅방), 홈 화면 종 아이콘 빨간 배지.
