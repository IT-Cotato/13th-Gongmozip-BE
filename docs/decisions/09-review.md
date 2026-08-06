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
  > ⚠️ **포인트 중복 지급 버그 정정 (2026-08-02, PR #56 리뷰 반영)**: 기능명세서
  > 5.1.3.6.1은 "나머지 팀원 전체에 대한 리뷰를 최종 완료하는 시점"에 10m를 **1회만** 지급하는
  > 것으로 정의하는데, 최초 구현은 리뷰 1건을 쓸 때마다 매번 지급하고 있었다(4인 팀이면 리뷰
  > 3개로 30m 획득하는 버그). `ReviewRepository.countByTeam_TeamIdAndReviewer_TeamMemberIdAndReviewee_Status`를
  > 추가해, 리뷰어가 방금 쓴 리뷰가 본인이 써야 할 마지막 리뷰(=활성 팀원 수 - 1건)일 때만
  > 지급하도록 수정했다.
  > ⚠️ **이탈 팀원 관련 집계/재확인 버그 정정 (2026-08-02, CodeRabbit PR #61 리뷰 반영)**:
  > 세 가지를 더 고쳤다.
  > 1. 지급 여부를 세는 카운트 쿼리가 대상(reviewee)이 이미 나간 리뷰까지 포함하고 있었다
  >    (`AndReviewee_Status` 조건 없음). 예: 3인 팀에서 리뷰어가 나간 팀원을 포함해 2건을
  >    써뒀는데 그중 1명이 나가면, 남은 활성 대상은 1명뿐인데 카운트는 여전히 2로 잡혀
  >    엉뚱한 시점에 조건이 충족될 수 있었다. `Reviewee_Status = ACTIVE` 조건을 추가해 대상이
  >    현재도 활성인 리뷰만 세도록 정정.
  > 2. `recheckAfterMemberLeft`가 팀 완료 여부만 재확인하고, 남은 팀원 개개인의 포인트 지급
  >    여부는 재확인하지 않았다 — 어떤 팀원이 나가기 전에 이미 (당시 기준) 필요한 리뷰를 다
  >    써뒀다면, 그 사람이 새로 리뷰를 쓰지 않는 한 지급 조건이 다시 검사될 일이 없어 정당하게
  >    받아야 할 포인트를 영영 못 받는 경우가 있었다. `recheckAfterMemberLeft`가 남은 활성
  >    팀원 전원에 대해 지급 조건을 재확인하도록 확장. 여러 번 나가도 중복 지급되지 않도록
  >    `CollaborationPointService.hasAwarded(member, team, reason)`(신규,
  >    `CollaborationPointHistory` 존재 여부로 판정)로 지급 전 먼저 확인하는 가드를 추가했다.
  > 3. 활성 팀원이 나가서 1명 이하로 줄면(더 이상 서로 리뷰할 대상이 없음) 필요 리뷰 쌍이
  >    0건이 되는데, 기존 로직은 이 경우를 그냥 무시하고 반환해 팀이 영원히 `SUBMITTED`에
  >    머물렀다. `completeReviewIfAllDone`이 필요 쌍 0건도 완료로 간주해 `COMPLETED`로
  >    전이하도록 수정 — `writeReview` 경로에서는 리뷰어/대상이 항상 서로 다른 활성 팀원이라
  >    활성 인원이 2명 미만일 수 없으므로, 이 분기는 `recheckAfterMemberLeft`에서만 실행된다.
- 팀/멤버십 관련 예외는 기존 관례대로 별도 `ReviewErrorCode`를 두지 않고 `TeamErrorCode`
  (`TEAM_NOT_FOUND`, `NOT_TEAM_MEMBER`, `INVALID_TEAM_STATUS`)를 재사용. `ReviewErrorCode`는
  리뷰 도메인 고유 규칙(본인 리뷰 금지, 중복 리뷰 금지)만 갖는다.
- ~~리뷰 조회(읽기) API는 만들지 않았다~~ → 2026-08-02 정정. "쓰기 전용 이력성 도메인은 조회
  API를 굳이 만들지 않는다"는 기존 관례를 따랐던 결정이었는데, 실제 Figma 팀원 리뷰 팝업
  화면(5.1.3.6.1~6.5)을 확인해보니 이미 리뷰를 쓴 팀원은 회색으로 비활성화해서 보여줘야
  했다 — 조회 API 없이는 프론트가 이걸 판단할 방법이 없어서(중복 제출을 시도해봐야 에러로만
  알 수 있음) `GET /api/teams/{teamId}/reviews/targets`를 추가했다. 아래 "리뷰 대상 목록 조회"
  참고.

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
- **`leaveTeam` 중 리뷰 완료 재확인 (2026-08-02, PR #56 리뷰 반영)**: 리뷰 진행 중 팀원이
  나가면 남은 활성 팀원 기준으로 완료 조건이 뒤늦게 충족돼도 아무도 재확인하지 않는 버그가
  있었다. `ReviewService.recheckAfterMemberLeft(Team)`를 추가해 `TeamService.leaveTeam`이
  나가는 시점의 `Team.status == SUBMITTED`일 때 호출한다. 자세한 내용은
  [01-team.md](./01-team.md) 참고.
- **리뷰 대상 목록 조회 (2026-08-02, Figma 5.1.3.6.1~6.5 확인 후 추가)**: `GET
  /api/teams/{teamId}/reviews/targets` — 나를 제외한 활성 팀원 목록을, 내가 이미 리뷰를 쓴
  대상은 `alreadyReviewed=true`로 표시해서 반환한다(`ReviewResponse.ReviewTargetResponse`).
  팀/채팅 화면과 동일하게 아바타(`CharacterService.findAvatarsByMembers`)도 함께 채워준다.
  `ReviewRepository.findByTeam_TeamIdAndReviewer_TeamMemberId`(리뷰어 기준 작성 목록) 추가.
  팀 상태로 제한하지 않는다(아직 SUBMITTED 전이라 대상이 없거나, 이미 COMPLETED로 넘어가
  과거 이력을 보는 경우에도 조회 자체는 막을 이유가 없어서).

## 리뷰 콘텐츠 형식 확정 (2026-08-06, Figma 5.1.3.6.1~6.5 확인 후 자유 텍스트 폐기)

위 "미정" 항목("실제 와이어프레임 확인 후 리뷰 콘텐츠 형식이 추가로 필요하면 컬럼을 더
추가해야 함")으로 남겨뒀던 부분을 Figma 확인 후 확정했다. 실제 화면에는 자유 텍스트 입력창이
전혀 없고, 3점 척도 응답 2개 + 다중 선택 키워드로만 구성돼 있어 `Review.content`(자유
텍스트, 최대 1000자)를 완전히 폐기하고 구조화된 필드로 교체했다.

- 마이그레이션 V31 — `reviews.content` 삭제, `communication_score`/`participation_score`
  (`VARCHAR(20)`, 기본값 `'NEUTRAL'`), `keywords`(`TEXT`, 기본값 `'[]'`) 추가.
- `ReviewAgreementLevel`(`domains/review/enums`) — "소통이 원활하게 이루어졌나요?" /
  "프로젝트에 적극적으로 참여하였나요?" 두 질문에 공용으로 쓰는 3점 척도:
  `DISAGREE`(아니다) / `NEUTRAL`(보통이다) / `AGREE`(그렇다).
- `ReviewKeyword`(`domains/review/enums`) — 와이어프레임의 7개 고정 키워드 칩과 1:1 대응:
  `LEADERSHIP`(리더십이 있는 팀원), `GOOD_COMMUNICATOR`(소통이 잘되는 팀원),
  `CREATIVE`(아이디어가 좋은 팀원), `PROBLEM_SOLVER`(문제해결을 잘하는 팀원),
  `TRUSTWORTHY`(믿음직한 팀원), `PROACTIVE`(적극적인 팀원), `CONSIDERATE`(배려심 있는 팀원).
  화면에 상한 선택 개수가 명시돼 있지 않아 최소 1개만 강제한다(`@NotEmpty`).
- `Review.keywords`는 별도 조인 테이블 없이 `MatchingReason.commonPoints`와 동일하게
  `StringListConverter`(`domains/profile/entity`, 이미 여러 도메인이 재사용 중)로 JSON 문자열
  컬럼에 저장한다 — 엔티티에는 `List<String>`(`ReviewKeyword.name()` 값)으로, API
  요청/응답에는 `List<ReviewKeyword>`로 노출해 타입 안전성을 유지한다
  (`ReviewConverter.toKeywords`).
- `WriteReviewRequest`가 `content` 대신 `communicationScore`/`participationScore`/`keywords`
  3개 필드를 받도록 교체 — 완료 판정·포인트 지급 로직(`awardPointIfReviewerJustCompleted`,
  `completeReviewIfAllDone`)은 리뷰 존재 여부만으로 판정하므로 이번 변경과 무관하게 그대로
  동작한다.
- "나가기" 시 뜨는 "작성하신 내용은 임시저장됩니다" 안내는 서버 쪽 초안 저장 기능이 아니다 —
  `Review`는 `POST .../reviews` 성공 시에만 생성되는 완결된 레코드이고 부분 저장 개념이 없다.
  프론트가 화면을 벗어나기 전까지 로컬 상태로만 입력값을 들고 있다가, 같은 세션에서 해당
  팀원 탭으로 돌아오면 그 로컬 상태를 복원해주는 것으로 해석했다 — 기기/세션을 넘어선 서버
  영속화가 필요하다면 별도 확인 후 추가해야 한다.
- 테스트: `ReviewServiceTest` 기존 케이스를 새 요청 스키마로 갱신.

## 미정 / 추후 확인 필요

- ~~실제 와이어프레임 확인 후 리뷰 콘텐츠 형식(별점/태그 등)이 추가로 필요하면 `Review`에
  컬럼을 더 추가해야 함.~~ → 2026-08-06 확정, 위 "리뷰 콘텐츠 형식 확정" 참고.
- "임시저장" 안내가 실제로 서버 영속화를 의미하는지(기기/세션을 넘어선 초안 복원 등)는 아직
  프론트/기획 확인 전 — 위 "리뷰 콘텐츠 형식 확정"의 해석(로컬 상태로만 처리)이 틀리면
  별도 draft API가 필요할 수 있음.
- 리뷰를 받은 내역을 마이페이지 등에서 보여줄지 여부 — 필요해지면 조회 API를 새로 추가.

## 관련 화면

5.1.1.2.2 협업후기 작성, 팀원 리뷰 팝업, 팀장 리뷰 팝업, 5.1.3.6.1~6.5 팀원리뷰, 팀원리뷰 완료,
팀원리뷰 중단
