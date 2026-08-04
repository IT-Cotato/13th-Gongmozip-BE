# 02. 팀장 선출

## 배경/목적

팀 생성 시점의 `MatchingApplication.leaderPreference` 인원수에 따라 3가지 경로로 분기하는 팀장
선출 로직이다. 3인 팀과 4인 팀 모두 같은 분기 철학을 사용한다.

> **정책 변경 (2026-08-03)**: 과거 문서의 유효 리더 점수 합산 방식은 폐기한다. 현재 신청에는
> `WANTS`(네), `NEUTRAL`(필요하면), `DOES_NOT_WANT`(아니요)가 저장된다. 팀장 선출 모드와 매칭
> 궁합의 리더 점수는 WANTS와 NEUTRAL의 실제 인원수를 사용한다. 기존 Team 생성 코드는 아직
> 신청 정보를 연결하지 않으므로 코드 반영 전까지는 종전처럼 `OPEN_NOMINATION`만 동작한다.

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

`WANTS` 인원수를 먼저 확인하고, WANTS가 없을 때 `NEUTRAL` 인원을 추천 후보풀로 사용한다.

| 조건 | mode | 설명 |
|---|---|---|
| WANTS 1명 | `AUTO_ASSIGNED` | 해당 사용자를 투표 없이 팀장으로 자동 선출 |
| WANTS 2명 이상 | `CANDIDATE_VOTE` | WANTS 사용자들을 확정 후보로 두고 경쟁 투표 |
| WANTS 0명, NEUTRAL 1명 이상 | `OPEN_NOMINATION` | NEUTRAL 사용자들을 추천 후보풀로 사용한 뒤 선출 진행 |
| WANTS 0명, NEUTRAL 0명 | `OPEN_NOMINATION` | 후보가 없으므로 예외적으로 무작위 임시 팀장 지정 |

## 결정사항

- **CANDIDATE_VOTE**: `TeamMember.isPreLeaderCandidate = true`인 사람들을 후보로 바로
  `LeaderVote` 카드 노출. "팀장 여부 투표" 단계 생략.
- **AUTO_ASSIGNED**: 유일한 사전 후보를 `TeamRole.LEADER`로 즉시 지정. `LeaderVote` 생성 안 함.
  인사 유도 챗봇 메시지 자체에 팀장 이름 포함.
- **OPEN_NOMINATION**:
  1. 신청 당시 `NEUTRAL` 사용자를 추천 후보풀로 제공한다.
  2. 후보풀 안에서 팀장 후보 등록과 `LeaderVote`를 진행한다.
  3. NEUTRAL도 없으면 팀원 중 무작위 1명을 임시 팀장으로 지정하고 시스템 메시지로 안내한다.
- **동률 처리** (CANDIDATE_VOTE, OPEN_NOMINATION 공통): 챗봇이 AI 판단으로 추천 후보 1명을
  제시하며 두 가지 선택지 제공
  - "추천 수락하기" → 해당 후보를 즉시 `TeamRole.LEADER`로 확정
  - "재투표하기" → `LeaderVote.round += 1`, 동률이었던 후보들 대상으로 재투표

## 임시 처리 (매칭 결과 연동 전)

13번 문서의 실제 Team 생성 연결이 구현되기 전까지:

- `Team.leaderSelectionMode`는 팀 생성 시 항상 `OPEN_NOMINATION`으로 고정한다.
- `TeamMember.isPreLeaderCandidate`는 항상 `false`로 고정한다.
- `CANDIDATE_VOTE` / `AUTO_ASSIGNED` 분기 코드는 존재하지만 실제 매칭 신청 입력과 아직 연결되지
  않아 도달하지 않는다.
- 연동 시 `MatchingApplication.leaderPreference`를 `TeamMember.isPreLeaderCandidate`와 추천 후보
  정보로 복사하고, 팀원 수가 3명 또는 4명인지 함께 검증한다.

## 구현 현황 (Phase 5 완료, 2026-08-03 정책 연결은 대기)

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

- `MatchingApplication.leaderPreference`를 13번의 Team 생성 입력과 연결해야 한다.
- WANTS가 1명이면 NEUTRAL 인원과 무관하게 `AUTO_ASSIGNED`, WANTS가 2명 이상이면 NEUTRAL
  인원과 무관하게 `CANDIDATE_VOTE`로 처리한다.
- WANTS가 0명일 때 NEUTRAL 후보풀을 기존 UI·AI 추천 카드에 어떻게 표시할지는 Team 생성 연동
  단계에서 API 응답 계약을 갱신한다.

## 관련 화면

5.1.3.2 팀장 선출 계열 전체, "팀장 후보 등록 후 팀장 투표 진행", "아무도 팀장 후보 등록 X",
"VER.2 팀장투표X", "팀장 투표 동률"
