# API 문서 (팀 채팅 / 협업 기능)

Phase 0~9([README.md](./README.md) 참고)로 구현한 팀 매칭 이후 협업 기능(채팅, 팀장 선출,
공모전 선정, 진행/제출, 리뷰, AI 연동)의 API를 한눈에 훑어보기 위한 요약 문서입니다.

> ⚠️ **첫 번째 버전입니다.** 기능을 빠르게 붙여나가는 데 집중했고, 필드명·에러코드 번호·
> 응답 형태·도메인 경계 일부는 앞으로 리팩토링하면서 바뀔 수 있습니다. 이 문서는 "지금 이런
> 엔드포인트가 있다"를 훑어보는 용도이고, **요청/응답 스키마와 전체 에러코드 목록의 정확한
> 스펙은 항상 Swagger UI(`/swagger-ui/index.html`, `SwaggerConfig`가 로컬을 1순위로 잡아둠)를
> 기준으로 확인하세요.** 컨트롤러의 `@CustomErrorCodes`에 각 API가 던질 수 있는 에러코드가
> 전부 명시돼 있습니다.

## 공통 사항

- 모든 응답은 `BaseResponse<T>` 래퍼(`status`, `code`, `message`, `data`)로 내려갑니다.
- REST API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다(공개 엔드포인트 없음).
- WebSocket(STOMP)은 HTTP 헤더가 아니라 **CONNECT 프레임의 `Authorization` 헤더**로 인증합니다
  (`StompAuthChannelInterceptor`). `/ws/**`는 HTTP 레이어에서는 인증 없이 열려 있습니다.
- `Team`이 곧 채팅방입니다(별도 ChatRoom 엔티티 없음) — 아래 API 대부분이 `teamId`를 경로에 씁니다.
- 팀/팀원 소속 여부·상태 관련 에러는 여러 도메인이 `TeamErrorCode`를 공유해서 씁니다(도메인별로
  같은 종류의 에러코드를 중복 정의하지 않음).

## 채팅

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/teams` | 채팅방(팀) 목록 조회 |
| GET | `/api/teams/{teamId}/members` | 대화상대(팀원) 목록 조회 |
| DELETE | `/api/teams/{teamId}/members/me` | 채팅방 나가기 (협업거리 -10m) |
| PATCH | `/api/teams/{teamId}/chatbot` | 챗봇 추가/삭제 — body `{ "enabled": boolean }` |
| GET | `/api/teams/{teamId}/messages` | 메시지 목록 조회 (최신순, 최근 50개) |
| PATCH | `/api/teams/{teamId}/read` | 채팅방 읽음 처리 |
| POST | `/api/reports` | 사용자 신고 — body `{ reportedMemberId, teamId?, reasonCode, customReasonText? }` |

### WebSocket (STOMP)

| 방향 | Destination | 설명 |
|---|---|---|
| 클라이언트→서버 | `/app/teams/{teamId}/messages` | 메시지 전송 — body `{ "content": string }` |
| 서버→클라이언트 | `/topic/teams/{teamId}` | 팀 채팅방 실시간 브로드캐스트 구독 (멤버/챗봇/시스템 메시지 전부 이 토픽) |
| 서버→클라이언트 | `/user/queue/errors` | 본인에게만 오는 처리 실패 에러 메시지 |

메시지가 처음 웹소켓을 구독하기 **전에** 이미 서버에서 발행된 경우(예: 팀 생성 직후 챗봇 인사)는
실시간 브로드캐스트로는 못 받으므로, 연결 시 `GET /api/teams/{teamId}/messages`로 과거 내역을
먼저 채워야 합니다 (`chat-test.html` 참고).

메시지의 `messageType`으로 카드형 메시지 종류를, `metadata`(JSON 문자열)로 카드 렌더링에 필요한
id 목록을 내려줍니다 — 실제 이름/아바타 등은 프론트가 팀원 목록/캐릭터 API로 조회해서 채웁니다.
전체 `messageType` 목록은 [08-ai.md](./decisions/08-ai.md)의 표 참고.

## 팀장 선출

| Method | Path | 설명 |
|---|---|---|
| PATCH | `/api/teams/{teamId}/leader-candidacy` | 팀장 여부 투표 — body `{ "wants": boolean }` |
| POST | `/api/teams/{teamId}/leader-votes` | 팀장 투표 — body `{ "candidateTeamMemberId": number }` |
| POST | `/api/teams/{teamId}/leader-votes/ai-recommendation/accept` | 동률 시 AI 추천 수락 (선착순 확정, body 없음) |

## 공모전 후보/투표

| Method | Path | 설명 |
|---|---|---|
| POST | `/api/teams/{teamId}/contest-candidates` | 후보 공모전 추가 — body `{ "contestId": number }` |
| GET | `/api/teams/{teamId}/contest-candidates` | 후보 공모전 리스트 조회 |
| DELETE | `/api/teams/{teamId}/contest-candidates/{contestCandidateId}` | 후보 공모전 삭제 |
| POST | `/api/teams/{teamId}/contest-candidates/votes` | 공모전 투표 (최대 2개) — body `{ "contestCandidateIds": number[] }` |

## 진행 상황 (중간점검 / 제출확인) — 팀장 전용

| Method | Path | 설명 |
|---|---|---|
| PATCH | `/api/teams/{teamId}/progress` | 중간점검 진행률 응답 — body `{ "progressPercent": 0~100 }` |
| PATCH | `/api/teams/{teamId}/submission` | 제출 여부 확인 — body `{ "completed": boolean }` |

## 팀원 리뷰

| Method | Path | 설명 |
|---|---|---|
| POST | `/api/teams/{teamId}/reviews` | 팀원 리뷰 작성 (팀 `SUBMITTED` 상태에서만) — body `{ "revieweeTeamMemberId": number, "content": string }` |

리뷰 조회(읽기) API는 아직 없습니다 — [09-review.md](./decisions/09-review.md)의 "미정" 참고.

## 개발용 임시 엔드포인트 (`@Profile("local")`, 기본값은 비활성)

실제 회원가입/매칭 플로우가 자리잡기 전까지 수동 테스트를 위해 만든 엔드포인트입니다. 나중에
삭제될 예정이므로 프론트에서 정식으로 의존하면 안 됩니다.

> ⚠️ **보안 노트**: 처음엔 `@Profile("!prod")`("prod가 아니면 켜짐")로 막아뒀는데, 이 방식은
> 아무 프로필도 안 잡힌 기본 상태에서 **기본값이 "켜짐"**이라 서버에 `SPRING_PROFILES_ACTIVE=prod`가
> 실제로 설정돼 있는지에 안전이 좌우되는 구조였다. 이 저장소 코드만으로는 배포 서버의 `.env`를
> 확인할 수 없고(`docker-compose.prod.yml`도 `SPRING_PROFILES_ACTIVE`를 명시하지 않음), `cd.yml`이
> `develop` push 시 바로 배포하는 구조라 "나중에 서버 설정 확인" 방식은 위험하다고 판단해
> `@Profile("local")`("local일 때만 켜짐")로 뒤집었다 — 이제 기본값은 항상 "꺼짐"이고, 로컬에서
> 쓰려면 `--spring.profiles.active=local`을 명시적으로 켜야 한다 (README 참고).

| Method | Path | 설명 |
|---|---|---|
| POST | `/api/test/auth/quick-login` | 이메일 인증 없이 회원+프로필 즉시 생성하고 accessToken 발급 |
| POST | `/api/test/teams` | `TeamCreationRequest` 그대로 받아 팀 생성 (매칭 도메인이 나중에 대체) |

## 관련 문서

- [README.md](./README.md) — Phase 진행 현황, 전체 문서 인덱스
- [decisions/](./decisions/) — 카테고리별 설계 결정 및 구현 현황
