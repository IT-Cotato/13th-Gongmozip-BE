# 07. 스케줄러 (중간점검 / 제출확인 / 인사 유도 타임아웃 / 팀장 선출 마감)

## 배경/목적

날짜/시간 기반으로 챗봇이 먼저 말을 거는 이벤트(중간점검, 제출확인, 인사 유도 타임아웃)의
트리거 타이밍과 배치 방식을 정의한다.

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
- 테스트: `TeamScheduleServiceTest`, `TeamProgressServiceTest`, `TeamSchedulerJobsTest`
- **트랜잭션 분리 (2026-08-01)**: 처음엔 `TeamScheduleService`의 3개
  메서드(`resolveDueContestVotingDeadlines`/`sendDueProgressChecks`/`sendDueSubmissionChecks`)가
  대상 팀 전체를 하나의 `@Transactional` 안에서 for 루프로 처리했다 — 팀이 많아지면 그만큼
  커넥션을 오래 점유하고, 루프 중 한 팀에서 예외가 나면 이미 처리된 다른 팀들까지 롤백되는
  문제가 있었다. "대상 팀 id 조회"(`findDue...TeamIds`, 논트랜잭션)와 "팀 1개 처리"
  (`resolve.../send...ForTeam`, 팀 단위 `@Transactional`)로 나누고, `TeamSchedulerJobs`가
  조회 결과를 순회하며 팀마다 별도 트랜잭션으로 처리하도록 바꿨다. 팀 하나가 실패해도 나머지
  팀은 계속 처리되도록 `TeamSchedulerJobs`에서 팀 단위로 try-catch도 추가.

- **인사 유도 2시간 타임아웃 (2026-08-02, PR #56 리뷰 반영)**: 기능명세서 5.1.3.1 E1은 챗봇의
  자기소개 안내 후 2시간 동안 응답이 없으면 자동으로 다음 단계(팀장 선출)로 넘어가야 하는데,
  최초 구현은 팀원 전원의 응답만 계속 기다리는 구조였다(스케줄러 없음). `TeamRepository`에
  `findByStatusAndCreatedAtLessThanEqual` 추가 — `GREETING` 진입이 `TeamService.createTeam`
  직후 딱 한 번, 동기적으로만 일어나므로 `Team.createdAt`을 "GREETING 시작 시각"의 대용으로
  써도 안전하다(재진입 경로 없음). `TeamScheduleService.findDueGreetingTimeoutTeamIds`/
  `forceAdvanceGreetingForTeam`을 다른 3개 이벤트와 동일한 조회/처리 분리 패턴으로 추가하고,
  `TeamSchedulerJobs`에 5분 간격 cron을 하나 더 추가했다.
  `ChatbotOrchestrationService.recordGreetingAndAdvance`의 "전원 인사 완료 시 다음 단계로"
  로직을 `advanceToLeaderSelecting(team, activeMembers)`로 추출해, 정상 경로(전원 응답)와
  타임아웃 강제 경로(`forceAdvanceGreetingIfDue`) 둘 다 재사용한다.

- **팀장 선출 마감 (2026-08-05)**: 팀장 여부 투표/팀장 투표 단계도 공모전 투표와 동일하게
  마감 없이는 무한정 `LEADER_SELECTING`에 머무를 수 있는 문제가 있었다. `Team.leaderSelectionDeadlineAt`
  (마이그레이션 V25)을 추가하고 공모전과 동일한 조회/처리 분리·5분 간격 cron 패턴을 그대로
  적용했다. 상세 로직(하위 단계 판정, 마감 시 처리 방식)은 [02-leader-election.md](./02-leader-election.md)
  참고.

- **GREETING 상태 전이 동시성 보호 (2026-08-06, PR #61 CodeRabbit 리뷰에서 발견 → 이슈 #62로
  분리 후 처리)**: `forceAdvanceGreetingIfDue`(스케줄러, 5분 간격)와 `recordGreetingAndAdvance`
  (팀원 메시지 트리거)가 거의 동시에 같은 팀의 `TeamStatus.GREETING` 조건을 통과하면
  `advanceAfterGreeting`이 두 번 실행돼 AI 추천 호출과 `LEADER_NOMINATION_CARD` 메시지가
  중복 발행될 수 있었다. `Team`에 `@Version` 낙관적 잠금(마이그레이션 V28, `version` 컬럼)을
  추가해 나중에 커밋을 시도하는 트랜잭션이 `ObjectOptimisticLockingFailureException`을
  받도록 했다. `ChatbotOrchestrationService` 자체는 손대지 않음 — 트랜잭션 커밋 시점에 JPA가
  자동으로 감지하므로 애플리케이션 코드에서 별도로 잠글 필요가 없다. `TeamSchedulerJobs.
  forceAdvanceGreetings`만 이 예외를 별도로 잡아 `error`가 아닌 `warn`으로 로깅하고(정상적인
  동시성 충돌이지 실제 장애가 아니므로) 해당 팀만 건너뛴 뒤 다음 팀 처리를 계속한다 — 스킵된
  팀은 다음 스케줄러 주기에 재조회했을 때 이미 `GREETING`이 아니면 자연히 대상에서 빠진다.
  테스트: `TeamOptimisticLockingIntegrationTest`(실제 두 트랜잭션으로 버전 충돌 재현),
  `TeamSchedulerJobsTest`(낙관적 잠금 충돌 시에도 나머지 팀 처리가 계속됨을 검증).

## 제출 여부 확인 재알림 (2026-08-06, Figma "제출 여부 미진행시" 커버리지 점검 중 발견)

Figma 목업에 "제출 여부 미진행시" 화면이 관련 화면으로 명시돼 있었는데도, 기존 구현은
`submissionCheckNotifiedAt`을 "최초 1회 발송" 멱등성 플래그로만 썼다 — 팀장이 "미완료"를
누르거나 아예 응답하지 않아도 다시 알려주는 로직이 전혀 없어서, 최초 카드를 놓치면 팀이
영원히 `IN_PROGRESS`에 머물 수 있었다.

- `Team.submissionCheckReminderAt`(마이그레이션 V30) — "다음 재알림을 언제 보낼지"를 담는
  컬럼. 최초 발송(`sendSubmissionCheckForTeam`), 팀장의 "미완료" 응답
  (`TeamProgressService.submitCompletion`), 재알림 발송 자체(`sendSubmissionCheckReminderForTeam`)
  세 지점 모두 이 값을 `now + 2시간`으로 계속 미룬다 — GREETING/팀장 선출 타임아웃과 동일한
  2시간 간격.
- `TeamScheduleService.findDueSubmissionCheckReminderTeamIds()` /
  `sendSubmissionCheckReminderForTeam(teamId)` — `IN_PROGRESS`이고 재알림 시각이 지난 팀에게
  더 짧은 문구("프로젝트가 진행완료되었으면, 진행완료 버튼을 눌러주세요.")로 같은
  `MessageType.SUBMISSION_CHECK_CARD`를 재발행한다. 최초 카드와 버튼 동작(미완료/진행 완료,
  `PATCH /api/teams/{teamId}/submission`)이 동일해 새 `MessageType`을 만들 필요는 없었다.
  `TeamSchedulerJobs.sendSubmissionCheckReminders()`가 5분 간격으로 확인.
- "진행 완료"로 상태가 `SUBMITTED`가 되면 스케줄러 조회 자체가 `status = IN_PROGRESS` 조건으로
  걸러지므로 자연히 멈춘다 — `submissionCheckReminderAt`을 명시적으로 `null`로 지우지 않는다
  (`leaderSelectionDeadlineAt`과 동일한 이유, 데이터 정리 문제일 뿐 기능 영향 없음).
- 테스트: `TeamProgressServiceTest`, `TeamScheduleServiceTest`, `TeamSchedulerJobsTest`.

> ⚠️ **race/인덱스 보완 (2026-08-06, CodeRabbit 리뷰 반영)**: 두 가지를 추가로 고쳤다.
> 1. `sendSubmissionCheckReminderForTeam`이 재알림 시각 존재 여부만 보고 발송했다 — 대상 id
>    조회 이후 팀장이 "미완료"로 다시 응답해 재알림 시각이 미래로 갱신됐어도 이를 무시하고
>    즉시 중복 카드를 발행할 수 있었다. 발송 직전에 재알림 시각이 실제로 지났는지(`isAfter(now)`
>    가 아닌지) 다시 검증하도록 수정.
> 2. `findDueSubmissionCheckReminderTeamIds()`가 5분마다 `status`/`submission_check_reminder_at`
>    으로 스캔하는데 `teams`에 해당 인덱스가 없었다 — V30에 복합 인덱스
>    (`idx_teams_status_submission_check_reminder_at`)를 추가했다.

## 미정 / 추후 확인 필요

- ~~`Team.status = SUBMITTED` 이후 "팀원 리뷰 단계로 이동"~~ → Phase 9에서 연결 완료.
  `SUBMITTED` 상태 자체가 리뷰 작성 창구이고, 전원이 서로 리뷰를 마치면 `COMPLETED`로
  자동 전이한다. 자세한 내용은 [09-review.md](./09-review.md) 참고.

## 관련 화면

5.1.3.5 중간점검, 5.1.3.6 공모전 마감 제출 여부 확인, 제출 여부 미진행시
