# 02. 팀장 선출

## 배경/목적

팀 생성 시점의 팀장 희망 점수 합산에 따라 3가지 경로로 분기하는 팀장 선출 로직.

> ⚠️ **데이터 갭 (2026-07-29 확인)**: 아래 로직은 `PersonalityProfile.leaderPreferenceScore`
> (원합니다=1/상관없어요=0.5/원하지않아요=0)를 전제로 설계했으나, 코드베이스 재확인 결과
> 이 필드는 **현재 어디에도 존재하지 않는다** (`PersonalityProfile` 엔티티 자체가
> `SurveySubmission`+`MatchingApplication`으로 재구성되며 사라짐, 두 엔티티 모두 팀장 희망
> 관련 필드 없음). 3분기 로직 자체는 그대로 유지하되, **데이터가 생기기 전까지는 항상
> `OPEN_NOMINATION` 경로만 동작**하도록 임시 처리한다 (아래 "임시 처리" 참고).

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

팀장 희망 점수(원합니다=1 / 상관없어요=0.5 / 원하지않아요=0)를 팀원 전체 합산해서 판정한다.
**이 점수 필드는 현재 `SurveySubmission`/`MatchingApplication` 어디에도 없다 —
survey/matching 담당자 확인 필요 (아래 "임시 처리" 참고).**

| 조건 | mode | 설명 |
|---|---|---|
| 사전 후보(score=1.0) 2명 이상 | `CANDIDATE_VOTE` | 인사 유도 → 팀장 투표(사전 후보 대상) → 선출 |
| 점수 합 1.0~1.5 | `AUTO_ASSIGNED` | 투표 없이 인사 유도 메시지에 팀장 안내 포함, 바로 확정 |
| 그 외 (후보 0명, 또는 0.5점만 있음) | `OPEN_NOMINATION` | 인사 유도 → 팀장 여부 투표(AI 2명 추천) → 팀장 투표 → 선출 |

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
  제시하며 두 가지 선택지 제공
  - "추천 수락하기" → 해당 후보를 즉시 `TeamRole.LEADER`로 확정
  - "재투표하기" → `LeaderVote.round += 1`, 동률이었던 후보들 대상으로 재투표

## 임시 처리 (데이터 갭 대응)

팀장 희망 점수 데이터가 준비되기 전까지:

- `Team.leaderSelectionMode`는 팀 생성 시 항상 `OPEN_NOMINATION`으로 고정한다.
- `TeamMember.isPreLeaderCandidate`는 항상 `false`로 고정한다.
- `CANDIDATE_VOTE` / `AUTO_ASSIGNED` 분기 로직은 코드에는 작성해두되(문서화된 스펙 그대로),
  판정 조건에 들어갈 입력값이 없어 실질적으로 도달하지 않는 dead branch 상태로 둔다.
- 데이터가 추가되면 판정 로직에 조건만 연결하면 되므로, Phase 5 구현 자체를 미루지 않는다.

## 구현 현황 (Phase 5 완료 — OPEN_NOMINATION만)

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

## 미정 / 추후 확인 필요

- **[담당자 확인 필요] 팀장 희망 점수 필드 부재** — 매칭 신청 시 "팀장 희망 여부"를 입력받아
  `SurveySubmission` 또는 `MatchingApplication`에 저장해야 `CANDIDATE_VOTE`/`AUTO_ASSIGNED`
  경로가 실제로 동작한다. survey/matching 담당자에게 공유하고 필드 추가 여부/일정 확인 필요.
- `leaderSelectionMode` 경계값 케이스 (예: 사전 후보 1명 + 상관없어요 2명 = 합 2.0인 경우
  `CANDIDATE_VOTE`인지 `AUTO_ASSIGNED`인지) — 위 데이터가 준비된 이후 실제로 마주치면 확인.

## 관련 화면

5.1.3.2 팀장 선출 계열 전체, "팀장 후보 등록 후 팀장 투표 진행", "아무도 팀장 후보 등록 X",
"VER.2 팀장투표X", "팀장 투표 동률"
