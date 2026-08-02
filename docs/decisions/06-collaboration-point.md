# 06. 협업거리 포인트

## 배경/목적

화면에 노출되는 "협업거리"는 프로젝트 진행 중 이벤트에 따라 적립/차감되는 게이미피케이션
포인트다. 매칭 신청 시점의 `MatchingApplication.collaborationDistance`(협업 희망 거리, 정적
값 — 매칭 신청 시 입력받아 스냅샷된 값)와 **이름은 같지만 완전히 다른 개념**이므로 절대 같은
컬럼을 재사용하지 않는다.

> 참고: 최초 설계 시 `PersonalityProfile.collaborationDistance`를 참조했으나, 해당 엔티티는
> 현재 `MatchingApplication`으로 재구성되었다. [01-team.md](./01-team.md)의 엔티티 참조 정정
> 참고.

## 엔티티 · 필드 정의

### CollaborationPointHistory

| 필드 | 설명 |
|---|---|
| member | FK |
| team | FK, nullable |
| delta | int (+/-) |
| reasonCode | `CollaborationPointReason` |
| createdAt | |

`Member` 또는 `Profile`에 캐시용 `collaborationPoint` 누적 컬럼을 두고, 히스토리 insert 시
같은 트랜잭션에서 갱신한다 (매번 SUM 집계하지 않기 위함).

### CollaborationPointReason 및 변화량

| reasonCode | delta | 트리거 |
|---|---|---|
| LEAVE_PENALTY | -10m | 채팅방 중도 이탈 |
| PROGRESS_CHECK_RESPONSE | +5m | 중간점검 응답 (팀장만 응답 가능) |
| PROJECT_COMPLETE_MEMBER | +20m | 프로젝트 완주 (팀원) |
| PROJECT_COMPLETE_LEADER | +30m | 프로젝트 완주 (팀장) |
| REVIEW_WRITTEN | +10m | 팀원 리뷰 작성 (Phase 9에서 연결 완료, [09-review.md](./09-review.md) 참고) |

## 결정사항

- 적립/차감을 기록하는 공용 서비스(`awardPoint(memberId, teamId, delta, reason)`)를 Phase 3에서
  먼저 만들고, 이후 각 트리거 지점(나가기, 중간점검 응답, 진행완료)에서 호출만 하도록 한다.
- `PersonalityProfile.collaborationDistance`는 절대 이 로직에서 수정하지 않는다 (매칭 알고리즘
  입력값 보존).

## 구현 현황 (Phase 3 완료)

- 엔티티/리포지토리: `domains/collaboration/entity/CollaborationPointHistory.java`,
  `CollaborationPointHistoryRepository.java`
- `CollaborationPointReason` enum에 사유별 delta(-10/+5/+20/+30/+10)를 내장해서, 호출부는
  `awardPoint(member, team, reason)`만 호출하면 됨 (delta를 직접 넘기지 않음 — 실수 방지)
- `Member`에 캐시 컬럼 `collaborationPoint`(기본 0) + `MAX_COLLABORATION_POINT=500` 상수 +
  `addCollaborationPoint(delta)` (0~500 클램핑) 추가
- `CollaborationPointService.awardPoint(...)` — 히스토리 저장 + Member 캐시 갱신을 한 트랜잭션에서 처리
- **`hasAwarded(member, team, reason)` 추가 (2026-08-02, CodeRabbit PR #61 리뷰 반영)**:
  `CollaborationPointHistoryRepository.existsByMember_MemberIdAndTeam_TeamIdAndReasonCode`로
  특정 사유의 지급 이력이 이미 있는지 확인한다. 팀원 이탈 재확인처럼 같은 지급 조건이 여러
  번 재평가될 수 있는 호출부(`ReviewService.recheckAfterMemberLeft`)에서, 지급 전에 먼저
  확인해 중복 지급을 막는 용도. 자세한 내용은 [09-review.md](./09-review.md) 참고.
- 연결된 트리거: `TeamService.leaveTeam()` → `LEAVE_PENALTY`
- **MyPage 연동**: `MyPageConverter.toMyPageMainResponse`가 하드코딩했던
  `CollaborationDistanceSummary(100, 500, 20)`를 실제 `member.getCollaborationPoint()` 기반
  계산으로 교체함 (기존에 "프로젝트 도메인 구현 후 연동" TODO로 남아있던 자리)
- ~~미연결 트리거~~ → 전부 연결 완료: `PROGRESS_CHECK_RESPONSE`/`PROJECT_COMPLETE_MEMBER`/
  `PROJECT_COMPLETE_LEADER`는 Phase 7([07-scheduler.md](./07-scheduler.md)), `REVIEW_WRITTEN`은
  Phase 9([09-review.md](./09-review.md))에서 각각 `collaborationPointService.awardPoint(...)`
  호출을 붙였다.
- 테스트: `CollaborationPointServiceTest`(적립/차감/클램핑), `TeamServiceTest`(나가기 시 호출 검증),
  `MyPageServiceTest` 갱신

## 미정 / 추후 확인 필요

- `ongoingProjectCount`/`completedProjectCount`(MyPage)는 이번 스코프에서 연결하지 않음 —
  `Team.status` 기반 매핑이 필요하며 Phase 6 이후 별도로 진행

## 관련 화면

5.1.2.4 채팅방 나가기, 5.1.3.5 중간점검, 팀원 리뷰 팝업, 팀장 리뷰 팝업
