# 13. OS 푸시 (FCM) — 설계

## 배경/목적

[11-notification.md](./11-notification.md)에서 정리한 세 채널 중 ③(OS 푸시, 폰 알림창)을 위한 백엔드
설계다. Phase 1(알림함)에 이어 이번 Phase 3도 구현·merge·운영 배포까지 완료했다(#200, PR #201) — 아래
"구현 현황"과 [12-frontend-notification-integration.md](./12-frontend-notification-integration.md) 참고.

Phase 1과 달리 이번엔 알림함(저장)과 무관하게 **두 개의 서로 다른 이벤트가 각각 푸시를 트리거**한다.

| 트리거 | 저장(알림함) | 푸시 발송 |
|---|---|---|
| 챗봇 카드 메시지 (`ChatService.postChatbotMessage`/`postChatbotCardMessage`) | ✅ CHATROOM | ✅ |
| 매칭 신청완료/결과공개 (`NotificationService.notifyMatchingEvent`) | ✅ MATCHING | ✅ |
| 다른 팀원의 일반 채팅 텍스트 (`ChatService.sendMessage`) | ❌ (알림함 대상 아님, 11-notification.md 참고) | ✅ |

즉 "알림함에 쌓이는 것"과 "푸시가 나가는 것"의 대상이 다르다 — 일반 채팅 메시지는 알림함엔 안 쌓이지만
푸시는 나가야 한다(최초 요구사항: "채팅에서 챗봇이든, 팝업이든, 다른 사람의 메세지든" 전부 푸시).

## 엔티티 · 필드 정의

### PushToken

| 필드 | 설명 |
|---|---|
| member | FK, 토큰 소유자 |
| token | FCM 등록 토큰 문자열, **유니크** |

- 회원 1명이 여러 토큰을 가질 수 있다(폰 + 데스크톱 브라우저 등 다중 기기) — `member` 기준 1:N.
- `token` 자체가 유니크한 이유: 같은 물리 기기에서 로그아웃 후 다른 계정으로 로그인하면 같은 토큰이 새
  회원에게 재등록돼야 한다(FCM 토큰은 기기+앱 인스턴스 단위지 계정 단위가 아님). 등록 API는 그래서
  "토큰 upsert"로 동작한다 — 이미 존재하는 토큰이면 `member`만 갈아끼운다.
- 만료/무효 토큰은 발송 실패 시 정리한다 — 아래 "무효 토큰 정리" 참고.

## API (모두 `notification` 도메인에 추가)

| Method | Path | 설명 |
|---|---|---|
| POST | `/api/notifications/push-tokens` | body `{ token }` — 로그인한 회원에 토큰 등록(upsert) |
| DELETE | `/api/notifications/push-tokens` | body `{ token }` — 로그아웃 시 토큰 해제(권장, 필수는 아님) |

프론트에서 FCM SDK로 토큰을 발급받은 뒤 로그인 직후(또는 알림 권한을 막 허용한 시점) 호출한다. 자세한
프론트 연동 순서는 이 설계가 구현된 뒤 [12-frontend-notification-integration.md](./12-frontend-notification-integration.md)에
추가한다.

## 발송 아키텍처

### 서비스 분리: 저장(NotificationService)과 발송(PushNotificationService)

Phase 1의 `NotificationService`는 "알림함에 뭘 남길지"만 책임진다. 이번에 새로 만드는
`PushNotificationService`는 "누구에게 무엇을 푸시로 보낼지"만 책임지고, 이 둘은 서로 독립적으로 호출된다
— 알림함 저장 여부와 푸시 발송 여부의 대상이 다르기 때문(위 표 참고).

```
ChatService.postChatbotMessage/postChatbotCardMessage
    └─ notificationService.notifyChatroomEvent(...)   // 저장 + 그 안에서 pushNotificationService 호출
ChatService.sendMessage (일반 텍스트)
    └─ pushNotificationService.sendToMembers(...)      // 저장 없이 발송만
MatchingApplicationService.apply / notifyTodayResultPublished
    └─ notificationService.notifyMatchingEvent(...)    // 저장 + 그 안에서 pushNotificationService 호출
```

`NotificationService.notifyChatroomEvent`/`notifyMatchingEvent` 내부에서 저장 직후
`pushNotificationService.sendToMembers(...)`를 호출하도록 한 줄만 추가하면 되고, 기존 호출부
(ChatService의 챗봇 훅, MatchingApplicationService)는 손댈 필요가 없다. `ChatService.sendMessage`만
새로 `pushNotificationService`를 직접 호출하도록 바뀐다(발신자 본인은 제외).

### 반드시 트랜잭션 커밋 이후, 비동기로 발송한다

FCM 호출은 외부 네트워크 I/O다. 두 가지를 반드시 지켜야 한다 — 어기면 이번 코드리뷰에서 고친
VARCHAR(500)/브로드캐스트 순서 버그와 같은 종류의 정합성 문제가 재발한다.

1. **트랜잭션 커밋 이후에만 실제 전송한다.** `ChatbotOrchestrationService.runAfterCommit`과 동일한
   패턴(`TransactionSynchronizationManager.registerSynchronization` + `afterCommit()`)을 그대로
   재사용한다 — 커밋 전에 보내면, 이후 같은 트랜잭션에서 예외가 나 롤백돼도 이미 사용자 폰에는 알림이
   뜬 뒤라 되돌릴 수 없다.
2. **요청 처리 스레드/스케줄러 스레드를 막지 않는다.** `AsyncConfig`에 전용 executor(`pushExecutor`,
   `AiSummaryExecutor`와 동일한 패턴)를 추가해 그 위에서 실행한다. FCM 응답이 느려도 채팅 메시지 전송이나
   매칭 결과공개 스케줄러가 지연되지 않는다.

### FcmClient 추상화 (global/push)

`global/ai/AiClient`+`AiGatewayClient`+`MockAiClient` 패턴을 그대로 따른다.

- `FcmClient` 인터페이스: `PushSendResult send(String token, PushPayload payload)`, `boolean isEnabled()`
- `FirebaseFcmClient`: Firebase Admin SDK로 실제 발송. Firebase 자격증명이 설정 안 돼 있으면
  `isEnabled() == false`를 반환해 발송을 조용히 건너뛴다(로컬/테스트 환경에서 `AiGatewayClient.isEnabled()`가
  API 키 없을 때 하는 것과 동일) — **자격증명 없이도 나머지 코드는 전부 컴파일·테스트 가능하고, 크리덴셜이
  준비되는 순간 바로 켜진다.**
- `PushPayload(String title, String body, Map<String, String> data)` — `data`에 딥링크용 최소 정보만
  담는다. CHATROOM/일반 채팅 메시지 유래 푸시는 `teamId`를 담아야, 프론트가 foreground 시 "지금 그 방을
  보고 있으면 표시 안 함" 판단을 할 수 있다([12-frontend-notification-integration.md](./12-frontend-notification-integration.md)의
  "인앱 배너" 절 참고 — 이 판단은 프론트가 로컬로 하고 서버는 필요한 데이터만 실어 보낸다).

### 무효 토큰 정리

FCM이 `UNREGISTERED`(기기에서 앱 삭제/알림 권한 철회 등)를 응답하면 그 `PushToken` 행을 즉시 삭제한다.
정리를 안 하면 죽은 토큰에 계속 발송을 시도해 쓸모없는 API 호출이 쌓인다. `PushNotificationService`가
발송 결과를 받아 처리한다(재시도하지 않음 — 실패 자체가 "이 토큰은 더 이상 유효하지 않다"는 신호).

## 결정사항

- **알림함 저장(NotificationService)과 푸시 발송(PushNotificationService)을 분리한다** — 위 "발송
  아키텍처" 참고. 대상 회원 집합이 다르고(일반 채팅 메시지는 푸시만, 저장 안 함), 책임도 다르다(하나는
  DB 쓰기, 하나는 외부 API 호출 + 재시도 없음 + 커밋 후 비동기).
- **Notification 저장 실패가 푸시 발송을 막지 않는다.** 반대로 푸시 발송 실패(FCM 장애 등)도 알림함
  저장이나 채팅 메시지 자체를 롤백시키지 않는다 — 커밋 후 비동기이므로 애초에 같은 트랜잭션이 아니다.
- FCM을 택한 이유(순수 Web Push 대신)는 [11-notification.md](./11-notification.md) 이전 논의에서 이미
  결정됨 — 브라우저 호환성을 Google이 관리해줌.

## 미정 / 결정 필요

- **Firebase 프로젝트 및 서비스 계정 키** — 해결됨(아래 "구현 현황" 참고). 학교 Google Workspace
  조직(skuniv.ac.kr) 계정 밑에 만들어졌다 — 조직 정책상 개인 계정으로 못 만들고 이 방법뿐이었다.
  팀원 초대가 막히면 그때 개인 계정으로 재생성을 검토한다.
- **서비스 계정 키 전달 방식** — 해결됨. `AI_GATEWAY`처럼 단순 환경변수를 택했고, JSON은 개행이 많아
  base64로 인코딩해 `FIREBASE_CREDENTIALS_BASE64` 하나에 담았다.
- **푸시 제목(title) 문구** — 임시로 모든 카테고리에 고정 문구 `"공모집"`을 쓴다
  (`NotificationService.PUSH_TITLE`, `ChatService.PUSH_TITLE`). Figma에 명시된 게 없어 우선 이렇게
  두었고, 카테고리별로 다르게 할지는 추후 조정 대상(미해결).

## 구현 현황 (2026-08-20)

- 엔티티/리포지토리/마이그레이션: `domains/notification/entity/PushToken.java`,
  `repository/PushTokenRepository.java`, `V44__create_push_tokens.sql`
- API: `domains/notification/controller/PushTokenController.java` —
  `POST /api/notifications/push-tokens`(등록, upsert), `DELETE /api/notifications/push-tokens`(해제)
- `global/push` — `FcmClient`(인터페이스), `PushPayload`, `PushSendResult`, `FirebaseFcmClient`
  (`firebase.credentials-base64`가 비어있으면 `isEnabled()=false`)
- `global/config/AsyncConfig`에 `pushExecutor` 빈 추가, `build.gradle`에 `com.google.firebase:firebase-admin:9.4.1`
- `domains/notification/service/PushNotificationService`(커밋 후 발송 예약) +
  `PushDispatchService`(실제 `@Async` 발송 + 무효 토큰 정리) — 두 빈으로 분리한 이유는 `@Async`가 같은
  빈 안에서 self-invocation으로는 동작하지 않기 때문(Spring AOP 프록시를 안 거침)
- 훅 연결: `NotificationService.notifyChatroomEvent`/`notifyMatchingEvent`(저장 직후 발송 예약),
  `ChatService.sendMessage`(발신자 제외 발송 예약) — `ChatService.notifyOtherMembersPush`
- 테스트: `PushTokenServiceTest`, `PushNotificationServiceTest`(`TransactionSynchronizationManager` 수동
  초기화/afterCommit 트리거 패턴, `ChatbotOrchestrationServiceTest`와 동일), `PushDispatchServiceTest`,
  `PushTokenControllerTest`, `ChatServiceTest`/`NotificationServiceTest`에 발송 호출 검증 추가

**Firebase 프로젝트 생성 및 서비스 계정 키 발급 완료 (2026-08-20).** `firebase.credentials-base64`가
비어있으면 `FirebaseFcmClient.isEnabled()`가 false를 반환해 발송을 스킵하는 fail-safe는 여전히 유효하다.
로컬 `.env`, **운영(EC2) `.env`** 둘 다 실제 값이 채워졌고, 운영 컨테이너도 `--force-recreate`로 재생성해
반영을 확인했다(단순 `restart`는 `env_file` 변경을 다시 안 읽어서 재생성이 필요했다).

**로컬 환경에서 실제 자격증명으로 검증 완료**:
- 앱을 실제로 기동해 `FirebaseFcmClient`가 싱글턴 빈으로 정상 생성됨(실패했다면 컨텍스트 기동 자체가
  실패했을 것 — 별도 초기화 성공 로그는 없지만 기동 성공이 곧 증거)
- `POST /api/notifications/push-tokens` 실제 호출 → DB에 정상 저장 확인
- 임시 테스트로 `FirebaseFcmClient.send()`를 더미 토큰으로 직접 호출 → FCM 서버로부터 HTTP 400(Bad
  Request, 더미 토큰이 유효한 형식이 아니라서 나는 정상 응답)을 받음 — 401/403이 아니므로 인증 자체는
  통과했다는 뜻.

**FE 연동에 필요한 웹 앱 설정값도 이미 발급받아 전달했다** — `firebaseConfig`(apiKey/authDomain/projectId/
storageBucket/messagingSenderId/appId) + VAPID 키(웹 푸시 인증서). 값 자체는 시크릿이 아니지만(클라이언트에
노출되는 값) 문서에는 싣지 않는다 — 전달 경로는 팀 채팅 참고.

**남은 건 실기기 전달 확인뿐이다** — 프론트가 FCM SDK로 진짜 토큰을 발급받아 등록해봐야 가능하고,
구조적으로 지금까지는 불가능했던 부분이다. FE 연동 시작하면 같이 확인한다.

## 관련 문서

[11-notification.md](./11-notification.md), [12-frontend-notification-integration.md](./12-frontend-notification-integration.md)
