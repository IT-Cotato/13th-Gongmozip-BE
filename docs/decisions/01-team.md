# 01. Team / TeamMember

## 배경/목적

매칭된 팀(=채팅방)의 상태와 참여자 정보를 관리한다. 챗봇 상태머신, 팀장 선출, 공모전 선정이
모두 이 엔티티들을 기준으로 동작한다.

## 엔티티 · 필드 정의

### Team

| 필드 | 타입 | 설명 |
|---|---|---|
| teamId | PK | |
| status | `TeamStatus` | 챗봇 진행 단계. 아래 상태머신 참고 |
| preferredCategory | `InterestCategory` | 매칭 시 팀 대표 카테고리 (공모전 추천 AI 입력값) |
| leaderSelectionMode | `LeaderSelectionMode` | 팀 생성 시점에 1회 계산 후 고정. [02](./02-leader-election.md) 참고 |
| contest | `Contest` FK, nullable | 투표로 확정되면 세팅 |
| chatbotEnabled | boolean, default true | [03-chat.md](./03-chat.md) 참고 |
| progressPercent | int, nullable | 중간점검 슬라이더 |
| progressCheckAt | LocalDateTime, nullable | contest 확정 시 계산. [07-scheduler.md](./07-scheduler.md) |
| submissionCheckAt | LocalDateTime, nullable | contest 확정 시 계산. [07-scheduler.md](./07-scheduler.md) |
| submitted | boolean | 팀장의 "진행 완료" 여부 |

### TeamStatus (상태머신)

```
MATCHED → GREETING → LEADER_SELECTING → LEADER_DECIDED
        → CONTEST_SELECTING → CONTEST_VOTING → CONTEST_DECIDED
        → IN_PROGRESS → SUBMITTED → COMPLETED
```

- `AUTO_ASSIGNED` 경로에서는 `LEADER_SELECTING` 단계를 건너뛰고 `GREETING` 메시지 자체에
  팀장 안내가 포함된 채로 바로 `LEADER_DECIDED`로 진입한다.

### TeamMember

| 필드 | 타입 | 설명 |
|---|---|---|
| team, member | FK | unique(team_id, member_id) |
| profile | `Profile` FK | 팀 생성(매칭 신청) 시점에 사용된 프로필 스냅샷. `profiles.is_main`이 제거되어 "대표 프로필" 개념이 없어졌으므로, 이 팀에서 어떤 프로필로 참여했는지를 명시적으로 고정한다 |
| role | `TeamRole` (`LEADER`/`MEMBER`) | |
| isPreLeaderCandidate | boolean | 팀 생성 시점 팀장 희망 점수 스냅샷. **현재는 항상 false로 고정** — [02-leader-election.md](./02-leader-election.md)의 데이터 갭 참고 |
| leaderCandidacy | `LeaderCandidacyStatus` (`UNDECIDED`/`WANTS`/`DOES_NOT_WANT`) | `OPEN_NOMINATION` 경로에서 사용 (현재 유일하게 동작하는 경로) |
| status | `ACTIVE`/`LEFT` | 채팅방 나가기 |
| greetedAt | LocalDateTime, nullable | 자기소개 메시지 최초 전송 시각. "전원 인사 완료" 판정용 |
| lastReadAt | LocalDateTime, nullable | 안읽음 배지 계산용 read cursor |
| joinedAt / leftAt | LocalDateTime | |

## 결정사항

- **엔티티 참조 정정**: 최초 설계 시 `PersonalityProfile`을 참조했으나, 현재 코드베이스에는
  해당 엔티티가 없다. `survey` 도메인은 `SurveySubmission`(설문 점수 보유) +
  `MatchingApplication`(매칭 신청 시점 스냅샷: `contestCategory`, `skillScore`, `skillGroup`,
  `collaborationDistance` 등)으로 재구성되어 있다. `Team.preferredCategory`는
  `MatchingApplication.contestCategory`를 참조하는 것으로 정정한다.
- 팀 생성 시 `leaderSelectionMode`를 1회 계산하고 각 팀원의 `isPreLeaderCandidate`를
  스냅샷하는 설계 자체는 유지하되, **입력 데이터(팀장 희망 점수)가 현재 존재하지 않아
  임시로 항상 `OPEN_NOMINATION` + `isPreLeaderCandidate=false`로 고정한다.**
  자세한 내용/후속 조치는 [02-leader-election.md](./02-leader-election.md).
- 챗봇을 가상의 TeamMember row로 만들지 않는다 (→ [03-chat.md](./03-chat.md)).
- **팀 생성 입력 계약**: 매칭 알고리즘이 어떻게 그룹을 짜는지는 이 도메인이 알 필요가 없다.
  `team` 도메인은 아래 계약만 받으면 팀을 생성할 수 있도록 설계한다.
  ```
  TeamCreationRequest {
      List<TeamMemberInput> members   // memberId + profileId (매칭 신청에 사용된 프로필)
      InterestCategory preferredCategory   // MatchingApplication.contestCategory 기반
  }
  ```
  현재 코드베이스에는 "개인의 매칭 신청"(`MatchingApplication`)만 있고 이를 그룹핑해서
  팀을 만드는 로직/엔티티는 존재하지 않는다. 매칭 그룹핑 구현 여부·시점과 무관하게, 위
  계약을 만족하는 호출(API 또는 내부 이벤트)이 있으면 Phase 1은 동작 가능하도록 만든다.
  Phase 1 개발 중에는 시드 데이터/테스트용 트리거로 이 계약을 대체한다.

## 미정 / 추후 확인 필요

- **팀장 희망 점수 데이터 부재** — survey/matching 담당자 확인 필요. [02-leader-election.md](./02-leader-election.md) 참고.
- **팀 그룹핑(매칭 결과) 산출 방식** — 현재 존재하지 않음. 언제/누가/어떤 형태로 만들지 확인 필요.
  확인 전까지는 위 `TeamCreationRequest` 계약만으로 Phase 1을 진행.
- 팀장 변경(수동 위임) 기능 — 챗봇 안내 문구에 언급되지만 화면/플로우 미정. TeamMember.role
  갱신 API로 충분해 보이나 별도 스코프로 분리 예정.

## 구현 현황 (Phase 1 완료)

- 엔티티: `domains/team/entity/Team.java`, `TeamMember.java`
- 리포지토리: `domains/team/repository/TeamRepository.java`, `TeamMemberRepository.java`
- 서비스: `domains/team/service/TeamService.java` — `createTeam`(내부 계약, 컨트롤러 미노출),
  `getMyChatRooms`(채팅방 목록), `getTeamMembers`(대화상대 조회, 팀 소속 검증 포함)
- **성능 개선 (2026-08-01)**: `getMyChatRooms`가 원래 채팅방 개수(N)만큼
  팀원 목록/마지막 메시지 조회 쿼리를 반복하고, 팀원의 `member`가 fetch join 안 돼있어 팀원
  수만큼 추가 lazy load까지 겹치는 N+1이었다. `TeamMemberRepository.findByTeamIdInAndStatus`
  (배치, member까지 JOIN FETCH)와 `MessageRepository.findLatestMessagePerTeam`(배치, native
  query)로 쿼리 수를 앱 실행당 상수 개로 줄였다. 안 읽은 메시지 개수만 팀마다 기준 시각
  (`lastReadAt`)이 달라 배치가 까다로워 그대로 팀 수만큼 남겨뒀다(가벼운 COUNT라 영향은 작음).
  `findByTeamIdAndStatus`(단건)도 같은 김에 `member` JOIN FETCH를 추가해
  `TeamProgressService.submitCompletion`의 N+1도 같이 해결됨.
- **`profileId` 노출 (2026-08-01, Figma 5.1.2.2.3 확인 후 추가)**: 채팅방에서 팀원 프로필로
  진입하려면(`GET /api/public/profiles/{profileId}` 등 기존 profile 도메인 API) profileId가
  필요한데 `TeamMemberSummaryResponse`에 없었다. `TeamMemberSummaryResponse.profileId` 추가.
- **아바타 데이터 연동 (2026-08-01, Figma 5.1 확인 후 추가)**: 채팅방 목록/팀원 목록 화면에
  캐릭터 아바타가 필요한데, 기존 응답에는 memberId조차 없어서 프론트가 아바타를 그릴 방법이
  없었다. `character` 도메인의 `CharacterService.findAvatarsByMembers(List<Member>)`(신규,
  캐릭터 정의/태그/특징 join 없이 `characterType`+`paletteCode`만 가볍게 배치 조회)를 호출해
  `ChatRoomSummaryResponse.avatars`(방 안 "나 제외 팀원"의 아바타 목록)와
  `TeamMemberSummaryResponse.avatar`(팀원 개별 아바타, 없으면 null)에 채워 넣는다. 협업 유형
  검사를 안 한 팀원은 결과에서 빠진다(예외 아님). `SurveySubmissionRepository`/
  `MemberCharacterRepository`에 `...In` 배치 조회 메서드를 추가해 팀원 수만큼 반복 쿼리하지
  않도록 함(이번에 방금 고친 N+1을 다시 만들지 않기 위해).
- 컨트롤러: `GET /api/teams`, `GET /api/teams/{teamId}/members`
- 테스트: `domains/team/service/TeamServiceTest.java`
- **`leaveTeam` 중도 이탈 시 PENDING 정체 수정 (2026-08-02, PR #56 리뷰 반영)**: 팀원이 팀장/
  공모전 투표나 리뷰 진행 도중 나가면 `activeMembers` 분모가 줄어드는데, 자동 개표/완료 조건
  (`LeaderElectionService.tally`, `ContestVotingService.tally`, `ReviewService.completeReviewIfAllDone`)은
  원래 새 투표/리뷰가 "제출"되는 시점에만 확인된다. 나간 사람이 마지막 미제출자였던 경우
  아무도 다시 확인하지 않아 팀이 다음 단계로 못 넘어가고 계속 PENDING 상태에 머무는 버그가
  있었다. `TeamService.leaveTeam`이 나간 시점의 `Team.status`에 따라 해당 서비스의
  `recheckAfterMemberLeft`를 호출해 즉시 재확인하도록 고쳤다. 개표 로직은 동률 시 재투표
  라운드를 DB에 명시적으로 기록하지 않아 "이미 개표했는지" 여부를 안전하게 재현할 수 없는데,
  "나간 사람이 해당 라운드에 아직 투표하지 않았을 때만 재확인"하는 조건으로 이 모호성을
  피했다 — 나간 사람이 이미 투표했었다면 개표는 이미 실행됐거나 다른 미투표자가 남아있는
  것이므로 중복 개표 위험이 없다. GREETING/팀장 여부 투표 단계는 이번 수정 범위에 포함하지
  않음(리뷰에서 지목된 범위가 "투표/리뷰"였고, GREETING은 별도로 추가한 2시간 타임아웃
  스케줄러가 최종 안전망 역할을 함).
- ⚠️ **임시**: `domains/team/controller/TeamTestController.java` (`POST /api/test/teams`,
  `@Profile("local")`) — 매칭 연동 전까지 수동 테스트(WebSocket 채팅 등)를 위해 `createTeam`을
  직접 호출할 수 있게 열어둔 개발용 엔드포인트. `local` 프로필을 명시적으로 켰을 때만 활성화되는
  fail-safe 방식(보안 검토 후 `!prod`에서 변경, 자세한 이유는 [api.md](../api.md) 참고). 매칭
  도메인이 실제로 연동되면 삭제.

## 구현 현황 (Phase 4 — 챗봇 상태머신 골격)

- `domains/chatbot/service/ChatbotOrchestrationService.java` — `Team.status` 전이 엔진의
  첫 부분. 이번 Phase에서는 `MATCHED → GREETING → LEADER_SELECTING` 두 전이만 구현:
  - `startGreeting(Team)`: `TeamService.createTeam` 완료 직후 호출됨. 상태를 `GREETING`으로
    바꾸고 인사 유도 챗봇 메시지를 발행한다.
  - `recordGreetingAndAdvance(teamId, memberId)`: `ChatWebSocketController.sendMessage`가
    멤버 메시지 저장 직후 호출한다. `GREETING` 상태가 아니면 아무 것도 안 하고, 발신자의
    `TeamMember.greetedAt`이 비어있으면 최초 메시지를 인사로 간주해 기록한다. 활성 팀원
    전원이 인사를 마치면 상태를 `LEADER_SELECTING`으로 전이하고 다음 안내 메시지를 발행한다.
  - 챗봇 메시지 발행은 `ChatService.postChatbotMessage(Team, String)`(신규)를 통해 저장+
    실시간 브로드캐스트까지 한 번에 처리한다 (`postSystemMessage`와 동일한 패턴).
- 순환 의존 방지를 위해 `ChatService`는 `ChatbotOrchestrationService`를 모른다 — 조립은
  호출자(`TeamService`, `ChatWebSocketController`)가 두 서비스를 순서대로 호출하는 방식으로
  처리한다.
- 이후 Phase(5~7)는 `LEADER_DECIDED`/`CONTEST_DECIDED`/`SUBMITTED` 등 나머지 전이를 이
  서비스에 메서드로 추가하며 확장한다. ~~AI 추천 로직은 Phase 8에서 연결 (현재는 고정 문구).~~
  → Phase 8에서 연결 완료, 자세한 내용은 [08-ai.md](./08-ai.md) 참고.
- 테스트: `domains/chatbot/service/ChatbotOrchestrationServiceTest.java`,
  `TeamServiceTest`(createTeam 시 `startGreeting` 호출 검증)

## 관련 화면

5.1 채팅목록, 5.1.2 팀원 프로필 열람(비공개 포함)
