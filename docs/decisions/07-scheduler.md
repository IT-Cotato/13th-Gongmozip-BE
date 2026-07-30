# 07. 스케줄러 (중간점검 / 제출확인)

## 배경/목적

날짜 기반으로 챗봇이 먼저 말을 거는 두 이벤트(중간점검, 제출확인)의 트리거 타이밍과 배치
방식을 정의한다.

## 엔티티 · 필드 정의

`Team`에 아래 필드를 두고 `Team.contest`가 확정되는 시점(공모전 투표 완료)에 1회 계산한다.

| 필드 | 계산식 |
|---|---|
| progressCheckAt | `team.createdAt + (contest.applyEndAt - team.createdAt) / 2` |
| submissionCheckAt | `contest.applyEndAt - 1일` |

## 결정사항

- **중간점검**: 팀 생성일과 공모전 마감일의 중간 날짜에 진행률 체크 메시지 발송. 진행률 응답은
  팀장만 가능. 응답 시 `PROGRESS_CHECK_RESPONSE`(+5m) 지급.
- **제출확인**: 공모전 마감일 하루 전에 "진행 완료 / 미완료" 확인 메시지 발송. 버튼은 팀장에게만
  노출(타 팀원에게는 버튼 없이 안내만). "진행 완료" 선택 시 `Team.status = SUBMITTED` →
  팀원 리뷰 단계로 이동(리뷰는 보류), 완주 포인트 지급.
- 배치 방식은 Spring `@Scheduled` 일 1회 cron으로 시작 — `progressCheckAt`/`submissionCheckAt`이
  오늘 날짜이고 아직 메시지를 발송하지 않은 `Team`을 조회해 처리. 별도 Quartz 등은 불필요할
  것으로 예상 (팀 수가 아주 많아지면 재검토).

## 구현 현황 (Phase 7 완료)

- 마이그레이션 V15: `teams`에 `contest_candidate_deadline_at`, `progress_check_notified_at`,
  `progress_check_responded_at`, `submission_check_notified_at` 추가 (전부 멱등성/마감 시각
  추적용). MySQL에서 되는 다중 컬럼 `ADD COLUMN` 콤마 문법을 H2가 못 받아서, V14 때와 비슷하게
  컬럼당 별도 `ALTER TABLE` 문으로 분리함.
- **공모전 후보/투표 마감도 이번에 같이 정리함** (04-contest-voting.md의 미정 항목):
  `ChatbotOrchestrationService.advanceToContestSelecting`이 `CONTEST_SELECTING` 진입 시
  `contestCandidateDeadlineAt`을 "오늘 오후 11시"로 세팅. `TeamSchedulerJobs`가 5분마다
  마감 지난 `CONTEST_SELECTING` 팀을 찾아 `ContestVotingService.resolveDeadlineIfDue`를 호출:
  후보가 없으면 아무 것도 안 하고, 투표가 하나도 없었으면 후보 중 하나를 무작위로 골라 확정
  (Phase 8에서도 AI 추천으로 바꾸지 않기로 결정, [08-ai.md](./08-ai.md) 참고), 일부라도 투표가
  있었으면 평소와 동일한 개표 로직(단독1위/동률)을 그대로 적용.
- `progressCheckAt`/`submissionCheckAt` 계산은 `ContestVotingService.decideContest`에서
  공모전이 막 확정된 시점에 1회 세팅 (`Team.scheduleCheckpoints`).
- `TeamScheduleService`(도메인 로직) / `TeamSchedulerJobs`(`@Scheduled` cron 트리거)로 분리 —
  cron 배선과 실제 로직을 나눠서 로직 쪽만 순수 단위 테스트 가능하게 함. 공모전 마감은 5분
  간격, 중간점검/제출확인은 매일 09:00 (`SchedulingConfig`에 `@EnableScheduling` 추가).
- `TeamProgressService` — 중간점검 응답(`PATCH /api/teams/{teamId}/progress`)과 제출확인
  응답(`PATCH /api/teams/{teamId}/submission`) 둘 다 팀장만 가능. 진행률은 최초 응답 1회만
  포인트 지급(`progressCheckRespondedAt`으로 멱등성 보장), 제출 완료 시 팀장은
  `PROJECT_COMPLETE_LEADER`(+30m), 나머지 활성 팀원은 `PROJECT_COMPLETE_MEMBER`(+20m) 지급 후
  `Team.status = SUBMITTED`.
- 테스트: `TeamScheduleServiceTest`, `TeamProgressServiceTest`

## 미정 / 추후 확인 필요

- ~~`Team.status = SUBMITTED` 이후 "팀원 리뷰 단계로 이동"~~ → Phase 9에서 연결 완료.
  `SUBMITTED` 상태 자체가 리뷰 작성 창구이고, 전원이 서로 리뷰를 마치면 `COMPLETED`로
  자동 전이한다. 자세한 내용은 [09-review.md](./09-review.md) 참고.

## 관련 화면

5.1.3.5 중간점검, 5.1.3.6 공모전 마감 제출 여부 확인, 제출 여부 미진행시
