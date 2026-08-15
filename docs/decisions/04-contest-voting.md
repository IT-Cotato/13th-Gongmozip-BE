# 04. 공모전 후보/투표

## 배경/목적

팀장 선출 이후 팀이 나갈 공모전을 후보로 모으고 투표로 확정하는 기능. 기존 `contest`
도메인의 `Contest` 엔티티를 그대로 참조한다.

## 엔티티 · 필드 정의

### ContestCandidate

| 필드 | 설명 |
|---|---|
| team | FK |
| contest | `Contest` FK |
| addedByTeamMember | FK |
| createdAt | |

unique(team_id, contest_id)

### ContestVote

| 필드 | 설명 |
|---|---|
| team | FK |
| contestCandidate | FK |
| voterTeamMember | FK |
| round | int, default 1 |

unique(contest_candidate_id, voter_team_member_id, round) — **후보별 1건** 제약. 한 투표자가
라운드당 여러 후보(현재 2개)에 투표할 수 있어야 하므로 voter 단위가 아닌 candidate 단위로
unique를 건다.

## 결정사항

- **다중선택 확정**: "원하는 공모전 2개를 선택해주세요" — 투표자 1명이 라운드당 최대 2개
  후보에 투표 가능. 선택 개수 제한(2개)은 서비스 레이어에서 검증 (DB 제약 아님).
- 챗봇이 팀 `preferredCategory` 기준으로 추천 리스트를 먼저 보여주고, "전체보기" 클릭 시
  `ContestCandidate` 전체 리스트로 이동한다 (신규 조회 화면 아님, 필터 없는 동일 리스트).
- **투표 참여자 0명**: 챗봇이 대신 공모전을 골라준다 (AI 기능, 실제 투표 레코드 생성 없이
  `Team.contest` 직접 세팅).
- **동률 처리**: 동률이 발생한 후보들만 대상으로 재투표 (`round += 1`), 리더 선출과 달리
  "AI 추천 수락" 경로는 없고 재투표만 존재 (현재 화면 기준).

## 구현 현황 (Phase 6 완료)

- 엔티티/리포지토리: `domains/contest/entity/ContestCandidate.java`, `ContestVote.java`,
  `ContestCandidateRepository`, `ContestVoteRepository` — 테이블 자체는 Phase 0(V12)에서
  이미 만들어져 있었고 unique 제약도 처음부터 올바르게 설계돼 있어(리더 투표 때와 달리)
  마이그레이션 추가 없이 그대로 사용
- 서비스: `domains/contest/service/ContestVotingService.java`
  - `addCandidate` / `removeCandidate` / `getCandidates` — 팀이 `CONTEST_SELECTING` 상태인
    동안 자유롭게 후보를 추가/삭제/조회
  - `submitVote(teamId, memberId, contestCandidateIds)` — 최대 2개 다중선택, 활성 팀원 전원이
    투표하면 자동 개표. 단독 1위면 `Team.contest` 확정 + `IN_PROGRESS`로 전이, 동률이면
    동률 후보만 대상으로 다음 라운드 재투표 (리더 투표와 동일하게 라운드는 별도 컬럼 없이
    "직전 라운드가 꽉 찼는데 미확정=동률"로 계산)
- **범위 밖으로 미룬 것**: "투표 참여자 0명이면 AI가 대신 골라줌"은 마감 시간이 되어야
  트리거되는데 스케줄러가 아직 없어서(Phase 7) 구현하지 않음. 지금은 CONTEST_SELECTING과
  CONTEST_VOTING을 별도 상태로 나누지 않고 하나로 취급 — 후보 추가와 투표가 동시에 열려있는
  상태. Phase 7 스케줄러가 마감 시각 기준으로 상태를 나누고 미참여 처리를 붙일 예정.
- 상태 전이 확장: `ChatbotOrchestrationService`에 `advanceToContestSelecting`(팀장 확정 직후,
  `LeaderElectionService`가 호출), `advanceToInProgress`(공모전 확정 직후, 이 서비스가 호출)
  추가. `Team.assignContest(Contest)` 뮤테이터 추가.
- 컨트롤러: `domains/contest/controller/ContestVotingController.java` —
  `POST/GET /api/teams/{teamId}/contest-candidates`,
  `DELETE .../contest-candidates/{contestCandidateId}`,
  `POST .../contest-candidates/votes`
- 테스트: `ContestVotingServiceTest`(10개 케이스)
- **`leaveTeam` 중 공모전 투표 재확인 (2026-08-02, PR #56 리뷰 반영)**:
  `ContestVotingService.recheckAfterMemberLeft(team, leftTeamMemberId)` 추가. 팀원이 공모전
  투표 도중 나가면 남은 활성 팀원 기준으로 개표 조건이 뒤늦게 충족돼도 `submitVote` 안에서만
  확인하던 `tally`가 다시 호출되지 않는 버그가 있었다. `TeamService.leaveTeam`이 나가는
  시점 호출한다. 자세한 내용/설계 근거는 [01-team.md](./01-team.md) 참고.

## 투표 마감 리마인더 (2026-08-06, Figma 5.1.3.3 커버리지 점검 중 발견)

Figma 목업에는 마감 임박 시 "공모전 투표 완료하셨나요? 투표마감까지 10분 남았어요!" 리마인더
카드가 있는데, 기존 스케줄러(`resolveContestVotingDeadlines`)는 마감 **순간**에 확정 처리만
할 뿐 마감 전에 미리 알리는 잡이 없었다.

- `Team.contestVoteReminderNotifiedAt`(마이그레이션 V29) — 중간점검/제출확인 알림과 동일한
  "1회만 발행" 멱등성 플래그 패턴.
- `TeamScheduleService.findDueContestVoteReminderTeamIds()` — `contestCandidateDeadlineAt`이
  10분 이내로 남았고 아직 리마인더를 안 보낸 `CONTEST_SELECTING` 팀을 조회.
  `sendContestVoteReminderForTeam(teamId)`가 `MessageType.CONTEST_VOTE_REMINDER_CARD`(메타데이터
  없음)로 안내 카드를 발행한다.
- `TeamSchedulerJobs.sendContestVoteReminders()` — 마감 확정 스케줄러와 동일하게 5분 간격.
- 프론트는 리마인더 카드의 버튼 라벨("투표하기"/"결과 보기")을 서버가 정해주지 않는다 — 아래
  `GET .../contest-candidates/votes`의 `myVoted`로 직접 판단해야 한다.
- 테스트: `TeamScheduleServiceTest`, `TeamSchedulerJobsTest`.

> ⚠️ **마감 이후 리마인더/투표 차단 (2026-08-06, CodeRabbit 리뷰 반영)**: 처음엔 "마감이 지난
> 팀도 리마인더 조회에 잠깐 걸릴 수 있지만 같은 5분 주기의 마감 확정 잡이 곧 처리하니
> 무해하다"고 판단해 하한 조건을 안 뒀다. 그런데 리마인더 카드의 버튼은 사용자를 실제 투표
> 흐름(`submitVote`)으로 보내고, `submitVote` 자체는 마감 시각을 검증하지 않았다 — 마감 확정
> 잡이 늦게 돌면 마감 후 투표가 그대로 집계에 반영될 수 있는 진짜 race였다. 두 군데를 고쳤다:
> `findDueContestVoteReminderTeamIds`/`sendContestVoteReminderForTeam`에
> `contestCandidateDeadlineAt > now` 하한을 추가했고(발송 직전에도 재검증),
> `ContestVotingService.submitVote`가 마감 시각이 지나면 `CONTEST_VOTE_DEADLINE_PASSED`로
> 거부하도록 방어선을 하나 더 뒀다.

## 투표 진행 상황 조회 API (2026-08-06, Figma 5.1.3.3 커버리지 점검 중 발견)

Figma의 "공모전 투표" 바텀시트("N명 참여중..")와 "투표 결과" 상세 화면(후보별 득표 막대그래프)은
전원이 투표를 마치기 **전에도** 현재까지의 참여 인원과 후보별 득표수를 보여주는데, 기존에는 이
데이터를 조회할 API가 전혀 없었다 — 개표 결과(승자 확정 또는 동률 재투표 카드)는 전원이 투표를
마쳐야만 채팅 메시지로 왔고, 그마저도 득표수 자체는 담기지 않았다(승자의 `contestId` 또는
동률 후보 `contestCandidateIds`만 메타데이터에 실림).

- `GET /api/teams/{teamId}/contest-candidates/votes` (`ContestVotingService.getVoteStatus`)
  신규 추가. 팀 상태 제한 없이(투표 종료 후에도) 호출 가능 — `getCandidates`와 동일한
  개방성.
- 응답(`ContestVoteStatusResponse`): 현재 라운드(`round`), 라운드 완료에 필요한 인원
  (`requiredVoterCount`, 활성 팀원 수), 지금까지 투표한 서로 다른 인원 수
  (`participatedVoterCount`), 요청자 본인이 이번 라운드에 투표했는지(`myVoted`), 현재 라운드
  후보별 득표수 내림차순 목록(`results`, 각 항목은 `ContestVoteTallyItemResponse` —
  `contestCandidateId`/`contest` 요약/`voteCount`).
- 개표(승자 확정, 동률 재투표 전환) 자체는 이 API가 하지 않는다 — 기존과 동일하게 `submitVote`가
  전원 투표 완료를 감지해 처리하고, 결과는 채팅 카드로 온다. 이 API는 순수 조회용.
- 라운드/후보 자격 판정 로직(`currentRound`, `eligibleCandidates`)을 `submitVote`와 그대로
  공유해 투표 처리와 조회 결과가 항상 같은 라운드 기준으로 일치한다.
- 테스트: `ContestVotingServiceTest`(득표 집계·참여 인원·`myVoted` 검증).

> ⚠️ **확정된 팀 조회 버그 정정 (2026-08-06, CodeRabbit 리뷰 반영)**: `currentRound`는 "직전
> 라운드가 꽉 찼으면 다음 라운드로 넘어간다"는 전제로 동작하는데, 팀이 이미 공모전을 확정해
> `CONTEST_SELECTING`을 벗어난 뒤에도 이 로직을 그대로 썼다. 그 결과 확정 직후
> `getVoteStatus`를 호출하면 승자를 결정지은 마지막 라운드가 아니라 그다음(투표가 하나도 없는)
> 라운드를 조회해 모든 후보의 득표수가 0으로 보이는 버그가 있었다. 팀 상태가
> `CONTEST_SELECTING`일 때만 `currentRound`로 다음 라운드를 예측하고, 그 외(확정됨)에는
> 실제 표가 쌓인 마지막 라운드(`lastVotedRound`, `findMaxRoundByTeamId`)를 그대로 조회하도록
> 수정했다. 단독 1위 확정과 동률 재투표 끝 확정 두 케이스 모두 회귀 테스트를 추가했다.

## 미정 / 추후 확인 필요

- ~~CONTEST_SELECTING/CONTEST_VOTING 상태 분리 및 후보 마감 처리~~ → Phase 7에서 해결.
  `contestCandidateDeadlineAt`(진입 당일 23시)을 스케줄러가 5분마다 확인해
  `ContestVotingService.resolveDeadlineIfDue`를 호출하는 방식으로 확정 — 상태를 별도로 나누지
  않고 마감 시각 컬럼 + 스케줄러 조합으로 처리. 투표 참여자 0명이면 무작위 확정, 일부 투표가
  있으면 기존 개표 로직 그대로 적용. **Phase 8에서도 이 무작위 선택은 AI 추천으로 바꾸지
  않기로 결정** — 실제 LLM이 붙기 전까지는 무작위와 실질적 차이가 없어서 우선순위를 낮췄다
  ([08-ai.md](./08-ai.md) 참고). 자세한 내용은 [07-scheduler.md](./07-scheduler.md) 참고.
- ~~공모전 추천 알고리즘~~ → 2026-08-05에 확정: 카테고리 내 마감이 가장 많이 남은 순서대로
  최대 3개. 자세한 내용은 [08-ai.md](./08-ai.md) 참고.
- ~~AI 추천 공모전이 실제 후보로 등록되지 않던 문제~~ → 2026-08-05 커버리지 점검 중 발견 후 해결.
  `advanceToContestSelecting`이 `CONTEST_RECOMMEND_CARD`에 추천 공모전 id를 메타데이터로만
  실어 보내고 `ContestCandidate`는 전혀 생성하지 않고 있었다 — "전체보기 클릭 시
  `ContestCandidate` 전체 리스트로 이동한다"는 위 서술과 실제 동작이 어긋나 있던 것.
  추천된 공모전을 `advanceToContestSelecting`에서 바로 `ContestCandidate`로 등록하도록
  고쳤다. `addedByTeamMember`가 `nullable=false`라 이 시점에 이미 확정된 팀장을 등록자로
  채운다(이 메서드는 항상 팀장 확정 직후에만 호출되므로 팀장 부재 케이스는 이론상 없음).
  순환 의존(`ContestVotingService` → `ChatbotOrchestrationService`) 때문에
  `ContestVotingService.addCandidate`를 호출하는 대신 `ContestCandidateRepository`를 직접
  주입받아 처리한다. **(2026-08-15 갱신)** 이 등록 로직은 AI 호출을 트랜잭션 밖으로 빼내는
  리팩터링으로 `ChatbotOrchestrationService`에서 `ChatbotContestRecommendationTxService`로
  옮겨졌다 — `ContestCandidateRepository` 직접 주입 방식은 그대로 유지. 자세한 내용은
  [08-ai.md](./08-ai.md)의 "AI 호출 비동기 분리" 참고.

## 공모전 공유 (2026-08-05, 기능명세서 3.4.2/5.1.4)

공모전 탭에서 상세 화면의 "채팅방에 공유하기"로 특정 채팅방(들)에 공모전을 공유하는 기능.
**공유와 후보 등록은 별개 액션이다** — 공유는 팀원 누구나 볼 수 있는 카드 메시지를 남길
뿐이고, 그 카드의 "+" 버튼을 눌러야 실제로 `ContestCandidate`가 생성된다(기존
`addCandidate` 재사용, 별도 API 아님).

- `MessageType.CONTEST_SHARE_CARD` 신규 추가.
- `ContestVotingService.shareContest(teamId, memberId, contestId)` — `{"contestId": id}`
  메타데이터를 담은 카드 메시지만 발행하고 `ContestCandidate`는 만들지 않는다. 공유 자체는
  공모전 후보를 실제로 등록하는 게 아니라서 `CONTEST_SELECTING` 상태로 제한하지 않는다(다른
  단계에서도 "이거 어때요?" 하고 공유만 해둘 수 있음).
- 컨트롤러: `domains/contest/controller/ContestShareController.java` —
  `POST /api/teams/{teamId}/contest-shares` (신규). 여러 채팅방에 공유하는 건 프론트가
  선택한 방마다 이 API를 반복 호출하는 방식(기존 `addCandidate`와 동일한 "단일 팀 단위" 계약).
- 후보 등록(카드의 "+" 버튼)은 기존 `POST /api/teams/{teamId}/contest-candidates`를 그대로
  호출한다 — `CONTEST_SELECTING` 상태 제한과 중복 등록 방지(`DUPLICATE_CONTEST_CANDIDATE`)는
  기존 로직 그대로 적용된다.
- 테스트: `ContestVotingServiceTest`(공유 성공/공모전 없음 케이스).

## 관련 화면

5.1.3.3 팀 공모전 추천 및 투표, 5.1.4 공모전 공유, 공모전 후보 추가, 공모전 후보 추가 완료,
투표결과_미투표, 공모전 동률, 3.4 공모전 목록, 3.4.1 공모전 정보(공유), 3.4.2 후보로 보내기
