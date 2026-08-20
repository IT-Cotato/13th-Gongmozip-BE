# 12. 프론트엔드 연동 가이드 — 알림

백엔드([13th-Gongmozip-BE](https://github.com/IT-Cotato))에서 알림 도메인을 만들면서, 프론트
([13th-Gongmozip-FE](https://github.com/IT-Cotato/13th-Gongmozip-FE))가 실제로 연결할 때 필요한 내용을
정리한다. 설계 배경/결정 이유는 [11-notification.md](./11-notification.md) 참고 — 이 문서는 "지금 뭘 붙일 수
있고, 뭘 아직 못 붙이는지"에 집중한다.

## 지금 붙일 수 있는 것 (Phase 1 — 알림함/배지)

프론트에 이미 알림 화면(`src/app/alarm/page.tsx`)과 홈 화면 종 아이콘 배지(`src/app/page.tsx`의
`Header()`)가 스켈레톤으로 존재하는 걸 확인했다 — 탭(`전체/기타/매칭/채팅방`)까지는 만들어져 있고
목록/배지 모두 실데이터 연동만 없는 상태였다. 아래 API로 그 자리를 채우면 된다.

### `GET /api/notifications`

| 쿼리 파라미터 | 필수 | 설명 |
|---|---|---|
| `category` | X | 생략하면 "전체" 탭. `OTHER`(기타) / `MATCHING`(매칭) / `CHATROOM`(채팅방) 중 하나 |
| `cursor` | X | 생략하면 최신 페이지(20건). 직전 응답의 `notifications` 배열 중 가장 마지막(가장 오래된) 항목의 `notificationId`를 넘기면 그 이전 20건을 이어서 받는다 |

```json
{
  "status": 200,
  "code": "NOTIFICATION_200_1",
  "message": "알림 목록 조회에 성공하였습니다.",
  "data": {
    "notifications": [
      {
        "notificationId": 42,
        "category": "CHATROOM",
        "body": "팀장 투표 결과가 공개되었어요! 지금 바로 확인해 보세요.",
        "relatedTeamId": 7,
        "isRead": false,
        "createdAt": "2026-08-20T20:15:00"
      },
      {
        "notificationId": 41,
        "category": "MATCHING",
        "body": "매칭 신청이 완료되었습니다.",
        "relatedTeamId": null,
        "isRead": true,
        "createdAt": "2026-08-20T13:58:12"
      }
    ],
    "hasNext": false
  }
}
```

- `isRead`가 스크린샷의 "회색 표시(안읽음)" 여부를 그대로 뒤집은 값이다 — `isRead: false`인 항목만 회색으로
  표시하면 된다.
- `relatedTeamId`가 있는 항목(CHATROOM)은 탭하면 `/chat/{relatedTeamId}`로 이동시키면 된다. `MATCHING`/
  `OTHER`는 `relatedTeamId`가 항상 `null`이다 — 매칭 알림은 오늘의 매칭 결과 화면 등 고정 경로로 보내면 된다.
- **알림 문구(`body`)는 백엔드가 소유한다** — 프론트에서 카테고리별로 문구를 다시 조립할 필요 없이 그대로
  렌더링하면 된다. 문구를 바꾸고 싶으면 백엔드 코드(`ChatbotOrchestrationService`가 남기는 챗봇 메시지
  원문, `MatchingApplicationService`의 `APPLY_COMPLETE_NOTIFICATION_BODY`/
  `RESULT_PUBLISHED_NOTIFICATION_BODY` 상수)를 고쳐야 한다.
- `category`에 위 세 값 이외의 문자열을 넘기면 `NOTIFICATION_400_1`(`올바르지 않은 알림 카테고리입니다.`)
  로 400을 응답한다(코드리뷰 findings 반영, 2026-08-20 — 최초 구현 땐 500이었음).

### `GET /api/notifications/unread-exists`

홈 화면 종 아이콘의 빨간 배지 표시 여부. 현재 `Header()`(`src/app/page.tsx:17-53`)가 조건 없이 항상
빨간 점을 그리고 있는데, 이 API 결과로 조건부 렌더링하면 된다.

```json
{ "status": 200, "code": "NOTIFICATION_200_2", "message": "...", "data": { "unreadExists": true } }
```

### `PATCH /api/notifications/read-all`

알림함 화면(`/alarm`)에 **진입하는 시점**에 한 번 호출한다. 탭(전체/기타/매칭/채팅방)과 무관하게 회원의
모든 알림을 읽음 처리한다 — 요구사항이 "알림창에 한 번 들어가면 회색 표시가 전부 사라져야 한다"였기
때문에, 탭을 전환할 때마다 다시 호출할 필요는 없고 페이지 진입 1회로 충분하다. 응답 후에는:

- 알림함 리스트를 다시 불러오거나 로컬에서 전부 `isRead: true`로 바꿔서 회색 표시를 지운다.
- 홈 화면으로 돌아갔을 때의 빨간 배지도 사라져야 하므로, `unread-exists`를 다시 조회하거나(권장) 전역
  상태(Zustand 등)에 배지 여부를 들고 있다면 `false`로 갱신한다.

### 인증

다른 API와 동일하게 `Authorization: Bearer <accessToken>` 헤더만 있으면 된다 — 알림 전용 인증/권한 로직
없음. 기존 `src/lib/http.ts`의 `apiFetch` 그대로 사용 가능.

## 지금 붙일 수 있는 것 (Phase 3 — OS 푸시)

백엔드 구현 완료, 실제 Firebase 프로젝트/서비스 계정 키로 로컬 검증까지 마쳤다(PR #201). 설계 배경은
[13-fcm-push.md](./13-fcm-push.md) 참고. **FE가 시작하기 전에 Firebase 콘솔에서 준비해둬야 하는 것 2가지**
(코드 아님, 콘솔 설정) — 백엔드 쪽에서 미리 발급해두기로 함:

1. **웹 앱 등록**으로 나오는 `firebaseConfig`(apiKey, projectId, messagingSenderId, appId 등) — FE
   코드에 그대로 넣어야 함
2. **웹 푸시 인증서(VAPID 키)** — `getToken()` 호출 시 필요

### `POST /api/notifications/push-tokens`

FCM SDK의 `getToken()`으로 발급받은 토큰을 로그인 직후(또는 알림 권한을 막 허용한 시점)에 등록한다.
같은 토큰이 이미 등록돼 있으면(예: 같은 브라우저에서 재로그인) 소유자만 자동으로 갈아끼워지므로
매번 그냥 호출하면 된다(upsert).

```json
// request
{ "token": "<FCM에서 발급받은 등록 토큰>" }
```
```json
// response 201
{ "status": 201, "code": "NOTIFICATION_201_1", "message": "푸시 토큰이 등록되었습니다." }
```

### `DELETE /api/notifications/push-tokens`

로그아웃 시 호출(권장, 필수는 아님). body 형식은 등록과 동일(`{ "token": "..." }"`). 본인 소유 토큰만
해제되고, 이미 없는 토큰이어도 에러 없이 끝난다.

### 인앱 배너 / OS 푸시 억제 정책

FCM Web SDK의 `onMessage()`가 foreground에서 로컬로 잡히므로, 이벤트의 데이터 payload에 담긴 `teamId`와
프론트의 현재 `usePathname()`(`/chat/[roomId]`)을 **프론트에서** 비교해 같은 방이면 배너를 스킵하고,
아니면 배너를 띄우는 방식을 권장한다 — 서버 쪽에 별도 "지금 이 방 보고 있는지" 추적 인프라(presence)는
만들지 않았다. background/종료 상태에서는 서비스워커가 자동으로 OS 알림을 띄우므로 별도 처리가 필요 없다.

- 챗봇 알림(CHATROOM)과 다른 팀원의 일반 채팅 메시지 모두 push의 `data`에 `teamId`가 담겨 온다.
- 매칭(MATCHING) 알림은 `data`가 비어있다 — 채팅방 개념이 없어 억제 판단이 필요 없음.
- 다른 팀원의 일반 채팅 메시지는 알림함(`GET /api/notifications`)엔 안 쌓인다 — 푸시로만 온다
  ([11-notification.md](./11-notification.md) 참고). 알림함 리스트를 새로고침해도 이 메시지는 안 보이는
  게 정상이다.
- 푸시 제목(title)은 현재 모든 카테고리에 고정 문구 `"공모집"`이다 — Figma에 명시된 문구가 있으면
  백엔드에 알려달라(`NotificationService.PUSH_TITLE`, `ChatService.PUSH_TITLE`).

### 아직 검증 안 된 부분

로컬에서 인증·API·발송 경로(FCM 서버까지 요청 도달, 인증 통과)는 확인했지만, **실제 기기에 알림이
뜨는지는 FE가 진짜 토큰으로 등록해봐야 확인 가능**하다 — 구조적으로 지금까지는 불가능했던 부분. 처음
연동할 때 뭔가 안 뜨면 알려달라, 같이 디버깅한다.

## 아직 없는 것 (프론트 작업 전까지는 백엔드로도 해결 안 되는 부분)

- **인앱 실시간 배너(Phase 4)** — 위 "인앱 배너 / OS 푸시 억제 정책" 절의 로직 자체가 프론트 구현 몫이다.
  백엔드는 `data.teamId`를 실어 보내는 것까지만 하고, foreground 판단/배너 UI는 전부 프론트에서 만든다.
- **iOS 홈 화면 설치 유도 UI(Phase 5)** — 아래 절 참고, 아직 화면이 없어 보류 중.

## iOS 홈 화면 설치 유도 — 화면 없어서 보류, 기록만 남김

iOS Safari는 **웹앱이 홈 화면에 추가(PWA로 설치)된 상태가 아니면 OS 푸시 자체가 뜨지 않는다** — FCM을
쓰더라도 내부적으로 Apple의 Web Push를 타기 때문에 이 제약은 그대로 적용된다. 프론트에 이미
`manifest.ts`/`PwaServiceWorker.tsx`로 PWA 기반은 갖춰져 있지만(`public/sw.js`는 현재 앱 셸 캐싱만
하고 `push`/`notificationclick` 리스너는 없음), "홈 화면에 추가해주세요" 안내 화면/배너는 아직 없다.

**지금은 만들지 않는다** — 위 FCM 작업(②③)을 시작할 때, iOS 사용자에게 설치를 유도하는 안내 UI를 반드시
같이 넣어야 한다는 점만 여기 기록해둔다. 이걸 빼먹으면 iOS 사용자는 인앱 배너까지는 받고 OS 푸시는
영영 못 받는 상태가 된다.

## 로드맵 요약

| Phase | 내용 | 상태 |
|---|---|---|
| 1 | 알림함 백엔드(엔티티/API/트리거) | ✅ merge됨 (#198) |
| 2 | 프론트 `/alarm`, 홈 배지 실데이터 연동 | 프론트 작업, 이 문서의 "지금 붙일 수 있는 것" 참고 |
| 3 | FCM 토큰 저장/발송 배관 | ✅ 백엔드 완료 + 로컬 실기기 자격증명 검증(#200), 프론트 미착수 |
| 4 | 인앱 실시간 배너(다른 팀원 메시지 포함 전체) | 미착수, 프론트 전용 작업(위 "인앱 배너 / OS 푸시 억제 정책" 참고) |
| 5 | iOS 홈 화면 설치 유도 UI | 미착수, Phase 3~4와 함께 진행 필수 |
