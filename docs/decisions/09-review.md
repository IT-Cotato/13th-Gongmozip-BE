# 09. 팀원 리뷰

## 배경/목적

프로젝트 종료(`Team.status = SUBMITTED`) 이후 팀원들이 서로에게 협업 후기를 남기는 기능.
이번 세션 전까지는 스코프에서 보류돼 있었다 (`Team.status.COMPLETED`, `CollaborationPointReason.REVIEW_WRITTEN`만 자리를 잡아둔 상태).

## 엔티티 · 필드 정의

### Review

| 필드 | 설명 |
|---|---|
| team | FK |
| reviewer | 작성자 `TeamMember` FK |
| reviewee | 대상 `TeamMember` FK |
| content | 자유 텍스트, 최대 1000자 |

unique(team_id, reviewer_team_member_id, reviewee_team_member_id) — 같은 팀에서 같은 대상에게
중복 작성 불가.

## 결정사항

- **작성 가능 시점**: `Team.status == SUBMITTED`일 때만. 별도의 `REVIEWING` 상태를 새로 만들지
  않고 기존 `SUBMITTED` 상태 자체를 리뷰 작성 창구로 쓴다 (요구사항 문서의 "제출 완료 시
  팀원 리뷰 단계로 이동"이 `TeamStatus`에 새 값을 추가하라는 뜻은 아니라고 판단).
- **범위**: 활성 팀원 전원이 자기 자신을 제외한 서로를 각각 1회씩 리뷰할 수 있는 완전
  peer-to-peer 구조. "팀원 리뷰 팝업"과 "팀장 리뷰 팝업"이 화면상 별개로 존재하지만, 백엔드
  데이터 구조는 동일하다고 보고 하나의 `Review` 엔티티로 처리한다 — 팀장이냐 아니냐는
  `reviewee.role`로 프론트가 판단해 팝업 문구만 다르게 보여주면 된다.
- **내용 형식**: 이번 구현은 자유 텍스트 한 줄만 받는다. 실제 와이어프레임에 별점/태그 선택
  같은 세부 UI가 있다면 그건 추후 필드 추가로 대응 — 현재는 스펙 확인 전이라 임의로 만들지
  않았다 (사용자 확인: 완료 조건만 결정, 리뷰 콘텐츠 형식은 논의 안 됨).
- **완료 조건 (사용자 결정, 2026-07-30)**: 활성 팀원 전원이 서로를 다 리뷰하면(=
  `n * (n-1)`개 리뷰가 모두 쌓이면) 그 즉시 `Team.status = COMPLETED`로 자동 전이. 시간 기반
  마감(Phase 7 스케줄러 패턴)은 채택하지 않음 — 아무도 리뷰를 안 쓰면 팀은 계속 `SUBMITTED`에
  머무르지만, 이 상태에 의존하는 다른 기능이 없어 문제 없음.
- `COMPLETED` 전이는 `ChatbotOrchestrationService.completeReview(Team)`가 담당 (다른 상태
  전이들과 동일하게 챗봇 오케스트레이션 서비스가 소유).
- 리뷰 작성 시 `CollaborationPointService.awardPoint(reviewer.member, team, REVIEW_WRITTEN)`
  (+10m) — Phase 3에서 이미 만들어둔 enum 값을 그대로 사용.
- 팀/멤버십 관련 예외는 기존 관례대로 별도 `ReviewErrorCode`를 두지 않고 `TeamErrorCode`
  (`TEAM_NOT_FOUND`, `NOT_TEAM_MEMBER`, `INVALID_TEAM_STATUS`)를 재사용. `ReviewErrorCode`는
  리뷰 도메인 고유 규칙(본인 리뷰 금지, 중복 리뷰 금지)만 갖는다.
- 리뷰 조회(읽기) API는 만들지 않았다 — `CollaborationPointHistory`와 동일하게 이 프로젝트의
  기존 관례가 "쓰기 전용 이력성 도메인은 조회 API를 굳이 만들지 않는다"였고, 요구사항에도
  리뷰 목록 조회 화면이 명시되지 않아 범위를 넓히지 않았다.

## 구현 현황 (Phase 9 완료)

- 마이그레이션 V16: `reviews` 테이블 생성.
- 엔티티/리포지토리: `domains/review/entity/Review.java`, `ReviewRepository`
  (`existsByTeam_TeamIdAndReviewer_TeamMemberIdAndReviewee_TeamMemberId`,
  `countByTeam_TeamIdAndReviewer_StatusAndReviewee_Status`로 완료 여부 판정).
- 서비스: `domains/review/service/ReviewService.writeReview(teamId, reviewerMemberId, request)`
  — 상태/본인/중복 검증 → 저장 → 포인트 지급 → 전원 완료 시 `completeReview` 호출까지 한 번에.
- `ChatbotOrchestrationService.completeReview(Team)` 추가 — `COMPLETED` 전이 + 마무리 챗봇 메시지.
- 컨트롤러: `domains/review/controller/ReviewController` — `POST /api/teams/{teamId}/reviews`.
- 테스트: `ReviewServiceTest`(7개 케이스), `ChatbotOrchestrationServiceTest`(completeReview 1개 추가).

## 미정 / 추후 확인 필요

- 실제 와이어프레임 확인 후 리뷰 콘텐츠 형식(별점/태그 등)이 추가로 필요하면 `Review`에
  컬럼을 더 추가해야 함.
- 리뷰를 받은 내역을 마이페이지 등에서 보여줄지 여부 — 필요해지면 조회 API를 새로 추가.

## 관련 화면

5.1.1.2.2 협업후기 작성, 팀원 리뷰 팝업, 팀장 리뷰 팝업
