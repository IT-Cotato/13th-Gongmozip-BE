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

### CHATROOM — `ChatService.postChatbotCardMessage` (카드형만, 2026-08-21 범위 축소)

**최초 구현은 `postChatbotMessage`(일반 텍스트)까지 전부 알림 대상이었는데, 실사용 중 알림함이 너무
시끄러워져서 카드형(`postChatbotCardMessage`)만 남기고 좁혔다.** 특히 "@챗봇" 자유질의 응답
(`postChatbotMessage`로 발행)은 질문할 때마다 팀 전체에게 알림이 갔는데, 개인 질문에 대한 답이라 팀
전체 알림함에 쌓일 이유가 없었다. 인사 유도/진행 안내/리뷰 완료 같은 다른 일반 텍스트 프롬프트도 마찬가지로
제외했다 — 카드형 메시지(팀장 확정, 공모전 확정, 투표 시작/리마인더 등 구조화된 1회성 이벤트)만 알림
가치가 있다고 판단했다.

카드형 중에서도 `CHATBOT_GUIDE_CARD`("활용 예시", `advanceToInProgress`에서 발행)는 제외했다 — 정적
안내 카드라 액션이 필요 없어서다. `ChatService.postChatbotCardMessage`가 `messageType !=
CHATBOT_GUIDE_CARD`일 때만 알림을 남기도록 가드한다.

`Team.chatbotEnabled=false`면 애초에 메시지 자체가 생략되므로 알림도 자연히 생략된다. **다른 팀원이 보낸
일반 텍스트(`sendMessage`)와 `SYSTEM_NOTICE`(`postSystemMessage`, 나가기/챗봇 토글 안내)는 처음부터 알림함
대상이 아니다.** 알림 본문은 채팅에 실제로 남는 `content`를 그대로 재사용한다(챗봇 카드마다 다른 문구를
알림용으로 다시 하드코딩하지 않기 위해).

> **알려진 별도 이슈 — 제출확인 재알림 스팸**: `TeamScheduleService.sendSubmissionCheckReminderForTeam`이
> `SUBMISSION_CHECK_CARD`(카드형)를 "진행완료" 미응답 시 **2시간마다 무한 반복** 발행한다. 이건 알림
> 필터링 범위와 무관하게 채팅방 자체에도 계속 쌓이는 별도 버그라, 스케줄러 쪽에서 반복 횟수 제한이나
> 최초 1회 이후 빈도 완화가 필요하다 — 아직 미해결.

### MATCHING — 신청 완료

`MatchingApplicationService.apply()`가 `matchingApplicationRepository.save(application)` 직후 호출.
본문은 고정 문구("매칭 신청이 완료되었습니다.").

### MATCHING — 결과 공개

매칭 결과는 [10-matching-algorithm-detail.md](./10-matching-algorithm-detail.md)/`MatchingResultQueryService`
설계상 **이벤트가 아니라 시간 게이트(`MatchingTimePolicy.isResultPublished`, 기본 16시)로 조회 시점에
계산되는 값**이라, 채팅처럼 "결과가 확정되는 순간" 자연스럽게 걸 수 있는 훅이 없다. 그래서 새 스케줄러
(`MatchingResultNotificationJobs`)를 만들어 결과 공개 시각에 그날 신청 중 확인할 결과가 있는 사람들에게
일괄 알림을 남긴다.

> **cron을 고정 시각으로 하드코딩하지 않고 5분마다 폴링한다 (2026-08-20, 코드리뷰 findings 반영).**
> 최초 구현은 `@Scheduled(cron = "0 0 16 * * *")`로 `matching.result-publish-time` 프로퍼티(기본 16:00)와
> 같은 값을 손으로 맞춰뒀었는데, 운영 중 그 프로퍼티만 바꾸면 실제 공개 시각과 알림 발송 시각이 조용히
> 어긋나는 문제가 있었다. `MatchingResponseDeadlineJobs`와 동일한 5분 폴링(`"0 */5 * * * *"`)으로 바꾸고,
> 실제 발송 여부는 매번 `MatchingTimePolicy.isResultPublished`로 그 시점의 설정값을 직접 확인하도록
> `MatchingApplicationService.notifyTodayResultPublishedIfDue()`에 위임했다 — 프로퍼티가 유일한 진실
> 공급원이 되어 더 이상 두 값이 어긋날 수 없다. 폴링마다 중복 발송되지 않도록
> `matching_result_notification_logs`(신청일 유니크)에 발송 여부를 기록해 하루 한 번만 보낸다 — 멱등성
> 체크와 로그 저장 사이의 경쟁은 스케줄러의 `@SchedulerLock`이 모든 실행을 같은 락 이름으로 직렬화해서
> 막는다(동시에 두 인스턴스가 실행되지 않음).

> **알림 대상 신청 상태에 `PASSED`를 조건부로 포함한다 (2026-08-20, 코드리뷰 findings 반영).**
> `MatchingResultQueryService.toResult()`를 다시 확인해보니, `PASSED`는 두 경우가 섞여 있었다 — 그룹
> 배정 **전**에 철회했으면(멤버십 없음) 볼 결과가 없어 즉시 `WITHDRAWN` 응답, 배정 **후**에 철회했으면
> (멤버십 있음) 공개 시각 이후 `WITHDRAWN` + 그룹 상세를 보여주는 실제 결과 화면을 받는다. 후자는 확인할
> 결과가 있는데 알림에서 빠져있던 버그였다. `notifyTodayResultPublished`가 `toResult()`와 같은 기준
> (`matchingGroupMemberRepository.findResultMembership`)으로 이 둘을 갈라 멤버십이 있는 `PASSED`만
> 알림 대상에 포함하도록 `hasPublishedResult()`를 추가했다 — `toResult()`를 직접 재사용하지는 않고
> 판정 기준(같은 리포지토리 메서드)만 공유한다.

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
  `MatchingApplicationService.apply`(MATCHING 신청완료), `MatchingApplicationService.notifyTodayResultPublishedIfDue`
  (5분마다 발행 여부를 직접 확인 → `notifyTodayResultPublished`) + `domains/scheduler/MatchingResultNotificationJobs`
  (MATCHING 결과공개). 멱등성은 `domains/matching/entity/MatchingResultNotificationLog`
  (`V43__create_matching_result_notification_logs.sql`, 신청일 유니크)가 보장한다.
- 테스트: `NotificationServiceTest`(신규), `ChatServiceTest`/`MatchingApplicationServiceTest`에 알림 생성
  검증 케이스 추가

### 코드리뷰 findings 반영 (2026-08-20)

PR #199 리뷰에서 나온 11개 findings 중 5개를 같은 PR에 추가 커밋으로 반영했다.

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
- **결과공개 알림 대상에 `PASSED`(그룹 배정 후 철회) 포함** — 위 "MATCHING — 결과 공개" 단락 참고.
- **`MatchingResultNotificationJobs`의 cron 하드코딩 드리프트 제거** — 위 "MATCHING — 결과 공개" 단락 참고.

나머지 findings(스케줄러 레이스, 중복 쿼리, 인덱스 미커버, N+1 저장, `markRead()` 죽은 코드)는 이번엔
반영하지 않았다 — PR #199 "To Reviewer" 체크리스트에 남아있다. 특히 "스케줄러 레이스"(`MatchingResponseDeadlineJobs`와
동시 실행) 항목은 이번에 `MatchingResultNotificationJobs`도 같은 5분 폴링 주기로 바뀌면서 두 잡이 같은
간격으로 나란히 도는 구조가 됐다 — 서로 다른 락 이름(`SchedulerLock`)을 쓰고 각자 자기 소관 데이터만
다루므로(하나는 `MatchingGroupMember`, 하나는 `MatchingApplication`/`Notification` 읽기 전용에 가까움)
직접 충돌하진 않지만, 리뷰에서 지적한 "같은 시각에 상태를 읽는 쪽과 바꾸는 쪽이 겹칠 수 있다"는 원래
우려 자체는 두 잡이 여전히 별개 트랜잭션이라 완전히 해소되진 않았다.

## 미정 / 추후 확인 필요
- **제출확인 재알림 2시간마다 무한 반복 버그** — 위 "CHATROOM" 절 참고, `TeamScheduleService` 쪽 수정 필요.
- 알림 삭제/보관 정책 없음 — 무한히 쌓인다. 트래픽이 늘면 오래된 read=true 알림을 주기적으로 정리하는
  배치가 필요할 수 있다.
- Phase 2(프론트 실데이터 연동), Phase 3~4(FCM 인앱 배너 + OS 푸시), Phase 5(iOS PWA 설치 유도)는
  [12-frontend-notification-integration.md](./12-frontend-notification-integration.md) 참고.

## 관련 화면

알림 화면(벨 아이콘, 탭: 전체/기타/매칭/채팅방), 홈 화면 종 아이콘 빨간 배지.
