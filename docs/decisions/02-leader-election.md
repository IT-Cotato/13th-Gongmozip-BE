# 02. 팀장 선출

## 배경/목적

팀 생성 시점의 팀장 희망 응답(WANTS/NEUTRAL/DOES_NOT_WANT)에 따라 3가지 경로로 분기하는
팀장 선출 로직.

> ✅ **데이터 갭 해소 (2026-08-05)**: 아래는 원래 `PersonalityProfile.leaderPreferenceScore`
> 를 전제로 설계했으나 그 필드가 존재하지 않아(2026-07-29 확인) 오래 `OPEN_NOMINATION`
> 고정으로 남아 있었다. 이후 매칭 도메인에 `MatchingApplication.leaderPreference`
> (`WANTS`/`NEUTRAL`/`DOES_NOT_WANT`)가 추가됐고, 2026-08-05에 이 값을 `TeamMember`까지
> 스냅샷해서 실제로 3분기 판정에 연결했다. 판정 기준은 "점수 합산"이 아니라 **WANTS 응답
> 개수** 기준으로 단순화했다(아래 표 참고) — PM이 정리한 케이스①②③ 스펙과 1:1로 대응된다.

## 엔티티 · 필드 정의

### LeaderVote

| 필드 | 설명 |
|---|---|
| team | FK |
| voterTeamMember | FK |
| candidateTeamMember | FK |
| round | int, default 1 — 동률 재투표 시 증가 |

unique(team_id, voter_team_member_id, round)

> ⚠️ **버그 정정 (2026-07-30, Phase 5 구현 중 발견)**: 처음 작성한 제약(team_id,
> voter_team_member_id, **candidate_team_member_id**, round)은 팀장 투표가 화면상 단일
> 선택(라디오 버튼)인데도 한 사람이 같은 라운드에 여러 후보에게 투표할 수 있게 되는 버그였다.
> `voter_team_member_id + round`만으로 유니크하게 고쳐서 라운드당 1인 1표만 가능하게 함
> (V14 마이그레이션).

### LeaderSelectionMode 판정 기준

`TeamConverter.determineLeaderSelectionMode(List<TeamMemberInput>)`가 팀원들의
`leaderPreference == WANTS` 응답 개수만으로 판정한다(2026-08-05 구현).

| WANTS 개수 | mode | 설명 |
|---|---|---|
| 1명 | `AUTO_ASSIGNED` | 나머지가 NEUTRAL/DOES_NOT_WANT 몇 명이든 무관. 투표 없이 인사 유도 메시지에 팀장 안내 포함, 바로 확정 |
| 2명 이상 | `CANDIDATE_VOTE` | 인사 유도 → 팀장 투표(사전 후보 대상, "팀장 여부 투표" 단계 생략) → 선출 |
| 0명 | `OPEN_NOMINATION` | NEUTRAL/DOES_NOT_WANT 섞여도 무관. 인사 유도 → 팀장 여부 투표(AI 2명 추천) → 팀장 투표 → 선출 |

## 결정사항

- **CANDIDATE_VOTE**: `TeamMember.isPreLeaderCandidate = true`인 사람들을 후보로 바로
  `LeaderVote` 카드 노출. "팀장 여부 투표" 단계 생략.
- **AUTO_ASSIGNED**: 유일한 사전 후보를 `TeamRole.LEADER`로 즉시 지정. `LeaderVote` 생성 안 함.
  인사 유도 챗봇 메시지 자체에 팀장 이름 포함.
- **OPEN_NOMINATION**:
  1. 챗봇이 전원에게 "팀장 여부 투표" 요청 (AI가 2명 추천 표시, 실제 후보 지정은 아님)
  2. 응답한 `TeamMember.leaderCandidacy = WANTS`인 사람들이 후보가 되어 `LeaderVote` 진행
  3. 아무도 `WANTS`를 선택하지 않으면 팀원 중 랜덤 1명을 임시 팀장으로 지정하고 시스템 메시지로 안내
- **동률 처리** (CANDIDATE_VOTE, OPEN_NOMINATION 공통): 챗봇이 AI 판단으로 추천 후보 1명을
  제시하며 두 가지 선택지 제공. **(2026-08-15 갱신)** 이 AI 추천 호출(`tally()` 내부)은 이제
  트랜잭션 밖에서 비동기로 처리된다 — [08-ai.md](./08-ai.md)의 "AI 호출 비동기 분리" 참고.
  - "추천 수락하기" → 해당 후보를 즉시 `TeamRole.LEADER`로 확정
  - "재투표하기" → `LeaderVote.round += 1`, 동률이었던 후보들 대상으로 재투표
  - **(2026-08-05 추가) 라운드 상한**: 재투표(2라운드 이상)도 또 동률이면 더 이상 재투표를
    반복하지 않고 AI 추천 후보로 자동 확정한다("추천 수락하기"를 강제 적용한 것과 동일한 효과).
  - **(2026-08-06 추가) "재투표하기" 클릭을 팀 전체에 알리는 API 추가**: 원래
    `LeaderElectionService.castVote`가 라운드를 저장된 표 개수로만 계산해서, "재투표하기"를
    눌러도 그 사실을 다른 팀원에게 알리는 서버 액션이 없었다(투표는 `POST .../leader-votes`를
    다시 호출하면 알아서 다음 라운드로 잡히지만, 화면(Figma 5.1.3.2 "재투표")에는 재투표
    시작을 알리는 별도 챗봇 메시지가 있음). `POST /api/teams/{teamId}/leader-votes/revote`
    (`LeaderElectionService.requestRevote`) 신규 추가 — 상태는 바꾸지 않고, 마지막 동률
    카드(`aiRecommendedTeamMemberId`가 있는 `LEADER_VOTE_CARD`)에서 동률이었던
    `candidateTeamMemberIds`만 그대로 꺼내 "팀원들의 의견에 따라 재투표를 진행합니다. 팀장을
    다시 선출해 주세요." 안내 카드를 재발행한다. "추천 수락하기"(`acceptAiRecommendation`)와
    동률 카드 조회 로직(`latestPendingTiebreakCard`)을 공유하며, 동률 상태가 아니면(=최신
    카드에 `aiRecommendedTeamMemberId`가 없으면) 기존과 동일하게 `NO_PENDING_AI_RECOMMENDATION`
    으로 거부한다. 여러 명이 "재투표하기"만 동시에 눌러도 안내 카드가 중복 발행될 뿐 상태
    오염은 없다(투표 자체의 라운드/자격 판정은 여전히 `castVote`가 저장된 표만으로 계산).
    > ⚠️ **`requestRevote`↔`acceptAiRecommendation` race 정정 (2026-08-06, CodeRabbit 리뷰
    > 반영)**: 위 "상태 오염 없음" 판단은 재투표끼리의 경합만 본 것이었다 — 실제로는 한 명이
    > "재투표하기"를 누르는 사이 다른 사람이 "추천 수락하기"를 눌러 팀장이 먼저 확정되면,
    > 재투표 쪽 트랜잭션이 그 사실을 모른 채 `LEADER_DECIDED` 이후에도 "재투표를 진행합니다"
    > 카드를 발행할 수 있었다(둘 다 잠금 없이 같은 동률 카드를 읽기만 함). `TeamRepository`에
    > `findByIdWithLock`(`PESSIMISTIC_WRITE`, 매칭 도메인과 동일한 패턴)을 추가해 두 메서드
    > 모두 팀 행을 잠그도록 바꿨다 — 나중에 잠금을 얻는 트랜잭션은 앞선 트랜잭션이 커밋한
    > 최신 상태(`LEADER_DECIDED`)를 보고 `INVALID_TEAM_STATUS`로 안전하게 실패한다. `castVote`/
    > `submitCandidacy` 등 기존 메서드는 이번 변경 범위 밖이라 잠금을 추가하지 않았다 — 필요성이
    > 확인되면 별도로 다뤄야 한다. **(2026-08-17 해결 — 아래 "개표 경합 방지 락" 참고.)**

## 개표 경합 방지 락 (2026-08-17, 실사용 중 재현된 유령 카드로 발견)

바로 위에서 "범위 밖"으로 남겨뒀던 `castVote`/`submitCandidacy`가 실제로 문제를 냈다. QA
테스트 중 팀원 2명만 투표한 상태(4명 중 2명)인데 "테스터1 님이 팀장으로 확정되었습니다!"
카드가 화면에 떴다 — DB를 확인해보니 `teams.status`는 여전히 `LEADER_SELECTING`이었고
`LEADER_RESULT_CARD` 메시지 자체가 저장돼 있지도 않았다. [04-contest-voting.md](./04-contest-voting.md)의
"개표 경합 방지 락"과 정확히 같은 메커니즘 — `ChatService.postChatbotMessage`가 커밋 전에
곧바로 브로드캐스트하는데, 그 트랜잭션이 (아마 클라이언트의 중복 요청 전송으로) 경합 끝에
롤백되면서 화면엔 잠깐 보였다가 DB엔 안 남는 "유령 카드"가 발생했다.

`resolveCandidacyPhase`(팀장 여부 투표 완료 후)와 `tally`(실제 투표 완료 후) 둘 다 여러
진입점이 수렴하는 구조라, `ContestVotingService`에 적용했던 것과 동일한 패턴을 이 서비스
전체로 확장했다:

- `submitCandidacy`/`castVote`: 기존 `requireTeamInLeaderSelecting`(무잠금) 대신 이미 있던
  `requireTeamInLeaderSelectingWithLock`(원래는 `requestRevote`/`acceptAiRecommendation`
  전용)을 재사용.
- `recheckAfterMemberLeft`: 호출자(`TeamService.leaveTeam`)가 잠금 이전에 로드해둔 `Team`을
  신뢰하지 않고, 진입 시 팀 id로 다시 잠가 얻은 최신 상태로 후보 등록/투표 재확인 로직 둘 다
  처리.
- `resolveCandidacyDeadlineIfDue`/`resolveVoteDeadlineIfDue`(스케줄러): `findById` →
  `findByIdWithLock`.

이제 `requireTeamInLeaderSelecting`(무잠금 버전)을 쓰는 곳이 없어져서 그 메서드는 제거했다.
`ContestVotingService`와 마찬가지로, 브로드캐스트를 커밋 이후로 미루는 범용 안전장치는 이번
스코프에서 다루지 않았다 — 필요하면 별도로 챙겨야 한다.

## 팀장 추천 규칙기반 알고리즘 (2026-08-05, PM 스펙 반영)

카톡으로 공유된 "팀장 추천 및 추천 이유(규칙기반 : MVP)" 스펙을 그대로 구현했다. 실제 LLM
호출 없이 결정론적 점수 계산만으로 동작한다(호출 빈도가 낮고, 팀장 선출처럼 이해관계가 걸린
결정은 설명가능성·재현성이 자연스러움보다 중요하다고 판단 — `08-ai.md`의 LLM 도입 범위 논의
참고).

- **적합도 점수**: 후보의 `extroversionType`(E/A/I) × 후보를 제외한 "잔여 팀원"의 다수
  유형 조합으로 결정된다.

  | 후보 유형 | 잔여 팀원 다수 유형 | 점수 |
  |---|---|---|
  | E | I 다수 | 10 |
  | E | A 다수 | 7 |
  | E | E 다수 | 3 |
  | A | 무관 | 6 |
  | I | E 다수 | 5 |
  | I | A 또는 I 다수 | 2 |
  | (무관) | 잔여 팀원 다수 유형이 갈려 다수결이 안 되는 경우 | 7 (후보 유형과 무관, A 다수와 동일 취급 — A 후보의 "무관" 규칙보다 이 특례가 우선한다) |

- **최종점수** = 적합도 점수 + (`leaderPreference == NEUTRAL`이면 +2, 아니면 +0). 후보풀은
  활성 팀원 전원(DOES_NOT_WANT 응답자도 포함하되 가점은 못 받음).
- **추천 2인** = 최종점수 상위 2명 (`AiClient.recommendLeaderCandidates`).
- **동률 처리** (추천 2인 선정, 팀장 투표 동률 추천 공통):
  1. 최종점수(적합도+가점) 내림차순
  2. 동률 시 팀장 희망 우선순위(`LeaderPreference.effectiveLeaderUnits`: WANTS>NEUTRAL>DOES_NOT_WANT) 내림차순
  3. 그래도 동률이면 외향성 원점수(3~15점, `extroversionScore * 3`과 동치) 내림차순
  4. 그래도 동률이면 팀ID+팀원ID로 시드를 고정한 랜덤값 — 같은 팀·같은 후보 조합이면 항상
     같은 결과를 낸다(재현 가능).
- 구현: `global/ai/AiClient.recommendLeaderCandidates(teamId, activeMembers)`,
  `recommendTiebreakLeader(teamId, activeMembers, tiedCandidateTeamMemberIds)` —
  `LeaderCandidateSnapshot(teamMemberId, leaderPreference, extroversionType, extroversionScore)`를
  입력으로 받는다. `MockAiClient`가 위 표를 그대로 구현(더 이상 랜덤 셔플 아님).
  `TeamConverter.toLeaderCandidateSnapshot(TeamMember)`로 변환.
- 테스트: `global/ai/MockAiClientTest`(적합도 표, 동률 처리 4단계, 재현성 검증).

## 구현 현황 (Phase 5 완료 — 원래는 OPEN_NOMINATION만 동작하던 시기의 기록)

> 아래는 Phase 5 당시(OPEN_NOMINATION 고정) 기록이다. 2026-08-05에 `AUTO_ASSIGNED`/
> `CANDIDATE_VOTE` 분기가 실제로 연결됐다 — 바로 아래 "구현 현황 (2026-08-05 — 케이스①③
> 연결)" 섹션 참고.

- 엔티티/리포지토리: `domains/team/entity/LeaderVote.java`, `LeaderVoteRepository.java`
- 서비스: `domains/team/service/LeaderElectionService.java`
  - `submitCandidacy(teamId, memberId, wants)` — 팀장 여부 투표. 활성 팀원 전원이 응답하면
    자동으로 다음 단계 분기 (0명→무작위 임시 팀장, 1명→즉시 확정, 2명 이상→`LEADER_VOTE_CARD`
    발행). 투표가 이미 시작된 뒤에는 여부를 되돌릴 수 없도록 막음.
  - `castVote(teamId, voterMemberId, candidateTeamMemberId)` — 팀장 투표. 활성 팀원 전원이
    투표하면 자동 개표. 단독 1위면 확정, 동률이면 동률이었던 후보들만 대상으로 다음 라운드를
    안내. ~~"AI 추천 수락" 경로는 Phase 8에서 AI가 붙으면 추가~~ → Phase 8에서 연결 완료,
    자세한 내용은 [08-ai.md](./08-ai.md) 참고.
  - 라운드 번호는 별도 컬럼 없이 계산으로 도출한다: 직전 라운드가 활성 팀원 수만큼 표를
    다 받았는데도 아직 팀장이 안 정해졌다면(=동률로 끝났다면) 다음 라운드로 간주.
  - 라운드 2 이상에서는 이전 라운드의 동률 후보만 투표 가능(=eligible 후보 집합이 라운드마다
    달라짐).
- 컨트롤러: `domains/team/controller/LeaderController.java` — `PATCH .../leader-candidacy`,
  `POST .../leader-votes`
- `ChatService.postChatbotCardMessage(Team, MessageType, content, metadata)` 신규 추가 —
  `Message.metadata`에 후보 팀원 id 목록을 JSON으로 담아 카드 렌더링에 쓸 수 있게 함.
  Jackson `ObjectMapper` 스프링 빈이 이 프로젝트에 없어(Boot 4 웹 스타터 구성 이슈로 추정)
  `LeaderElectionService`가 `new ObjectMapper()`를 직접 들고 있음 — 사소한 직렬화 용도라 이걸로
  충분하지만, 다른 곳에서도 JSON 직렬화가 필요해지면 그때 `JacksonAutoConfiguration`이 왜
  안 붙는지 원인을 봐야 함.
- 마이그레이션: V14 (unique 제약 버그 수정, 위 참고)
- 테스트: `LeaderElectionServiceTest`(12개 케이스), `ChatbotOrchestrationServiceTest` 갱신
- **`leaveTeam` 중 팀장 투표 재확인 (2026-08-02, PR #56 리뷰 반영)**:
  `LeaderElectionService.recheckAfterMemberLeft(team, leftTeamMemberId)` 추가. 팀원이 팀장
  투표 도중 나가면 남은 활성 팀원 기준으로 개표 조건이 뒤늦게 충족돼도 `castVote` 안에서만
  확인하던 `tally`가 다시 호출되지 않는 버그가 있었다. `TeamService.leaveTeam`이 나가는
  시점 호출한다. 자세한 내용/설계 근거는 [01-team.md](./01-team.md) 참고.
  > ⚠️ **"팀장 여부 투표" 단계 확장 (2026-08-02, CodeRabbit PR #61 리뷰 반영)**: 처음엔
  > `LeaderVote`가 아직 없는 candidacy 단계(`submitCandidacy`)는 범위 밖으로 남겨뒀는데,
  > CodeRabbit이 이 단계도 동일한 PENDING 정체 버그가 있다고 지적해 추가로 수정했다.
  > `recheckAfterMemberLeft`가 `!leaderVoteRepository.existsByTeam_TeamId(...)`인 경우
  > (=아직 투표 시작 전) `resolveCandidacyPhase`와 동일한 경로로 재확인하되, 나간 사람의
  > `LeaderCandidacyStatus`가 `UNDECIDED`(=아직 응답 안 함)였을 때만 재확인한다 — 이미
  > 응답을 마친 사람이 나간 경우는 "전원 응답 완료" 조건에 영향이 없으므로(다른 미응답자가
  > 남아있거나 이미 다음 단계로 넘어갔거나) 재확인이 필요 없고, 잘못 재확인하면 이미 발행된
  > `LEADER_VOTE_CARD` 메시지를 중복 발행할 위험이 있다.
  > ⚠️ **득표 1위 후보 이탈 시 크래시 수정 (2026-08-02, CodeRabbit PR #61 리뷰 반영)**:
  > `recheckAfterMemberLeft`가 개표 조건 충족을 감지해 `tally`를 호출할 때, 마침 득표 1위
  > 후보 본인이 나간 사람이면 `tally`의 우승자 조회(`activeMembers`에서 후보를 찾는 로직)가
  > 실패해 `TeamException(INVALID_LEADER_CANDIDATE)`를 던졌다 — 이 예외가
  > `TeamService.leaveTeam`과 같은 트랜잭션에서 전파돼 나가기 자체가 롤백되는 심각한 버그였다.
  > `tally`가 득표 집계 전에 현재 활성 상태인 후보의 표만 먼저 걸러내도록 수정 — 나간 후보의
  > 표는 집계에서 제외되어 남은 활성 후보들 사이에서 다시 우승자가 결정된다. 유효한(=활성)
  > 후보가 아무도 안 남으면(득표했던 후보가 전부 나간 경우) 후보가 아예 없었을 때와 동일하게
  > 활성 팀원 중 1명을 임시 팀장으로 무작위 지정한다.

## 구현 현황 (2026-08-05 — 케이스①③ 연결)

- 마이그레이션 V24, `TeamMember`/`TeamMemberInput` 확장은 [01-team.md](./01-team.md) 참고.
- `TeamService.createTeam()`: `AUTO_ASSIGNED`면 WANTS 응답자에게 팀 생성 시점에 바로
  `TeamMember.assignAsLeader()`를 호출한다(투표 로직 자체를 타지 않음).
- `ChatbotOrchestrationService`:
  - `startGreeting()`이 `AUTO_ASSIGNED`면 인사 유도 메시지 자체에 팀장 이름을 포함시킨다
    (`greetingPromptFor(Team)`).
  - 전원 인사 완료(`recordGreetingAndAdvance`/`forceAdvanceGreetingIfDue`) 시
    `advanceAfterGreeting(Team, List<TeamMember>)`로 분기:
    - `AUTO_ASSIGNED`: `LEADER_SELECTING`을 건너뛰고 바로 `LEADER_DECIDED` → `CONTEST_SELECTING`.
    - `CANDIDATE_VOTE`: `postCandidateVoteCard()` — 사전 후보(`isPreLeaderCandidate=true`)는
      `leaderCandidacy=WANTS`, 나머지는 `DOES_NOT_WANT`로 즉시 확정하고 바로
      `LEADER_VOTE_CARD` 발행("팀장 여부 투표" 단계 생략). 이렇게 해야
      `LeaderElectionService.castVote()`의 "전원 응답 완료" 선행조건을 만족한다.
    - `OPEN_NOMINATION`: AI 추천 2명을 담은 `LEADER_NOMINATION_CARD` 발행(추천 2명).
      **(2026-08-15 갱신)** AI 호출(`AiClient.recommendLeaderCandidates`)은 커밋 후
      `ChatbotLeaderNominationAsyncService`가 비동기로 처리 — 자세한 내용은
      [08-ai.md](./08-ai.md)의 "AI 호출 비동기 분리" 참고.
  - `ChatbotOrchestrationService`는 `LeaderElectionService`에 의존하지 않는다(반대 방향
    의존은 이미 존재 — `assignLeader()`가 `advanceToContestSelecting()`을 호출) —
    순환 의존을 피하려고 `CANDIDATE_VOTE` 카드 발행 로직을 `LeaderElectionService`에 위임하지
    않고 `ChatbotOrchestrationService` 안에 직접 둔 것.
- 팀장 추천 규칙기반 알고리즘은 위 "팀장 추천 규칙기반 알고리즘" 섹션 참고.
- 테스트: `ChatbotOrchestrationServiceTest`(AUTO_ASSIGNED 인사말/단계 스킵, CANDIDATE_VOTE
  카드 발행 신규 케이스), `LeaderElectionServiceTest`(재투표 라운드 상한 신규 케이스),
  `TeamServiceTest`(AUTO_ASSIGNED/CANDIDATE_VOTE 판정 신규 케이스).

## 팀장 확정 결과 카드 (2026-08-05, Figma 5.1.3.2 결과 화면 확인 후 추가)

Figma 목업을 다시 확인해보니 팀장이 최종 확정될 때(단독 1위, 동률 재투표/AI 추천 수락,
후보 0명→무작위 지정, AUTO_ASSIGNED 인사말 직후 모두 포함) 아바타+이름이 노출되는 카드가
붙어 있었는데, 그동안은 `postChatbotMessage`로 평문 텍스트만 보내고 있어 프론트가 카드를
그릴 방법이 없었다.

- `MessageType.LEADER_RESULT_CARD` 신규 추가.
- `LeaderElectionService.assignLeader()`(팀장이 확정되는 모든 경로가 공유하는 단일 지점)가
  `postChatbotMessage` 대신 `postChatbotCardMessage`로 `{"leaderTeamMemberId": <id>}`
  메타데이터를 실어 보낸다.
- `ChatbotOrchestrationService`의 `AUTO_ASSIGNED` 인사 완료 분기도 동일하게 카드로 변경.
  두 서비스가 각자 `toLeaderResultMetadata`/`toIdMetadata` 헬퍼를 따로 두되(순환 의존
  방지), 메타데이터 키(`leaderTeamMemberId`)는 동일하게 맞춰 프론트가 카드 출처와 무관하게
  같은 방식으로 파싱할 수 있도록 했다.
- 테스트: `LeaderElectionServiceTest`(모든 assignLeader 경로에서 카드 발행 검증),
  `ChatbotOrchestrationServiceTest`(AUTO_ASSIGNED 카드 메타데이터 검증).

## 미정 / 추후 확인 필요

- `leaderSelectionMode` 경계값 케이스는 이제 WANTS 개수만으로 결정돼 모호성이 없다(과거 "점수
  합 1.0~1.5" 방식에서 있던 경계값 문제는 해소됨).
- 매칭 도메인의 `LeaderRecommendation`/`MatchingAiWorker`(`POST/GET
  /api/ai/teams/{teamId}/leader-recommendation`)는 이번 변경과 별개로 존재하는 온디맨드 API로,
  이번에 구현한 채팅 자동 흐름(`AiClient.recommendLeaderCandidates`)과는 연결돼 있지 않다.
  두 메커니즘이 중복돼 있으니 프론트에서 실제로 온디맨드 API를 쓰는지 확인 후 정리 필요.
- 실제 LLM 연동은 이번 스코프 밖 — 위 알고리즘은 순수 규칙기반이다.

## 팀장 후보/투표 마감 타이머 (2026-08-05, ContestVotingService 패턴 그대로 적용)

Figma 목업의 "투표 마감까지 00:00:00" 카운트다운에 대응하는 백엔드가 없었던 문제(마감
안 되면 `LEADER_SELECTING`에 무한정 머무를 수 있음)를 해소했다. GREETING의 2시간
타임아웃, 공모전의 `contestCandidateDeadlineAt` + 스케줄러와 동일한 패턴을 그대로 따랐다.
최초 구현은 마감 시각을 하나만 두고 "후보 등록"과 "투표" 두 하위 단계가 공유했는데,
2026-08-15에 두 마감을 완전히 분리했다 — 아래 "후보 등록/투표 마감 분리" 참고.

- `LeaderElectionService.resolveCandidacyDeadlineIfDue(teamId)` / `resolveVoteDeadlineIfDue(teamId)`
  — 스케줄러가 각각 5분 간격으로 호출한다. 지금 어느 하위 단계인지는 활성 팀원의
  `leaderCandidacy`로 판정한다(`existsByTeam_TeamId`만으로는 "후보 확정 직후 아직 아무도
  투표 안 한 상태"와 "아직 후보 여부 투표 중"을 구분할 수 없기 때문 — 둘 다 `LeaderVote`가
  0건이다). 각 메서드는 지금이 자기 담당 하위 단계가 아니면 아무 것도 하지 않고 즉시
  리턴한다(후보 등록 마감 처리가 이미 투표 단계로 넘어간 팀을 건드리지 않도록, 그 반대도
  마찬가지).
  - `resolveCandidacyDeadlineIfDue`: 한 명이라도 `UNDECIDED`면 마감까지 응답 안 한 사람을
    `DOES_NOT_WANT`로 간주하고 `resolveCandidacyPhase`를 그대로 재사용해 확정한다(0/1/2명
    이상 분기 로직 재사용). 이미 후보 등록이 끝난 팀이면 아무 것도 하지 않는다.
  - `resolveVoteDeadlineIfDue`: 아직 후보 등록이 안 끝났으면 아무 것도 하지 않는다(이론상
    후보 등록 마감이 먼저 지났어야 하는 상황). 후보 등록이 끝났다면 현재 라운드에 투표가
    있으면 있는 대로 `tally()`로 개표하고, 하나도 없으면 무작위로 임시 팀장을 지정한다
    (`ContestVotingService.resolveDeadlineIfDue`와 동일한 정책).
- 스케줄러: `TeamScheduleService.findDueLeaderCandidacyDeadlineTeamIds`/
  `resolveLeaderCandidacyDeadlineForTeam` + `findDueLeaderVoteDeadlineTeamIds`/
  `resolveLeaderVoteDeadlineForTeam`, `TeamSchedulerJobs.resolveLeaderCandidacyDeadlines`/
  `resolveLeaderVoteDeadlines`(둘 다 5분 간격 cron, 팀별 예외 격리는 기존 잡들과 동일,
  `@SchedulerLock`로 다중 인스턴스 중복 실행 방지).
- 테스트: `LeaderElectionServiceTest`(마감 시 미응답자 자동 거절/무투표 무작위 지정/부분
  투표 개표/후보 등록·투표 마감 분리 신규 케이스), `TeamScheduleServiceTest`, `TeamSchedulerJobsTest`.

### 후보 등록/투표 마감 분리 (2026-08-15, PM 요구사항)

기존에는 `leaderSelectionDeadlineAt` 하나가 "후보 등록"과 "투표" 두 하위 단계를 전부
커버해서, 후보 등록이 늦게 끝나면(예: 마감 직전까지 응답 안 한 사람이 있어서) 투표에 쓸 수
있는 실제 시간이 그만큼 줄어드는 문제가 있었다. PM 요구사항으로 두 마감을 완전히 분리했다.

- **후보 등록 마감**: 3시간(`ChatbotOrchestrationService.LEADER_CANDIDACY_TIMEOUT_HOURS`) —
  `LEADER_SELECTING` 진입 시(`advanceAfterGreeting`) 1회 세팅. `Team.leaderCandidacyDeadlineAt`
  (마이그레이션 V36, 기존 `leaderSelectionDeadlineAt`을 rename).
- **투표 마감**: 8시간(`LeaderElectionService.LEADER_VOTE_TIMEOUT_HOURS`) —
  `Team.leaderVoteDeadlineAt`(마이그레이션 V36, 신규 컬럼)에 세팅. 후보 등록 마감과 달리
  **투표 라운드가 새로 열릴 때마다 매번 새로 세팅된다** — 동률로 재투표(`tally()`의 동률
  분기)가 시작될 때도 직전 라운드에서 남은 시간을 물려받지 않고 그 시점부터 다시 8시간을
  받는다(PM 결정: "재투표마다 8시간 새로 카운트"). 재투표 카드를 다시 보여주기만 하는
  `requestRevote()`(같은 라운드를 재공지)는 새 라운드를 여는 게 아니므로 마감을 갱신하지
  않는다.
- `resolveCandidacyPhase`가 후보 2명 이상이라 `LEADER_VOTE_CARD`를 처음 발행하는 시점,
  그리고 `LeaderTiebreakTxService.applyTiebreakResult`가 동률이라 재투표 카드를 발행하는
  시점, 이 두 곳에서만 `team.scheduleLeaderVoteDeadline(...)`을 호출한다. 후보 0/1명(즉시
  확정)인 경우는 투표 자체가 없으므로 건드리지 않는다. **(2026-08-15 갱신)** 동률 쪽은
  원래 `tally()` 안에서 직접 세팅했는데, AI 추천 호출을 비동기로 빼면서
  `LeaderTiebreakTxService`로 옮겨졌다 — [08-ai.md](./08-ai.md) 참고.
- `AUTO_ASSIGNED`는 여전히 대기 자체가 없어 둘 다 세팅되지 않는다.
- `status=LEADER_SELECTING AND leaderCandidacyDeadlineAt/leaderVoteDeadlineAt <= now` 스캔용
  복합 인덱스(`idx_teams_status_leader_candidacy_deadline_at`,
  `idx_teams_status_leader_vote_deadline_at`, 마이그레이션 V37)를 같이 추가했다 — 기존
  `leaderSelectionDeadlineAt`에도 없던 인덱스였는데, 스케줄러 job이 1개에서 2개로 늘면서
  인덱스 없는 스캔 비용도 같이 늘어 이번에 챙겼다(`submission_check_reminder_at`이 V30에서
  CodeRabbit 리뷰로 인덱스를 받았던 것과 동일한 이유).
- **API 노출 갱신**: `GET /api/teams/{teamId}/members`(`TeamMembersResponse`)의
  `leaderSelectionDeadlineAt` 필드가 `leaderCandidacyDeadlineAt`으로 이름이 바뀌고,
  `leaderVoteDeadlineAt`이 새로 추가됐다 — **프론트 연동 영향 있음**: 후보 등록 단계에서는
  `leaderCandidacyDeadlineAt`만 채워지고 `leaderVoteDeadlineAt`은 null, 투표 단계로
  넘어가면 반대가 된다. 값 자체는 실시간으로 push되지 않는 고정 시각이다 — 프론트가 이
  값을 한 번 받아 `deadline - 지금시각`을 로컬에서 매초 계산해 카운트다운을 그리면 되고,
  마감 이후 처리(팀장 확정 등)는 평소처럼 채팅 메시지로 실시간 push된다.
  > ⚠️ **팀장 확정 이후에도 값이 지워지지 않음**: `LEADER_SELECTING`을 벗어나도(팀장 확정 등)
  > 두 필드를 명시적으로 `null`로 지우는 코드가 없어 과거 마감 시각이 DB에 계속 남는다.
  > 의도적으로 고치지 않았다 — 스케줄러 조회 자체가 `status=LEADER_SELECTING AND
  > deadline<=now`로 필터링하고 각 `resolve...DeadlineIfDue`도 진입 시 상태를 한 번 더
  > 확인해 이중으로 막혀 있어 스케줄러 오동작 위험이 없고, 프론트도 같이 내려주는 `status`가
  > `LEADER_SELECTING`일 때만 카운트다운을 그리면 되므로 남아있는 값이 화면에 노출될 일도
  > 없다. 단순 데이터 정리(hygiene) 문제라 기능상 영향은 없음.

## 관련 화면

5.1.3.2 팀장 선출 계열 전체, "팀장 후보 등록 후 팀장 투표 진행", "아무도 팀장 후보 등록 X",
"VER.2 팀장투표X", "팀장 투표 동률"
