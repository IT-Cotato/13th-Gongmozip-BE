# 01. Team / TeamMember

## 배경/목적

매칭된 팀(=채팅방)의 상태와 참여자 정보를 관리한다. 챗봇 상태머신, 팀장 선출, 공모전 선정이
모두 이 엔티티들을 기준으로 동작한다.

## 엔티티 · 필드 정의

### Team

| 필드 | 타입 | 설명 |
|---|---|---|
| teamId | PK | |
| status | `TeamStatus` | 챗봇 진행 단계. 아래 상태머신 참고. `GET /api/teams/{teamId}/members`(`TeamMembersResponse.status`)로 노출 (2026-08-02, PR #56 리뷰 반영 — 이전엔 어디에도 안 나가서 프론트가 "지금 SUBMITTED라 팀원 리뷰 팝업을 띄워야 하는지" 등을 판단할 방법이 없었음) |
| preferredCategory | `InterestCategory` | 매칭 시 팀 대표 카테고리 (공모전 추천 AI 입력값) |
| leaderSelectionMode | `LeaderSelectionMode` | 팀 생성 시점에 1회 계산 후 고정. [02](./02-leader-election.md) 참고 |
| contest | `Contest` FK, nullable | 투표로 확정되면 세팅 |
| contestCandidateDeadlineAt | LocalDateTime, nullable | 공모전 후보/투표 마감 시각. `GET /api/teams/{teamId}/members`(`TeamMembersResponse.contestCandidateDeadlineAt`)로 노출 (2026-08-05). [04-contest-voting.md](./04-contest-voting.md) 참고 |
| chatbotEnabled | boolean, default true | [03-chat.md](./03-chat.md) 참고 |
| progressPercent | int, nullable | 중간점검 슬라이더 |
| progressCheckAt | LocalDateTime, nullable | contest 확정 시 계산. [07-scheduler.md](./07-scheduler.md) |
| submissionCheckAt | LocalDateTime, nullable | contest 확정 시 계산. [07-scheduler.md](./07-scheduler.md) |
| submitted | boolean | 팀장의 "진행 완료" 여부 |
| leaderSelectionDeadlineAt | LocalDateTime, nullable | 팀장 여부 투표/팀장 투표 마감 시각. `GET /api/teams/{teamId}/members`(`TeamMembersResponse.leaderSelectionDeadlineAt`)로 노출 (2026-08-05). [02-leader-election.md](./02-leader-election.md) 참고 |
| version | Long | `@Version` 낙관적 잠금(마이그레이션 V28, 2026-08-06). `GREETING` 상태 전이 동시 실행 방지용 — [07-scheduler.md](./07-scheduler.md) 참고 |

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
| leaderPreference | `LeaderPreference` (`WANTS`/`NEUTRAL`/`DOES_NOT_WANT`) | 팀 생성(매칭 신청) 시점 팀장 희망 여부 스냅샷. `leaderSelectionMode` 판정과 팀장 추천 알고리즘 입력값 (2026-08-05, [02-leader-election.md](./02-leader-election.md) 참고) |
| extroversionType | `ExtroversionType` (`I`/`A`/`E`) | 팀 생성 시점 협업 유형 검사 외향성 유형 스냅샷. 팀장 추천 알고리즘의 "잔여 팀원 다수 유형" 판정에 쓰인다 |
| extroversionScore | BigDecimal | 외향성 원점수(3문항 평균, 1.0~5.0) 스냅샷. 팀장 추천 동률 처리 3순위(원점수 3~15점 환산)에 쓰인다 |
| role | `TeamRole` (`LEADER`/`MEMBER`) | |
| isPreLeaderCandidate | boolean | 팀 생성 시점 팀장 희망("네") 스냅샷. `leaderPreference == WANTS`인 팀원만 true (2026-08-05부터 실제 값 반영, 과거엔 데이터 부재로 항상 false 고정이었음) |
| leaderCandidacy | `LeaderCandidacyStatus` (`UNDECIDED`/`WANTS`/`DOES_NOT_WANT`) | `OPEN_NOMINATION`/`CANDIDATE_VOTE` 경로에서 사용 |
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
- **(2026-08-05 갱신)** 팀 생성 시 `leaderSelectionMode`를 1회 계산하고 각 팀원의
  `isPreLeaderCandidate`를 스냅샷하는 설계가 실제로 동작한다. 매칭 도메인이
  `MatchingGroupCompletionService`에서 `MatchingApplication.leaderPreference`/
  `extroversionType`/`extroversionScore`를 `TeamMemberInput`에 실어 보내고,
  `TeamConverter.determineLeaderSelectionMode()`가 팀원들의 WANTS 응답 개수로
  `CANDIDATE_VOTE`/`AUTO_ASSIGNED`/`OPEN_NOMINATION`을 실제로 분기한다. 자세한 내용은
  [02-leader-election.md](./02-leader-election.md).
- 챗봇을 가상의 TeamMember row로 만들지 않는다 (→ [03-chat.md](./03-chat.md)).
- **팀 생성 입력 계약**: 매칭 알고리즘이 어떻게 그룹을 짜는지는 이 도메인이 알 필요가 없다.
  `team` 도메인은 아래 계약만 받으면 팀을 생성할 수 있도록 설계한다.
  ```
  TeamCreationRequest {
      List<TeamMemberInput> members   // memberId, profileId, leaderPreference,
                                       // extroversionType, extroversionScore
                                       // (모두 매칭 신청 시점 MatchingApplication 스냅샷)
      InterestCategory preferredCategory   // MatchingApplication.contestCategory 기반
  }
  ```
  **(2026-08-05 갱신)** 매칭 도메인의 `MatchingGroupCompletionService`(수락 API/수동 패스/
  12시 마감 공통 확정 로직)가 실제로 이 계약을 만족해 `TeamService.createTeam()`을 호출한다.

## 미정 / 추후 확인 필요

- 팀장 변경(수동 위임) 기능 — 챗봇 안내 문구에 언급되지만 화면/플로우 미정. TeamMember.role
  갱신 API로 충분해 보이나 별도 스코프로 분리 예정.
- **(2026-08-06 삭제)** `domains/team/controller/TeamTestController.java`(`POST /api/test/teams`) —
  매칭 연동이 실제로 완료돼(위 참고) 더 이상 필요 없어 삭제했다. `chat-test.html`의 "빠른 준비"
  버튼도 이미 `TeamMemberInput`에 추가된 필드(leaderPreference 등)를 안 보내고 있어 어차피
  실패하던 상태였다 — 함께 제거했다. 자세한 내용은 [03-chat.md](./03-chat.md) 참고.

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
  것이므로 중복 개표 위험이 없다. GREETING 단계는 이번 수정 범위에 포함하지 않음(별도로 추가한
  2시간 타임아웃 스케줄러가 최종 안전망 역할을 함).
  > ⚠️ **팀장 여부 투표 단계 누락 발견 및 수정 (2026-08-02, CodeRabbit PR #61 리뷰 반영)**:
  > 처음엔 "팀장 여부 투표"(`LeaderVote`가 아직 하나도 없는 candidacy 단계) 중 나가는 경우는
  > 범위 밖으로 남겨뒀는데, CodeRabbit이 이 경우도 똑같이 PENDING에 남을 수 있다고 지적했다
  > (마지막 미응답자가 나가버리면 아무도 다시 확인하지 않음). `LeaderElectionService.recheckAfterMemberLeft`가
  > `LeaderVote`가 없는 상태에서도, 나간 사람이 응답 전(UNDECIDED)이었을 때만 재확인하도록
  > 확장했다. 자세한 내용은 [02-leader-election.md](./02-leader-election.md) 참고.
- **`Team.status` 노출 (2026-08-02, Figma 5.1.3.6/팀원 리뷰 팝업 확인 후 추가)**: 공모전
  제출 완료 시(`TeamProgressService.submitCompletion`) 활성 팀원 전원에게 협업거리 포인트를
  자동 지급하고 완료 시스템 메시지를 브로드캐스트하는 것까지는 이미 구현돼 있었지만, 정작
  `Team.status` 자체는 어떤 응답에도 노출되지 않아 프론트가 "지금 SUBMITTED니까 팀원 리뷰
  팝업을 띄워야 한다"는 걸 판단할 방법이 없었다(채팅 메시지의 `SYSTEM_NOTICE` 타입은 나가기/
  챗봇 토글 메시지와 구분이 안 되고, 완료 시점에 접속 안 해있던 팀원은 WebSocket 브로드캐스트도
  놓침). `TeamMembersResponse.status`(`Team.status.name()`)를 추가해 `GET
  /api/teams/{teamId}/members` 응답에 포함시켰다.
- **채팅방 목록 정렬 (2026-08-03, Figma 5.2 채팅방 설정 확인 후 추가)**: 카카오톡처럼 "최신
  메시지 순"/"안읽은 메시지 순" 두 가지로 정렬할 수 있어야 했다. `GET /api/teams`에 쿼리
  파라미터 `sort`(`ChatRoomSortType`: `LATEST`(기본값)/`UNREAD`)를 추가. `ChatRoomSummaryResponse`가
  이미 `lastMessageAt`/`unreadCount`를 갖고 있어서 별도 쿼리 없이 애플리케이션 레벨에서
  `Comparator`로 정렬한다(방 개수가 몇 개 안 되는 개인별 채팅방 목록이라 DB 정렬로 옮길
  필요는 없다고 판단). `LATEST`는 `lastMessageAt` 내림차순. `UNREAD`는 안읽은 메시지
  개수(`unreadCount`) **크기 순이 아니다** — 카카오톡 실제 동작을 확인해보니, 안읽은 방을
  개수와 무관하게 먼저 모아 보여주고(그 안에서는 최신 메시지 순), 다 읽은 방들은 그 뒤에
  역시 최신 메시지 순으로 이어붙이는 방식이었다. `unreadCount == 0` 여부(오름차순, 즉
  안읽은 방이 먼저) → `lastMessageAt` 내림차순 2단 정렬로 구현. 메시지가 한 번도 없던 방
  (`lastMessageAt = null`)은 두 정렬 기준 모두에서 항상 맨 뒤로 보낸다.
- ⚠️ **임시**: `domains/team/controller/TeamTestController.java` (`POST /api/test/teams`,
  `@Profile("local")`) — 원래 매칭 연동 전까지 수동 테스트(WebSocket 채팅 등)를 위해 `createTeam`을
  직접 호출할 수 있게 열어둔 개발용 엔드포인트. `local` 프로필을 명시적으로 켰을 때만 활성화되는
  fail-safe 방식(보안 검토 후 `!prod`에서 변경, 자세한 이유는 [api.md](../api.md) 참고).
  **(2026-08-05 갱신)** 매칭 도메인 연동(`MatchingGroupCompletionService`)이 이미 완료돼 삭제
  대상이지만, 로컬 수동 테스트 용도로 계속 쓰이고 있어 우선 남겨둠 — 위 "미정" 섹션 참고.

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

## 구현 현황 (2026-08-05 — 팀장 희망 데이터 배선 완료)

- 마이그레이션 V24: `team_members`에 `leader_preference`/`extroversion_type`/
  `extroversion_score` 컬럼 추가.
- `TeamRequest.TeamMemberInput`에 같은 3개 필드 추가. `MatchingGroupCompletionService`가
  `MatchingApplication`에서 값을 읽어 그대로 실어 보낸다.
- `TeamConverter.determineLeaderSelectionMode(List<TeamMemberInput>)` 신규 — WANTS 응답
  개수로 `leaderSelectionMode`를 실제로 판정(더 이상 `OPEN_NOMINATION` 하드코딩 아님).
  `TeamConverter.toTeamMember()`가 3개 필드를 스냅샷하고 `isPreLeaderCandidate`도
  `leaderPreference == WANTS` 기준으로 실제 계산한다.
- `TeamService.createTeam()`이 `AUTO_ASSIGNED`(WANTS 1명)인 경우 팀 생성 시점에 바로
  `TeamMember.assignAsLeader()`를 호출해 팀장을 확정한다.
- 자세한 케이스①②③ 분기 로직과 팀장 추천 알고리즘은 [02-leader-election.md](./02-leader-election.md)
  참고.
- 테스트: `TeamServiceTest`(AUTO_ASSIGNED/CANDIDATE_VOTE 신규 케이스),
  `MatchingGroupCompletionServiceTest`(TeamMemberInput 필드 전달 검증).

## 관련 화면

5.1 채팅목록, 5.1.2 팀원 프로필 열람(비공개 포함)
