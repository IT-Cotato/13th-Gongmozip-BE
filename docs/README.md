# 팀 채팅 / 매칭 이후 협업 기능 — 설계 문서

매칭이 완료된 팀이 채팅방에서 자기소개 → 팀장 선출 → 공모전 선정 → 진행 → 제출까지 진행하는
기능 전체의 설계 문서입니다. 백엔드(Spring Boot, `org.cotato.gongmozip`) 신규 도메인 설계 및
결정사항을 정리합니다.

## 문서 구조

- [requirements.md](./requirements.md) — 원본 요구사항 정리 + 화면 번호 매핑
- [api.md](./api.md) — 구현된 API 요약 (첫 버전, 정확한 스펙은 Swagger 기준)
- `decisions/` — 카테고리(도메인)별 설계 결정사항
  - [00-architecture.md](./decisions/00-architecture.md) — 전체 아키텍처, 패키지 구조, 기술 결정
  - [01-team.md](./decisions/01-team.md) — Team / TeamMember, 상태머신
  - [02-leader-election.md](./decisions/02-leader-election.md) — 팀장 선출 로직, LeaderVote
  - [03-chat.md](./decisions/03-chat.md) — Message, 챗봇 on/off, 나가기, 신고
  - [04-contest-voting.md](./decisions/04-contest-voting.md) — 공모전 후보/투표
  - [05-report.md](./decisions/05-report.md) — 사용자 신고
  - [06-collaboration-point.md](./decisions/06-collaboration-point.md) — 협업거리 포인트
  - [07-scheduler.md](./decisions/07-scheduler.md) — 중간점검/제출확인 배치 트리거
  - [08-ai.md](./decisions/08-ai.md) — 팀장 추천/동률 판단/공모전 카테고리 추천 AI 연동
  - [09-review.md](./decisions/09-review.md) — 팀원 리뷰

각 `decisions/*.md`는 아래 템플릿을 따릅니다.

```
## 배경/목적
## 엔티티 · 필드 정의
## 결정사항
## 미정 / 추후 확인 필요
## 관련 화면
```

## 전체 아키텍처 한줄 요약

`Team` = 채팅방(1:1). 별도 ChatRoom 엔티티 없이 Team이 곧 채팅방 aggregate root이며,
팀장 선출/공모전 투표/신고/리뷰는 전부 Team을 기준으로 스코프된다. 상세는
[00-architecture.md](./decisions/00-architecture.md) 참고.

## 개발 순서 (Phase)

- [x] Phase 0 — 마이그레이션 & 공통 enum/뼈대
- [x] Phase 1 — Team / TeamMember 생성·조회 (매칭 결과 반영)
- [x] Phase 2 — 채팅 기본 기능 (Message, 나가기, 신고, 챗봇 on/off)
- [x] Phase 3 — 협업거리 포인트 인프라 (CollaborationPointHistory)
- [x] Phase 4 — 챗봇 상태머신 골격 (TeamStatus 전이 엔진)
- [x] Phase 5 — 팀장 선출 플로우 (LeaderVote)
- [x] Phase 6 — 공모전 후보/투표 (ContestCandidate, ContestVote)
- [x] Phase 7 — 스케줄러 (중간점검 / 제출확인)
- [x] Phase 8 — AI 기능 연결 (팀장 추천, 동률 판단, 공모전 카테고리 추천)
- [x] Phase 9 — 팀원 리뷰

각 Phase 착수/완료 시 관련 `decisions/*.md`를 갱신한다.
