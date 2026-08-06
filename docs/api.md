# API 문서 (팀 매칭 / 팀 채팅 / 협업 기능)

Phase 0~9([README.md](./README.md) 참고)로 구현한 팀 매칭과 매칭 이후 협업 기능(채팅, 팀장 선출,
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

## 매칭

알고리즘 자체의 상세 설계는 [10-matching-algorithm-detail.md](./decisions/10-matching-algorithm-detail.md)
참고. 아래는 신청부터 팀 확정까지 프론트가 호출하는 API 흐름입니다.

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/matching/applications/eligibility` | 매칭 신청 자격 및 오늘 참여 현황 조회 (신청 화면 진입 전 확인용) |
| GET | `/api/matching/applications/me/today` | 오늘의 내 매칭 신청 상태 + 현재 철회 가능 방식 조회 |
| POST | `/api/matching/applications` | 매칭풀 신청 — body `{ profileId, contestCategory, leaderPreference, noticeConfirmed: true }`, 하루 1회·14시 전만 가능 |
| POST | `/api/matching/applications/{applicationId}/withdraw` | 매칭 신청 통합 철회 — 신청 취소와 매칭 결과 패스를 구분하지 않는 단일 엔드포인트 |
| GET | `/api/matching/results/me/today` | 오늘의 내 매칭 결과 조회 — 그룹/팀원/궁합 점수는 `publishedAt` 이후에만 채워짐 |
| GET | `/api/matching/groups/{matchingGroupId}/responses` | 매칭 그룹의 팀원별 수락/대기 응답 현황 조회 |
| POST | `/api/matching/groups/{matchingGroupId}/accept` | 매칭 결과 수락 |

### 신청 상태 흐름

`MatchingApplicationStatus`: `WAITING`(매칭풀 대기) → `MATCHING`(계산 중) → `PROPOSED`(결과 제안, 그룹원
응답 대기) → `MATCHED`(팀 확정). 중간에 `CANCELED`(14시 전 무료 취소), `PASSED`(14시 이후 또는 결과
제안 후 패스), `REASSIGN_PENDING`(그룹원 부족으로 다음 배치 재배정 대기), `FAILED`(팀 구성 실패)로
빠질 수 있음. 화면 분기는 이 상태값이 아니라 `/results/me/today`가 내려주는 `resultStatus`
(`NOT_APPLIED` / `NOT_PUBLISHED` / `PROCESSING` / `MATCHED` / `UNMATCHED` / `WITHDRAWN`)를 기준으로
하는 걸 권장 — 공개 시각 전후, 결과 없음 등 화면에 필요한 분기가 이미 계산돼서 내려옴.

### "나의 매칭현황" 화면 (팀원 응답 대기 / 수락 완료) 연동 참고

- **대기 화면** ("N명 중 M명 응답 완료"): `GET /api/matching/groups/{matchingGroupId}/responses` 응답의
  `proposedTeamSize`가 N, `members` 중 `responseStatus != PENDING`인 개수가 M. `activeMemberCount`는
  PASS한 인원을 제외한 후보 수라 진행률 계산에는 쓰지 말 것 — 진행률(M/N)은 항상 `members` 배열을
  직접 세서 구해야 함.
- **매칭신청취소**: `POST /api/matching/applications/{applicationId}/withdraw` 하나만 호출하면 됨.
  이미 그룹(`PROPOSED`)에 들어간 뒤라 서버가 자동으로 `PENALIZED_PASS`로 처리하며 협업거리가
  깎임 — 프론트가 신청 단계인지 그룹 배정 후인지 구분해서 다른 API를 호출할 필요 없음. 응답의
  `withdrawalType`으로 실제 어떤 방식으로 처리됐는지 확인 가능.
- **수락 완료 화면**: 마지막 팀원이 `accept`를 호출하는 순간 서버가 바로 `Team`을 생성한다.
  `AcceptResponse.groupStatus`가 `CONFIRMED`이면 같은 응답의 `teamId`로 "채팅방 바로가기"를 바로
  연결할 수 있음(팀 = 채팅방, 위 채팅 섹션 참고). 아직 다른 팀원이 응답 중이면 `groupStatus`는
  `PROPOSED`로 남고 `teamId`는 `null` — 이 경우 대기 화면을 유지해야 함.
- **동일 요청 재시도**: 이미 `ACCEPTED`한 상태에서 `accept`를 다시 호출해도 에러 없이 같은
  `AcceptResponse`가 그대로 돌아옴(네트워크 재시도 대비 멱등 처리) — 프론트에서 중복 클릭 방어를
  위해 별도로 요청 상태를 캐싱할 필요는 없음.

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

팀장 선출은 별도 화면 진입 없이 **채팅방 안의 카드 메시지**로 전부 진행됩니다. 판정 로직과
전체 시나리오는 [02-leader-election.md](./decisions/02-leader-election.md) 참고. 프론트는 아래
3개 API만 있으면 되고, "지금 무엇을 보여줄지"는 전부 채팅 메시지의 `messageType` + `metadata`로
내려옵니다(REST로 별도 상태를 폴링하지 않음).

| Method | Path | 설명 |
|---|---|---|
| PATCH | `/api/teams/{teamId}/leader-candidacy` | 팀장 여부 투표 — body `{ "wants": boolean }` |
| POST | `/api/teams/{teamId}/leader-votes` | 팀장 투표 — body `{ "candidateTeamMemberId": number }` |
| POST | `/api/teams/{teamId}/leader-votes/ai-recommendation/accept` | 동률 시 AI 추천 수락 (선착순 확정, body 없음) |
| POST | `/api/teams/{teamId}/leader-votes/revote` | 동률 시 재투표 시작 안내 (body 없음, 아래 참고) |

### 매칭 시점 팀장 희망에 따른 3가지 분기 (`LeaderSelectionMode`)

팀 생성 시 매칭 신청 때 고른 팀장 희망(`WANTS`) 응답 개수로 자동 분기되며, 프론트가 별도로
분기할 필요 없이 채팅에 오는 카드 종류로만 화면을 그리면 됩니다.

| WANTS 개수 | 모드 | 채팅에 오는 것 |
|---|---|---|
| 1명 (VER.2, 팀장투표X) | `AUTO_ASSIGNED` | 인사 유도 텍스트 메시지 자체에 팀장 이름이 포함됨(카드 아님). 팀원 전원이 인사를 마치면 그 뒤에 `LEADER_RESULT_CARD`가 발행되고 바로 공모전 추천(`CONTEST_RECOMMEND_CARD`)으로 넘어감 — 투표 API 호출 자체가 필요 없음 |
| 2명 이상 (VER.1, "매칭 등록 단계에서 이미 2명 이상") | `CANDIDATE_VOTE` | "팀장 여부 투표" 단계 없이 바로 사전 희망자 전원을 후보로 담은 `LEADER_VOTE_CARD` 발행 |
| 0명 (VER.1, "아무도 사전 팀장 희망 하지 않은 경우") | `OPEN_NOMINATION` | `LEADER_NOMINATION_CARD`(AI가 2명 추천)로 "팀장 할래요/안할래요" 투표부터 시작 |

### 카드 종류와 프론트 처리

- **`LEADER_NOMINATION_CARD`** (팀장 여부 투표, OPEN_NOMINATION 전용): `metadata.aiRecommendedTeamMemberIds`(배열)에 AI가 추천한 후보 2명의 `teamMemberId`가 담김. **본인의 `teamMemberId`가 이 배열에 포함되면** "AI가 당신을 팀장 후보로 추천했어요!" 같은 개인화 문구를 프론트에서 붙여서 보여주면 됨(서버는 추천 대상 id만 내려주고 문구 자체는 이미 카드 텍스트에도 포함돼 있음). 사용자 응답은 `PATCH /leader-candidacy`.
- **`LEADER_VOTE_CARD`**: 세 가지 경우에 재사용되는 같은 타입이라 `metadata` 필드 유무로 구분해야 함.
  - 최초 투표 카드(후보 확정 직후): `metadata.candidateTeamMemberIds`만 있음 → "팀장 투표" 바텀시트, 후보 중 라디오 선택 후 `POST /leader-votes`.
  - 동률 카드: `candidateTeamMemberIds` + `aiRecommendedTeamMemberId`가 함께 있음 → "재투표하기"/"추천 수락하기" 두 버튼 노출.
    - "추천 수락하기" → `POST /leader-votes/ai-recommendation/accept` (body 없음).
    - "재투표하기" → `POST /leader-votes/revote` 호출 → 성공하면 서버가 같은 동률 후보 목록으로 새 `LEADER_VOTE_CARD`(안내 텍스트만 다르고 `aiRecommendedTeamMemberId`는 빠짐)를 채팅방에 발행함 → 이 새 카드가 온 뒤 각자 `POST /leader-votes`로 재투표. **이 엔드포인트는 상태를 바꾸지 않고 안내 메시지만 다시 보내는 용도**라, 여러 명이 동시에 눌러도 안전하지만 안내 카드가 중복으로 여러 번 뜰 수 있음 — 프론트에서 버튼을 누른 사람 화면은 즉시 비활성화하는 정도로 충분함.
- **`LEADER_RESULT_CARD`** (팀장 확정, 모든 경로 공통 도착점): `metadata.leaderTeamMemberId`로 최종 팀장을 알려줌. 이 카드가 오면 팀장 선출 플로우는 끝난 것이고, 곧이어 `CONTEST_RECOMMEND_CARD`(공모전 추천, 다음 파트)가 뒤따라옴.
- 후보가 끝까지 0명이거나(OPEN_NOMINATION) 마감까지 아무도 투표하지 않으면 서버가 무작위로 임시 팀장을 지정하고 동일하게 `LEADER_RESULT_CARD`로 안내함 — 프론트가 별도로 "후보 없음" 화면을 만들 필요 없이 같은 카드로 처리됨.

### 마감 타이머

`GET /api/teams/{teamId}/members` 응답(`TeamMembersResponse`)의 `leaderSelectionDeadlineAt`으로
"투표 마감까지 00:00:00" 카운트다운을 그리면 됨 — 서버가 실시간으로 갱신해서 push하는 값이
아니라 고정 시각이므로, 받은 값과 현재 시각의 차이를 프론트에서 매초 로컬 계산하면 됨. 마감
이후 처리(자동 확정 등)는 스케줄러가 처리하고 결과는 평소처럼 채팅 메시지로 온다.

> ⚠️ **알려진 이슈 (확인 필요, 아직 미수정)**: `AUTO_ASSIGNED`(VER.2) 케이스에서 Figma는 인사
> 유도 메시지와 `LEADER_RESULT_CARD`가 동시에(=아무도 인사하기 전에) 함께 나타나는데, 현재
> 서버는 인사 유도 텍스트만 먼저 보내고 `LEADER_RESULT_CARD`는 **팀원 전원이 인사를 마친
> 뒤에야** 별도 문구와 함께 발행한다. 의도된 사양 변경인지 프론트/기획과 확인 필요 —
> 확인되면 이 문서와 `02-leader-election.md`를 함께 갱신할 것.

## 공모전 후보/투표

팀장 선출과 마찬가지로 대부분 채팅 카드로 진행되고, 후보 추가/삭제/투표만 REST로 호출한다.
설계 근거는 [04-contest-voting.md](./decisions/04-contest-voting.md) 참고.

| Method | Path | 설명 |
|---|---|---|
| POST | `/api/teams/{teamId}/contest-candidates` | 후보 공모전 추가 — body `{ "contestId": number }` |
| GET | `/api/teams/{teamId}/contest-candidates` | 후보 공모전 리스트 조회 |
| DELETE | `/api/teams/{teamId}/contest-candidates/{contestCandidateId}` | 후보 공모전 삭제 |
| POST | `/api/teams/{teamId}/contest-candidates/votes` | 공모전 투표 (최대 2개) — body `{ "contestCandidateIds": number[] }` |
| GET | `/api/teams/{teamId}/contest-candidates/votes` | 현재 라운드 투표 진행 상황(참여 인원, 후보별 득표수) 조회 — 신규 |
| POST | `/api/teams/{teamId}/contest-shares` | 공모전을 채팅방에 공유 — body `{ "contestId": number }`, 후보 등록과 별개 액션 |

### 카드 종류

- **`CONTEST_RECOMMEND_CARD`** (팀장 확정 직후 자동 발행): `metadata.contestIds`에 담긴 공모전은 이미 `ContestCandidate`로 등록까지 끝난 상태 — "전체보기"는 새로 조회하는 화면이 아니라 `GET .../contest-candidates` 전체 목록을 보여주면 됨.
- **`CONTEST_SHARE_CARD`** (공모전 탭에서 공유): `metadata.contestId`만 있고, 이 시점엔 후보로 등록되지 않음. 카드의 "+" 버튼을 눌러 확인 모달("해당 공모전을 후보로 추가하겠습니까?")에서 "예"를 누르면 그제서야 `POST /contest-candidates`를 호출해 후보로 등록.
- **`CONTEST_VOTE_CARD`**: 동률 시 재투표 안내(`metadata.contestCandidateIds` = 동률이었던 후보). 리더 선출과 달리 AI 추천 수락 경로는 없고 재투표만 있음 — 안내를 본 뒤 각자 `POST .../votes`를 다시 호출하면 됨(라운드는 서버가 저장된 표로 자동 계산).
- **`CONTEST_VOTE_REMINDER_CARD`** (마감 10분 전, 신규): 메타데이터 없이 고정 문구만 옴. 버튼 라벨("투표하기"/"결과 보기")은 서버가 정해주지 않으므로, 프론트가 `GET .../contest-candidates/votes`의 `myVoted`로 직접 판단해야 함 — `true`면 "결과 보기", `false`면 "투표하기".
- **`CONTEST_RESULT_CARD`** (확정 도착점): `metadata.contestId`로 최종 공모전을 알려줌. 이 카드 다음 곧바로 `IN_PROGRESS` 전환 메시지 + `CHATBOT_GUIDE_CARD`("@챗봇" 사용 예시)가 온다.

### "공모전 투표"/"투표 결과" 화면 연동 참고

- **"N명 참여중.."** / **"투표 결과"의 후보별 득표 막대그래프**: `GET .../contest-candidates/votes`로 조회. 응답의 `requiredVoterCount`가 분모, `participatedVoterCount`가 "N명 참여", `results[].voteCount`가 후보별 득표 막대 — **전원이 투표를 마치기 전에도** 호출 가능(개표를 확정하지 않는 순수 조회용). `round`가 바뀌면(동률 재투표 진입) 이 API도 자동으로 새 라운드 기준 값을 반환함.
- **투표 여부에 따른 버튼 분기**: `myVoted`가 `false`면 "투표하기"(바텀시트 열어 `POST .../votes`), `true`면 "결과 보기"(바로 위 조회 결과를 보여주기만 함).
- **동률/최종 확정 여부**: 이 조회 API는 그 자체로 알려주지 않음 — 개표 완료(승자 확정 또는 재투표 카드 발행)는 항상 채팅 메시지(`CONTEST_VOTE_CARD`/`CONTEST_RESULT_CARD`)로 옴. 폴링 대신 웹소켓 구독으로 감지하는 걸 권장.
- **투표 참여자 0명**: 마감 시각에 서버가 후보 중 하나를 무작위로 대신 확정하고 `CONTEST_RESULT_CARD`로 안내함 — 별도 "참여자 없음" 화면 없이 같은 카드로 처리하면 됨.

## 진행 상황 (중간점검 / 제출확인) — 팀장 전용

`IN_PROGRESS` 상태에서 팀 생성일~공모전 마감일 중간 시점에 "중간점검", 마감 하루 전에
"제출확인"이 각각 챗봇 카드로 한 번 자동 발행된다(스케줄러, [07-scheduler.md](./decisions/07-scheduler.md)).
두 응답 모두 팀장만 가능 — 팀원 화면에는 버튼 없이 안내 카드만 노출하면 됨.

| Method | Path | 설명 |
|---|---|---|
| PATCH | `/api/teams/{teamId}/progress` | 중간점검 진행률 응답 — body `{ "progressPercent": 0~100 }` |
| PATCH | `/api/teams/{teamId}/submission` | 제출 여부 확인 — body `{ "completed": boolean }` |

### "@챗봇에게 말하기" 버튼

`CHATBOT_GUIDE_CARD`의 "@챗봇에게 말하기" 버튼은 서버 API 호출이 아니라 **프론트가 메시지
입력창에 `@챗봇 `을 미리 채워주기만 하면 됨** — 사용자가 이어서 질문을 입력해 전송하면
그 메시지가 일반 메시지 전송(`POST /app/teams/{teamId}/messages`)으로 나가고, 서버가
`@챗봇`으로 시작하는 메시지를 감지해 AI 답변을 자동으로 붙인다.

### 협업거리 보상 금액 (고정값)

아래 금액은 `CollaborationPointReason`에 고정된 상수라 API 응답에 매번 실려오지 않는다.
"완주를 축하드려요" 같은 개인화 팝업은 서버가 만들어주지 않으므로, 프론트가 아래 값과 뷰어의
역할(팀장/팀원)을 기준으로 직접 구성해야 한다 — 실제 누적 협업거리는
`GET /api/members/me/collaboration-distance`, 변경 이력은
`GET /api/members/me/collaboration-distance/histories`로 확인 가능.

| 시점 | 대상 | 금액 |
|---|---|---|
| 중간점검 최초 응답 | 팀장 | +5m |
| 제출확인 "진행 완료" | 팀장 | +30m |
| 제출확인 "진행 완료" | 팀원(팀장 제외) | +20m |
| 팀원 리뷰 작성 | 리뷰 작성자 | +10m |

### 제출확인 "미완료"/무응답 시 재알림 — 신규

기존에는 제출확인 카드를 팀장이 놓치거나 "미완료"를 선택하면 다시 알려줄 방법이 없어 팀이
`IN_PROGRESS`에 무한정 머무를 수 있었다. 이제 최초 카드 발송, "미완료" 응답, 재알림 발송
시점마다 서버가 내부적으로 다음 재알림 시각을 2시간 뒤로 계속 미루며, "진행 완료"가 나올 때까지
2시간 간격으로 같은 `SUBMISSION_CHECK_CARD`(문구만 "프로젝트가 진행완료되었으면, 진행완료
버튼을 눌러주세요."로 더 짧음)를 반복 발행한다. 프론트는 카드 타입이 같으므로 최초 카드와
동일하게 "미완료"/"진행 완료" 버튼만 그리면 되고, 별도로 재시도 로직을 구현할 필요는 없다.

## 팀원 리뷰

팀 상태가 `SUBMITTED`인 동안(제출확인에서 "진행 완료" 선택 직후) 활성 팀원끼리 자기 자신을
제외한 서로를 각 1회씩 리뷰한다. 설계 근거는 [09-review.md](./decisions/09-review.md) 참고.

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/teams/{teamId}/reviews/targets` | 리뷰 대상 팀원 목록 조회 — 이미 작성한 대상은 `alreadyReviewed=true` |
| POST | `/api/teams/{teamId}/reviews` | 팀원 리뷰 작성 — body `{ revieweeTeamMemberId, communicationScore, participationScore, keywords }` |

- `communicationScore`/`participationScore`: `"DISAGREE"`(아니다) / `"NEUTRAL"`(보통이다) /
  `"AGREE"`(그렇다) 중 하나. 각각 "소통이 원활하게 이루어졌나요?" / "프로젝트에 적극적으로
  참여하였나요?" 질문에 대응.
- `keywords`: `["LEADERSHIP", "GOOD_COMMUNICATOR", "CREATIVE", "PROBLEM_SOLVER", "TRUSTWORTHY", "PROACTIVE", "CONSIDERATE"]`
  중 최소 1개 이상 다중 선택(중복 선택 가능, 상한 없음). 각각 "리더십이 있는 팀원" / "소통이
  잘되는 팀원" / "아이디어가 좋은 팀원" / "문제해결을 잘하는 팀원" / "믿음직한 팀원" /
  "적극적인 팀원" / "배려심 있는 팀원"에 대응 — 화면의 7개 키워드 칩과 1:1로 매핑됨.

> ⚠️ **API가 최근에 바뀜**: 이전에는 `content`(자유 텍스트) 하나만 받았지만, 실제 와이어프레임에
> 자유 텍스트 입력창이 없어 위 3개 필드로 교체됐다(2026-08-06). 이 문서를 캐싱해서 예전 스키마로
> 연동 중이었다면 갱신 필요.

### "팀원 리뷰" 화면 연동 참고

- **탭 목록/회색 비활성화**: `GET .../reviews/targets`의 각 항목이 `alreadyReviewed`를 갖고
  있음 — `true`인 팀원은 탭을 회색으로 비활성화. 전원 `alreadyReviewed=false`면 "아직 작성한
  팀원 리뷰가 없어요" 빈 상태를 보여주면 됨.
- **완료 팝업 문구 분기("팀원 리뷰 팝업" vs "팀장 리뷰 팝업")**: 서버는 리뷰이(reviewee)가
  팀장인지 여부로 문구를 나눠주지 않는다 — 대상 목록 응답의 `role`(`"LEADER"`/`"MEMBER"`)로
  프론트가 직접 판단해서 팝업 문구만 다르게 그리면 됨.
- **"작성하신 내용은 임시저장됩니다" (나가기 확인 모달)**: 서버에 초안 저장 API가 없다 — 이
  안내는 프론트가 화면을 벗어나기 전까지 로컬 상태로만 입력값을 들고 있다가, 같은 세션에서
  해당 탭으로 돌아오면 로컬 상태를 복원해주는 것으로 해석했다. 기기/세션을 넘어선 영속화가
  실제로 필요하면 확인 후 별도 API를 추가해야 함.
- **전원 리뷰 완료 시 협업거리 +10m**: 본인이 나머지 팀원 전체에 대한 리뷰를 다 쓴 시점에
  서버가 알아서 1회만 지급한다(중복 지급 없음) — 프론트가 지급 여부를 계산할 필요 없이, 마지막
  리뷰 제출 응답을 받으면 곧바로 "협업거리가 10m 더 늘었어요!" 화면으로 넘어가면 됨. 실제 지급
  여부/금액 확인은 위 "협업거리 보상 금액" 섹션의 조회 API 참고.

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
