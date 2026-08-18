# 03. 채팅 (Message, 챗봇 on/off, 나가기)

## 배경/목적

채팅방 내 메시지 송수신, 챗봇 참여 상태, 채팅방 나가기, 안읽음 처리를 정의한다.

## 엔티티 · 필드 정의

### Message

| 필드 | 설명 |
|---|---|
| team | FK |
| senderType | `MEMBER` / `CHATBOT` / `SYSTEM` |
| senderTeamMember | nullable — CHATBOT/SYTEM이면 null |
| messageType | `TEXT`, `SYSTEM_NOTICE`, `SLEADER_NOMINATION_CARD`, `LEADER_VOTE_CARD`, `CONTEST_RECOMMEND_CARD`, `CONTEST_VOTE_CARD`, `SUBMISSION_CHECK_CARD` |
| content | 텍스트 |
| metadata | 카드가 참조하는 투표/후보 id 등 (JSON 텍스트) |

### 챗봇 on/off

별도 엔티티 없음. `Team.chatbotEnabled` boolean 하나로 처리.

> **결정 이유**: 챗봇은 투표/신고/리뷰의 대상이 되지 않는다. 가상의 TeamMember row를 만들면
> unique 제약, 리스트 조회, 투표 후보 필터링 등 모든 쿼리에 "챗봇 제외" 조건을 추가해야 해서
> 오히려 복잡해진다. "대화상대 조회" API 응답에서 실제 TeamMember 배열 + `chatbotEnabled`
> 플래그를 함께 내려주고, 프론트에서 리스트 맨 아래에 얹는 방식으로 처리한다.
> 추가/삭제 시 `SYSTEM_NOTICE` 메시지("OOO님이 챗봇을 추가/제거했습니다")를 기록한다.

> **끄면 실제로 뭐가 멈추는지 (2026-08-01, Figma 5.1.2 확인 후 결정)**: 처음엔 `respondToMentionIfAny`
> (`@챗봇` 자유 질의)만 이 플래그를 체크하고 나머지 챗봇 메시지(인사 유도, 팀장/공모전 관련 안내,
> 중간점검/제출확인 알림 등)는 전부 무시하고 계속 발행되고 있었다. **`chatbotEnabled=false`면
> 챗봇이 남기는 메시지를 전부 막기로 결정** — `ChatService.postChatbotMessage`/
> `postChatbotCardMessage`에서 중앙으로 가드한다(호출하는 쪽마다 따로 체크할 필요 없음).
> 단, **`Team.status` 상태 전이 자체는 그대로 진행된다** — 챗봇이 꺼져있어도 팀장 선출/공모전
> 확정 같은 로직은 정상 동작하고, 그걸 알리는 메시지만 조용히 생략된다. `SYSTEM_NOTICE`는
> 챗봇 메시지가 아니라 전혀 다른 발신자 타입이라 이 가드와 무관하게 계속 발행된다.

### 나가기

`TeamMember.status = LEFT`, `leftAt` 기록. 나가기 시 confirm 다이얼로그
("협업거리가 10m 줄어들어요") → [06-collaboration-point.md](./06-collaboration-point.md)의
`LEAVE_PENALTY` 이벤트 발생.

### 읽음 처리

`TeamMember.lastReadAt` 갱신 방식 (per-message read-flag 대신 커서 방식으로 안읽음 배지 계산).

## 결정사항

- 프로필 팝업은 기존 `Profile.isPublic` 그대로 사용 — 비공개면 "비공개 프로필입니다" 안내만
  노출, 신규 필드 불필요.
- **위 결정이 실제로는 구현 안 돼 있던 걸 발견 (2026-08-01, Figma 5.1.2.2.1 확인 중)**:
  `GET /api/public/profiles/{profileId}`가 비공개 프로필이면 존재하지 않는 리소스와 동일하게
  `PROFILE_NOT_FOUND`(404)를 던지고 있었다 — "안내만 노출" 계획과 다르게 API 호출 자체가
  실패하는 상태. `ProfileService.getPublicProfile`을 고쳐서 비공개일 땐 예외 대신
  `PublicProfileResponse`에 `isPublic=false` + 닉네임/캐릭터(아바타)만 채우고 나머지(학교/학년/
  전공/프로젝트 등)는 전부 비운 채로 정상 응답하도록 함. 이 API는 팀 채팅 전용이 아니라
  profile 도메인 전반에서 쓰는 범용 API라 이 변경은 팀 채팅뿐 아니라 다른 화면에도 영향을 준다.
- "팀원 이름 수정"(로컬 별칭) 기능은 **이번 스코프에서 제외**. 추후 필요 시 별도 설계 필요
  (viewer × target 조인 엔티티가 필요해 TeamMember 필드 하나로는 해결 안 됨).
- **메시지별 안읽음 인원 수 (2026-08-06, 이슈 #54)**: 카카오톡처럼 메시지 하나하나마다 "이
  메시지를 아직 안 읽은 팀원 수"를 표시한다. 새 엔티티/컬럼 없이 기존 `lastReadAt` 커서
  방식 그대로 활용 — 메시지 M의 안읽음 수 = 활성 팀원 중(보낸 사람 본인은 항상 제외)
  `lastReadAt`(없으면 `joinedAt`)이 M의 `createdAt`보다 이전인 인원 수. 팀당 활성 팀원이
  3~4명뿐이라 팀원 목록을 한 번만 조회해 메모리에서 메시지별로 계산하면 충분하고(이슈에서
  우려한 N+1 없음), 채팅방 목록의 기존 `unreadCount` 계산과 동일한 기준이라 일관적이다.
- **실시간 갱신도 포함**: "읽음 처리는 실시간 브로드캐스트가 필요 없다"는 기존 결정(위 구현
  현황 참고)은 team 단위 unreadCount 얘기였고, 메시지별 안읽음 수는 화면에 이미 떠있는 숫자를
  살려둬야 해서 이번엔 브로드캐스트를 추가했다. `markAsRead` 호출 시 그 시점까지 안읽음이었던
  (=이번에 새로 읽음 처리된) 메시지들만 골라 다시 계산해 `/topic/teams/{teamId}/read-updates`로
  `{messageId, unreadCount}` 목록을 내려준다 — 기존 메시지 브로드캐스트 토픽과 섞지 않고 별도
  destination을 둬서, 프론트가 새 메시지 수신과 안읽음 갱신을 명확히 구분해 처리할 수 있게 했다.
  **(2026-08-06 PR #86 코드래빗 리뷰 반영)** 처음엔 안읽음 이후 메시지를 전부 조회하는 새
  쿼리를 추가했었는데, 팀원이 아주 오래 안 읽으면 그만큼 조회/브로드캐스트 규모가 무제한으로
  커지는 문제가 있었다. 어차피 화면에 기본으로 뜨는 건 최신 50건뿐이므로 그 이상 과거
  메시지의 갱신을 보내는 건 무의미하다고 보고, 새 쿼리 대신 이미 있던
  `findByTeamIdBeforeCursor`(cursor 없이 호출하면 최신 50건, `senderTeamMember.member`까지
  이미 fetch join)를 그대로 재사용해 메모리에서 필터링하는 방식으로 바꿨다 — 조회 규모도
  자연스럽게 50건으로 제한되고, 발신자 N+1 문제도 같이 해결됐다.
  **(2026-08-10 이슈 #115, cursor 페이지네이션 도입 후 재검토)** `getMessages`에 cursor를 붙여
  최신 50건 너머까지 스크롤해 볼 수 있게 되면서, "화면은 최신 50건뿐이라 그 이상은 무의미하다"는
  위 전제가 깨진 게 아닌지 한 번 의심했다. 잠시 캡을 없애고 `unreadSince` 이후 전체를 무제한
  조회하도록 바꿨다가, 그러면 팀원이 몇 주씩 안 읽다가 한 번에 수천 건을 읽었을 때 쿼리/브로드
  캐스트가 다시 무제한으로 커지는 원래 문제(PR #86)가 되살아나서 되돌렸다. 대신 메시지별
  안읽음 수는 **0에 도달하면 다시는 바뀌지 않는 값**이라는 성질에 기대기로 했다 — 카카오톡 등
  대형 채팅 서비스도 이 카운트를 전체 이력에 걸쳐 실시간으로 갱신하지 않고, "활발히 보고 있을
  만한 최근 구간"만 실시간으로 맞추고 그보다 오래된 메시지는 다음에 그 페이지를 다시 불러올 때
  (`getMessages`가 요청마다 그 자리에서 새로 계산해 내려줌) 정확한 값으로 맞춰지는 eventually
  consistent 방식을 쓴다. 그래서 `markAsRead`는 `findByTeamIdBeforeCursor`로 여전히 최신 50건
  범위 안에서만 실시간 갱신을 계산하고, cursor로 더 과거를 스크롤해 본 메시지의 안읽음 수는
  그 페이지를 재조회할 때 정확한 값을 받는 것으로 의도적으로 남겨뒀다.

## 구현 현황 (Phase 2 완료, 2026-07-29 WebSocket으로 전환)

- 엔티티: `domains/chat/entity/Message.java`
- 리포지토리: `domains/chat/repository/MessageRepository.java`
- 서비스: `domains/chat/service/ChatService.java` — `sendMessage`,
  `getMessages`(cursor 없으면 최신 50건, cursor 있으면 그 이전 50건 + `hasNext`,
  2026-08-10 이슈 #115), `markAsRead`, `postSystemMessage`(팀 도메인에서 나가기/챗봇 토글 시
  호출). 메시지가 저장될
  때마다 `SimpMessagingTemplate`으로 `/topic/teams/{teamId}`에 브로드캐스트한다 — `sendMessage`,
  `postSystemMessage` 양쪽 다 이 경로를 타므로, 나중에 챗봇/투표 결과 메시지를 추가해도
  실시간 push를 별도로 구현할 필요 없음.
- **발신자 아바타 (2026-08-01, Figma 5.1.3.1 확인 후 추가)**: `MessageItemResponse`에
  `senderAvatar`(`characterType`+`paletteCode`) 추가 — `getMessages`는 조회된 메시지의
  발신 팀원들을 모아 `CharacterService.findAvatarsByMembers`로 배치 조회하고, 실시간
  브로드캐스트(`broadcast`)는 방금 보낸 발신자 1명만 조회한다. 이걸 하면서
  `MessageRepository.findByTeam_TeamIdOrderByCreatedAtDesc`(2026-08-10 이슈 #115에서
  `findByTeamIdBeforeCursor`로 이름 변경)도 `senderTeamMember`/`profile`/
  `member`를 LEFT JOIN FETCH하도록 고쳤다 — 원래 `sender.getProfile().getNickname()` 자체가
  메시지마다 lazy load되는 기존 N+1이었는데 이번에 같이 잡음. CHATBOT/SYSTEM 메시지나 협업
  유형 검사를 안 한 발신자는 `senderAvatar`가 null.
- **cursor 페이지네이션 (2026-08-10, 이슈 #115)**: `GET /messages`가 항상 최신 50건만 반환하고
  더 과거 메시지를 불러올 방법이 없어, 대화가 쌓인 방에서는 팀 생성 시 발행되는 인사 메시지 등
  가장 오래된 메시지가 API로 영구히 조회 불가능했다. `messageId` 기반 `cursor` 쿼리 파라미터와
  응답 `hasNext` 플래그를 추가해 위로 스크롤 시 이전 메시지를 이어서 조회할 수 있게 했다.
  cursor 필터(`messageId < cursor`)와 정렬을 처음엔 `createdAt DESC, messageId DESC`로 뒀는데,
  동시 저장 시 `messageId`(삽입 순서)와 `createdAt`(애플리케이션이 찍는 타임스탬프) 순서가
  어긋나면 페이지 경계에서 메시지가 중복/누락될 수 있다는 코드래빗 리뷰(PR #116)를 받아 정렬을
  `messageId DESC` 단일 기준으로 통일했다.
- **전송(WebSocket)**: `domains/chat/websocket/ChatWebSocketController` —
  STOMP `@MessageMapping("/teams/{teamId}/messages")`. 인증은 `global/websocket/
  StompAuthChannelInterceptor`가 STOMP `CONNECT` 프레임의 `Authorization` 헤더로 처리
  (HTTP 핸드셰이크 단계가 아님 — SecurityConfig에서 `/ws/**`는 permitAll).
  `WebSocketConfig`가 `/topic`(브로드캐스트) · `/app`(클라이언트→서버) prefix와 엔드포인트
  `/ws`를 등록한다.
- **이력 조회/읽음 처리(REST 유지)**: `GET /api/teams/{teamId}/messages`,
  `PATCH /api/teams/{teamId}/read` — 실시간성이 필요 없어 REST로 남김
- 나가기: `DELETE /api/teams/{teamId}/members/me` (`TeamService.leaveTeam`, `TeamMember.leave()`)
- 챗봇 on/off: `PATCH /api/teams/{teamId}/chatbot` (`TeamService.toggleChatbot`)
- 채팅방 목록(`GET /api/teams`)에 최근 메시지 미리보기(`lastMessageContent`/`lastMessageAt`)와
  안읽음 수(`unreadCount`)를 포함하도록 Phase 1 응답을 확장함
- **정렬 옵션 추가 (2026-08-03, Figma 5.2 채팅방 설정 확인)**: `GET /api/teams?sort=LATEST|UNREAD`
  — 최신 메시지 순(기본값)/안읽은 메시지 순. 자세한 내용은 [01-team.md](./01-team.md) 참고.
- 팀 소속 검증 예외는 별도 `ChatErrorCode` 없이 `TeamErrorCode`를 그대로 재사용 (Team=채팅방
  아키텍처 결정에 따름). STOMP 쪽 예외는 `ChatWebSocketController`의 `@MessageExceptionHandler`가
  `/user/queue/errors`로 클라이언트에 내려준다.
- **수동 테스트 페이지**: `resources/static/chat-test.html` — JWT 토큰 + teamId 입력 후 STOMP
  연결/전송/수신을 브라우저에서 직접 확인 가능 (`@stomp/stompjs` CDN 사용). 토큰은
  `/api/auth/login` 응답을, teamId는 실제 매칭으로 생성된 팀을 그대로 써야 한다.
  **(2026-08-06 삭제)** 원래 있던 "빠른 준비" 버튼(`POST /api/test/teams` 호출)은
  `TeamTestController`와 함께 제거했다 — 매칭 도메인 연동이 끝나 컨트롤러 자체가 불필요해졌고,
  버튼이 보내던 요청도 이미 `TeamMemberInput`에 추가된 필드(leaderPreference 등)를 빠뜨려
  어차피 실패하던 상태였다. `AuthTestController`(`domains/auth/controller`)의 quick-login은
  그대로 남아있다 — `@Profile("local")` 임시 엔드포인트(명시적으로 켜야만 활성화되는 fail-safe
  방식 — 이유는 [api.md](../api.md) 참고), 매칭/실가입 플로우 연동 후 삭제 예정.
  **(2026-08-06 추가)** 메시지별 안읽음 수 뱃지 표시 + "읽음 처리" 버튼 + `/read-updates`
  구독을 붙여, 다른 탭(팀원 시점)에서 읽음 처리했을 때 이 탭의 숫자가 실시간으로 줄어드는
  것까지 눈으로 확인할 수 있다.
- 테스트: `ChatServiceTest`(브로드캐스트 검증 포함, 안읽음 수 계산/실시간 갱신 케이스 추가),
  `TeamServiceTest`(나가기/챗봇 토글 케이스)

## 미정 / 추후 확인 필요

- 채팅방 "설정"(⚙️) 화면 상세 스펙 미정.
- STOMP 엔드포인트 `allowedOriginPatterns("*")`는 개발 편의를 위한 설정 — 배포 전 실제 허용
  origin으로 좁혀야 함.
- 협업거리 차감(-10m, 나가기 페널티)은 이미 Phase 3에서 `leaveTeam`에 연결 완료
  ([06-collaboration-point.md](./06-collaboration-point.md) 참고).

## 관련 화면

5.1 채팅목록, 5.1.1.1 메시지 입력, 5.1.2 메뉴_챗봇 삭제/추가, 5.1.2.4 채팅방 나가기
