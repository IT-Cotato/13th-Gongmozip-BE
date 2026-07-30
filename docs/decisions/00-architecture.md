# 00. 전체 아키텍처

## 배경/목적

매칭 완료 이후의 팀 협업 기능(채팅, 팀장 선출, 공모전 선정, 리뷰)을 어떤 도메인 구조로
쪼갤지 정의한다.

## 결정사항

- **Team = 채팅방.** 별도 `ChatRoom` 엔티티를 두지 않는다. 1:1 관계이고, 팀장선출/공모전투표/
  신고/리뷰가 전부 "이 팀"을 기준으로 상태가 바뀌므로 aggregate root를 하나로 통일한다.
- 신규 도메인 패키지(기존 `domains/{auth,member,profile,contest,survey}` 컨벤션 따름):
  - `team` — Team, TeamMember, TeamStatus, TeamRole, LeaderSelectionMode,
    LeaderCandidacyStatus, LeaderVote
  - `chat` — Message, MessageSenderType, MessageType
  - `report` — Report, ReportReason
  - `collaboration` — CollaborationPointHistory, CollaborationPointReason
  - `contest` 도메인 확장 — ContestCandidate, ContestVote (기존 Contest 엔티티 재사용)
- 기존 컨벤션 유지: `BaseEntity` 상속, `@Builder` + `@NoArgsConstructor(PROTECTED)` +
  `@AllArgsConstructor(PRIVATE)`, enum 컬럼은 `EnumType.STRING`, 다중값 리스트는 기존
  `StringListConverter`/`InterestCategoryListConverter` 패턴 재사용.
- 팀 생성은 "매칭" 기능의 산출물을 받는 것으로 가정한다. 매칭 알고리즘 자체는 이 문서 범위 밖.
  단, **매칭 도메인이 무엇을 넘겨줘야 하는지는 입력 계약으로 명시**한다 (→
  [01-team.md](./01-team.md)의 `TeamCreationRequest`). 매칭 그룹핑 로직의 구현 여부/시점과
  무관하게 이 계약만 만족하면 `team` 도메인은 동작한다.
- **survey/matching 도메인 현황 (2026-07-29 확인)**: `PersonalityProfile`은 더 이상 존재하지
  않고 `SurveySubmission`(설문 점수) + `MatchingApplication`(매칭 신청 시점 스냅샷)으로
  재구성되어 있다. `MatchingApplication`은 개인 단위 신청 기록일 뿐, 여러 명을 팀으로 묶는
  로직/엔티티는 아직 없다. 팀장 희망 점수 필드도 현재 없음 —
  [02-leader-election.md](./02-leader-election.md)의 데이터 갭 참고.

## 기술 결정 (미정 — Phase 진행 중 확정)

- [x] **메시지 전송/실시간 수신 (2026-07-29 확정, Phase 4 착수 전 전환)**: STOMP over WebSocket.
  Phase 2에서 REST로 먼저 만들었으나, Phase 4(챗봇 상태머신)부터는 시스템/챗봇 메시지가
  실시간으로 보여야 의미가 있어서 호출 지점이 적을 때(3곳) 미리 전환함. REST `POST /messages`는
  제거하고 STOMP `@MessageMapping("/teams/{teamId}/messages")`로 대체. 이력 조회(`GET`)와
  읽음 처리(`PATCH`)는 실시간성이 필요 없어 REST로 유지 (하이브리드 구조). 상세는
  [03-chat.md](./03-chat.md).
- [ ] **스케줄러**: Spring `@Scheduled` 배치로 충분한지, Quartz 등 별도 도구 필요한지

## 관련 화면

전체 화면 (5.1.x 계열)
