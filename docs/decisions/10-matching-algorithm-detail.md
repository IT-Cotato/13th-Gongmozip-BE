# 10. 매칭 알고리즘 상세 설계 및 구현 지도

## 문서 목적과 상태

이 문서는 매칭 신청부터 14시 배치 실행, 팀 구성, 결과 저장·공개·응답과 실제 Team 확정까지의 정책과 구현을 한곳에서 설명한다.
다른 설계 문서를 먼저 읽지 않아도 이해할 수 있도록 필요한 배경, 계산식, 실행 순서와 검증 방법을 모두 포함한다.

> **기준일 (2026-08-05)**: 이 문서는 현재 구현을 설명하는 설계·검증 문서다.
> 각 `구현 코드` 링크는 현재 파일의 실제 라인을 가리킨다. 리팩터링으로 줄 번호가 달라지면 링크 옆의 클래스와 메서드 이름을 기준으로 찾는다.

이번 범위에 포함되는 것은 다음과 같다.

- 저장된 프로젝트 AI 평가를 이용한 역량 점수
- 카테고리별 백분위 그룹과 소규모 그룹 병합
- 4인 우선·3인 보완 팀 크기 계획
- 3인·4인 팀 궁합 점수
- Brute Force와 Multi-start Greedy 혼합 방식
- 14시 배치, 결과 저장, 16시 공개 시각
- 16시 이후 본인의 팀·팀원·궁합 점수 조회
- 결과 수락·패스, 다음 날 12시 응답 마감과 패널티
- 수락 인원에 따른 실제 Team 생성과 피해 사용자 자동 재배정
- JDBC ShedLock과 배치 유니크 제약

앱 내 알림과 알림 전달 인프라는 포함하지 않는다. 결과 조회·응답·그룹 정리·실제 Team 생성과
자동 재배정까지는 알림 없이도 백엔드 상태 전이가 완결되도록 구현되어 있다.

## 문서 읽는 순서

처음 읽는다면 아래 순서가 가장 이해하기 쉽다.

1. **1장**에서 신청자가 14시 배치를 거쳐 결과로 저장되는 전체 흐름을 본다.
2. **2~4장**에서 개인 역량 점수, 풀 분할, 팀 크기 결정 규칙을 본다.
3. **5~7장**에서 알고리즘 입력·출력, 팀 궁합 점수, 최종 계획 비교 기준을 본다.
4. **8~10장**에서 Brute Force, Multi-start Greedy, fallback을 본다.
5. **11~12장**에서 트랜잭션, DB 저장, 결과 조회, 스케줄러 중복 실행 방지를 본다.
6. **13장**의 테스트를 실행해 문서와 구현이 일치하는지 확인한다.

코드를 따라갈 때는 모든 클래스를 한꺼번에 읽지 말고 아래 `시작 라인`부터 메서드 호출을 따라간다.

## 구현 코드 빠른 지도

| 단계 | 책임 | 시작 라인 |
|---|---|---|
| 신청 스냅샷 | 선택 프로필 참조와 신청 시점의 설문·역량 점수를 `MatchingApplication`에 저장 | [`MatchingApplicationService.apply()`](../../src/main/java/org/cotato/gongmozip/domains/matching/service/MatchingApplicationService.java#L139-L193) |
| 프로젝트 점수 | 저장된 프로젝트 AI 평가의 완료 여부 확인 및 평균 계산 | [`StoredProjectScoreProvider.evaluate()`](../../src/main/java/org/cotato/gongmozip/domains/matching/score/StoredProjectScoreProvider.java#L32-L54) |
| 역량 점수 | 학점·프로젝트·수상·자격증·협업거리 가중 합산 | [`SkillScoreCalculator.calculate()`](../../src/main/java/org/cotato/gongmozip/domains/matching/score/SkillScoreCalculator.java#L31-L73) |
| 14시 진입점 | 매일 14시에 ShedLock을 획득하고 배치 오케스트레이터 호출 | [`MatchingSchedulerJobs.runDailyMatching()`](../../src/main/java/org/cotato/gongmozip/domains/scheduler/MatchingSchedulerJobs.java#L21-L27) |
| 전체 지휘 | 풀 준비 → 배치 선점 → 알고리즘 실행 → 성공·실패 저장 | [`MatchingBatchOrchestrator.runDaily()`](../../src/main/java/org/cotato/gongmozip/domains/matching/service/MatchingBatchOrchestrator.java#L36-L98) |
| 풀 준비 | WAITING 신청 잠금, 카테고리 분리, 배치 생성 및 신청 연결 | [`MatchingPoolPreparationService.prepare()`](../../src/main/java/org/cotato/gongmozip/domains/matching/service/MatchingPoolPreparationService.java#L40-L68) |
| 4분할·병합 | 12명 미만 단일 풀, 12~23명 병합, 24명 이상 4분위 | [`MatchingPoolPartitioner.partition()`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/MatchingPoolPartitioner.java#L33-L57) |
| 팀 크기 | 배정 인원을 최대화하고 동률이면 4인 팀 최대화 | [`TeamSizePlanner.planSizes()`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/TeamSizePlanner.java#L18-L51) |
| 배치 선점 | 실행 가능한 배치를 잠그고 팀 크기와 입력 스냅샷 확정 | [`MatchingBatchClaimService.claim()`](../../src/main/java/org/cotato/gongmozip/domains/matching/service/MatchingBatchClaimService.java#L38-L66) |
| 팀 점수 | 리더·협업 성향·성격·외향성으로 0~100점 계산 | [`TeamCompatibilityCalculator.calculate()`](../../src/main/java/org/cotato/gongmozip/domains/matching/score/TeamCompatibilityCalculator.java#L28-L65) |
| 계획 비교 | 배정 수, 재배정 우선, 평균 점수 등의 사전식 비교 | [`MatchingPlanComparator.compare()`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/MatchingPlanComparator.java#L16-L43) |
| 알고리즘 선택 | 풀 크기에 따른 알고리즘 선택과 timeout fallback | [`MatchingAlgorithmSelector.match()`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/MatchingAlgorithmSelector.java#L27-L55) |
| 완전탐색 | 가능한 팀 조합을 탐색하고 최선의 계획 선택 | [`BruteForceMatchingAlgorithm.match()`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/BruteForceMatchingAlgorithm.java#L40-L64) |
| 휴리스틱 탐색 | 여러 시작 순서로 Greedy를 실행하고 교환 개선 | [`MultiStartGreedyMatchingAlgorithm.match()`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/MultiStartGreedyMatchingAlgorithm.java#L42-L82) |
| 결과 저장 | 팀·팀원 저장, 신청 상태 변경, 입력 전체 포함 검증 | [`MatchingResultPersistenceService.persistSuccess()`](../../src/main/java/org/cotato/gongmozip/domains/matching/service/MatchingResultPersistenceService.java#L46-L74) |
| 결과 조회 API | 인증 회원의 오늘 결과 조회 요청을 읽기 서비스로 전달 | [`MatchingResultController.getTodayResult()`](../../src/main/java/org/cotato/gongmozip/domains/matching/controller/MatchingResultController.java#L40-L45) |
| 결과 조회 정책 | 오늘 신청을 우선하고 열린·확정된 이전 결과를 fallback으로 조회 | [`MatchingResultQueryService.getTodayResult()`](../../src/main/java/org/cotato/gongmozip/domains/matching/service/MatchingResultQueryService.java#L35-L128) |
| 결과 응답 | 공개된 제안 결과의 수락·패스와 그룹 상태 전이 | [`MatchingResponseService`](../../src/main/java/org/cotato/gongmozip/domains/matching/service/MatchingResponseService.java#L48-L151) |
| Team 확정 | 유효 수락자 3명 또는 4명으로 실제 Team 생성 | [`MatchingGroupCompletionService.completeIfReady()`](../../src/main/java/org/cotato/gongmozip/domains/matching/service/MatchingGroupCompletionService.java#L31-L74) |
| 응답 마감 | 마감된 미응답자를 처리하고 Team 확정 또는 재배정 | [`MatchingResponseDeadlineService.processGroup()`](../../src/main/java/org/cotato/gongmozip/domains/matching/service/MatchingResponseDeadlineService.java#L51-L116) |
| DB 구조 | 배치·신청 연결·유니크 제약·ShedLock 테이블 | [`V21__implement_matching_batch.sql`](../../src/main/resources/db/migration/V21__implement_matching_batch.sql#L3-L91) |
| 동적 풀·3인 팀 | 분위 범위와 그룹 모드, 3·4인 팀 제약 | [`V22__support_dynamic_matching_pools_and_three_person_teams.sql`](../../src/main/resources/db/migration/V22__support_dynamic_matching_pools_and_three_person_teams.sql#L1-L21) |
| 응답·재배정 스키마 | 응답 마감·Team 연결·응답 출처·재배정 이력 | [`V23__implement_matching_responses_and_reassignment.sql`](../../src/main/resources/db/migration/V23__implement_matching_responses_and_reassignment.sql#L1-L93) |

## 1. 전체 처리 흐름

> **구현 코드:** [`MatchingSchedulerJobs.runDailyMatching()`](../../src/main/java/org/cotato/gongmozip/domains/scheduler/MatchingSchedulerJobs.java#L21-L27) → [`MatchingBatchOrchestrator.runDaily()`](../../src/main/java/org/cotato/gongmozip/domains/matching/service/MatchingBatchOrchestrator.java#L36-L98) 순서로 시작한다.

```text
14:00 당일 WAITING 신청 마감 (결과 공개 전까지 신청 불가)
  ↓
신청 당시 skillScore·성향·협업방식 스냅샷 고정
  ↓
6개 공모전 카테고리로 분리
  ↓
카테고리별 인원수 확인
  ├─ 24명 이상: 백분위 4개 그룹
  ├─ 12~23명: 백분위 4개 그룹 → 6명 미만 그룹 반복 병합
  └─ 12명 미만: 카테고리 단일 그룹
  ↓
각 유효 풀의 팀 크기 목록 결정
  → 배정 인원 최대화
  → 동률이면 4인 팀 최대화
  ↓
유효 풀 인원으로 알고리즘 선택
  ├─ 임계값 이하: Brute Force
  └─ 임계값 초과: Multi-start Greedy
  ↓
Brute Force 시간 초과 시 전체 입력으로 Greedy fallback
  ↓
3·4인 MatchingGroup, 원본 신청, 미배정 결과 저장
  → 제안 그룹이 저장되면 패스는 공개 전후와 관계없이 가능
  ↓
15:30 새 풀 시작 중단 기준(best effort)
  ↓
16:00 결과 공개·수락 가능, 다음 날 매칭 신청 접수 시작
  ↓
GET /api/matching/results/me/today로 본인 결과 조회
  ↓
각 그룹원이 다음 날 12:00 전까지 수락 또는 패스
  ├─ 유효 인원 3명 또는 4명이 모두 수락: 실제 Team 생성, 그룹 CONFIRMED
  ├─ 패스로 유효 인원이 3명 미만: 그룹 CANCELED, 피해자 자동 재배정
  └─ 12:00 미응답: 패널티 적용 후 Team 확정 또는 그룹 EXPIRED·피해자 재배정
```

카테고리가 다른 신청자는 어떤 예외에서도 섞지 않는다.

신청 접수는 하루 종일 닫히지 않고 매칭 진행 구간(14:00~결과 공개 전)에만 막는다. 결과
공개(16:00)부터는 신청이 다음 날 매칭으로 접수되며 `application_date`에 다음 날이 저장된다.
자격/신청 응답의 `applicationDate`가 지금 신청하면 참여하게 되는 매칭 날짜를 알려준다. 취소나
패스 여부와 무관하게 같은 신청 대상일에는 한 번만 신청할 수 있다.

## 2. 역량 점수와 카테고리 분류

> **구현 코드:** 신청 스냅샷은 [`MatchingApplicationService.apply()`](../../src/main/java/org/cotato/gongmozip/domains/matching/service/MatchingApplicationService.java#L139-L193), 점수식은 [`SkillScoreCalculator.calculate()`](../../src/main/java/org/cotato/gongmozip/domains/matching/score/SkillScoreCalculator.java#L31-L73), 프로젝트 평가 검증은 [`StoredProjectScoreProvider.evaluate()`](../../src/main/java/org/cotato/gongmozip/domains/matching/score/StoredProjectScoreProvider.java#L32-L54)에 있다.

### 2.1 카테고리

신청자는 다음 중 하나를 선택한다.

| 순서 | 카테고리 |
|---:|---|
| 1 | IT/AI/기술 |
| 2 | 마케팅/광고/브랜딩 |
| 3 | 아이디어/기획 |
| 4 | 미술/디자인 |
| 5 | 사진/영상 |
| 6 | 데이터 분석 |

카테고리는 1차 분리 기준이며, 이후 백분위 계산도 카테고리마다 독립적으로 수행한다.

### 2.2 역량 점수

각 항목은 0~100 원점수로 환산한 뒤 가중합한다.

| 항목 | 기존 사용자 | 첫 매칭 사용자 |
|---|---:|---:|
| 학점 | 20% | 20% |
| 프로젝트 AI 평가 | 40% | 50% |
| 수상 경험 | 10% | 10% |
| 자격증 개수 | 10% | 10% |
| 협업거리 | 20% | 10% |

```text
학점 원점수 = 학점 / 학점 만점 × 100

수상·자격증 원점수
  0개 = 0
  1개 = 50
  2개 = 60
  3개 = 70
  4개 = 80
  5개 = 90
  6개 이상 = 100

협업거리 원점수 = min(협업거리, 500) / 500 × 100
```

협업거리 가중점수만 보면 `협업거리(m) / 25`와 같아 기존 사용자는 100m=4점, 200m=8점,
500m=20점이다. 첫 매칭 사용자는 같은 원점수에 10%를 적용한다.

프로젝트가 없으면 프로젝트 원점수는 0점이다. 하나 이상이면 선택한 프로필의 모든 프로젝트가
`COMPLETED` 평가를 가져야 하며, 완료 점수의 산술평균을 소수 둘째 자리 `HALF_UP`으로 반올림한다.
평가가 없거나 `PENDING/PROCESSING/FAILED/OUTDATED`인 프로젝트가 하나라도 있으면 신청을 제한한다.
신청 트랜잭션에서는 외부 AI를 호출하지 않는다.

### 2.3 신청 스냅샷과 그룹 결정 시점

신청 행에는 최종 `skillScore`를 저장한다. 실제 역량 그룹은 한 사람만 보고 신청 시점에 정할 수
없으므로 14시 배치가 같은 카테고리 신청자를 모두 조회한 뒤 정한다.

- 기존 `MatchingApplication.skillGroup`은 레거시 호환 필드다.
- 현재 배치의 풀 판정에는 신청 시점 `skillGroup`을 사용하지 않는다.
- 처리된 유효 풀은 `MatchingApplication.matchingBatch`로 추적한다.

## 3. 백분위 그룹과 병합

> **구현 코드:** 진입점은 [`MatchingPoolPartitioner.partition()`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/MatchingPoolPartitioner.java#L33-L57), 4분할은 [`createQuartiles()`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/MatchingPoolPartitioner.java#L65-L78), 6명 미만 병합은 [`mergeUndersized()`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/MatchingPoolPartitioner.java#L85-L104)에 있다.

### 3.1 카테고리 인원별 처리

| 카테고리 총인원 | 백분위 | 병합 | 최종 유효 풀 |
|---:|---|---|---|
| 24명 이상 | 적용 | 미적용 | 4개 |
| 12~23명 | 적용 | 6명 미만 그룹 반복 병합 | 1~3개 (`23명 → 6·6·11`) |
| 12명 미만 | 미적용 | 미적용 | 카테고리 단일 풀 1개 |
| 0명 | 미적용 | 미적용 | 없음 |

24명 이상이면 균등 4등분의 각 그룹이 최소 6명이므로 병합하지 않는다. 12명 미만은 역량 그룹을
나누지 않으며, 카테고리를 넘어 인원을 보충하지 않는다.

### 3.2 결정적 정렬

> 정렬 기준 구현: [`CANDIDATE_ORDER`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/MatchingPoolPartitioner.java#L28-L31)

카테고리 안에서 다음 순서로 정렬한다.

```text
skillScore ASC
appliedAt ASC
matchingApplicationId ASC
```

동점자도 인원 균등 배치를 위해 서로 다른 분위에 들어갈 수 있다. 신청 시각과 ID는 같은 입력에서
같은 결과를 보장하는 tie-breaker이며 점수 자체를 변경하지 않는다.

### 3.3 균등 4등분

정렬된 신청 수를 `n`이라고 한다.

```text
base = floor(n / 4)
remainder = n mod 4

Q1..Q(remainder) 크기 = base + 1
나머지 Q 크기 = base
```

Q1이 가장 낮은 점수 분위, Q4가 가장 높은 점수 분위다. 신청자를 정렬 순서대로 연속 배치한다.

예시:

| n | Q1 | Q2 | Q3 | Q4 |
|---:|---:|---:|---:|---:|
| 12 | 3 | 3 | 3 | 3 |
| 13 | 4 | 3 | 3 | 3 |
| 20 | 5 | 5 | 5 | 5 |
| 21 | 6 | 5 | 5 | 5 |
| 23 | 6 | 6 | 6 | 5 |
| 24 | 6 | 6 | 6 | 6 |

#### 균등 분할 선택 이유

- 고정 점수구간에서 발생하던 특정 그룹 쏠림을 줄인다.
- 같은 점수를 항상 묶는 것보다 그룹 인원을 안정적으로 유지한다.
- 정렬 보조 기준이 명시되어 재실행과 장애 복구 시 결과를 재현할 수 있다.

### 3.4 6명 미만 그룹 병합

> 병합할 이웃 선택 구현: [`chooseNeighborIndex()`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/MatchingPoolPartitioner.java#L107-L122)

12~23명일 때만 적용한다. 각 그룹은 병합 후에도 연속된 분위 범위를 가진다.

```text
while 6명 미만 그룹이 존재한다:
  1. 인원이 가장 적은 미달 그룹을 선택
  2. 동률이면 sourceQuartileTo가 큰 높은 점수 그룹을 선택
  3. 인접 그룹 중 인원이 적은 그룹을 병합 대상으로 선택
  4. 양쪽 인원이 같으면 높은 점수 인접 그룹을 선택
  5. 두 그룹을 합치고 점수순 그룹 목록을 다시 구성
```

가장 낮거나 높은 끝 그룹은 인접 그룹이 하나뿐이므로 그 그룹과 병합한다.

#### 예시: 21명

```text
초기: Q1=6, Q2=5, Q3=5, Q4=5

미달 그룹의 인원이 모두 5명
→ 가장 높은 Q4부터 선택
→ 유일한 이웃 Q3과 병합: Q3_Q4=10

남은 미달 Q2=5
→ 이웃 Q1=6, Q3_Q4=10 중 작은 Q1과 병합
→ Q1_Q2=11

최종: Q1_Q2=11, Q3_Q4=10
```

#### 병합 순서 선택 이유

- 처리 순서를 명시하지 않으면 같은 `6·5·5·5`도 `6·15` 또는 `11·10`으로 달라질 수 있다.
- 가장 작은 그룹을 우선해 미달 그룹이 마지막에 고립되는 상황을 줄인다.
- 같은 크기면 높은 그룹부터 처리해 위 사례가 `11·10`으로 수렴하므로 최종 풀이 더 균등하다.
- 인접 그룹이 같으면 높은 쪽과 합친다는 정책과 일관된다.

### 3.5 유효 풀 식별

최종 그룹을 점수 오름차순으로 `poolOrdinal=1..N`으로 다시 번호 매긴다.

```text
groupingMode
  CATEGORY_ONLY       // 카테고리 12명 미만
  QUARTILE            // 24명 이상, Q1~Q4
  MERGED_QUARTILE     // 12~23명, 병합 결과

sourceQuartileFrom
sourceQuartileTo
poolOrdinal
```

예를 들어 Q1_Q2는 `sourceQuartileFrom=1`, `sourceQuartileTo=2`다. 카테고리 단일 풀은 전체 범위를
뜻하도록 1과 4를 저장하되 `groupingMode=CATEGORY_ONLY`로 구분한다.

## 4. 팀 크기 계획

> **구현 코드:** [`TeamSizePlanner.planSizes()`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/TeamSizePlanner.java#L18-L51)가 가능한 3인·4인 팀 개수를 비교한다.

### 4.1 확정 규칙

유효 풀 인원 `n`에 대해 4인 팀 수 `a`, 3인 팀 수 `b`를 다음 사전식 기준으로 선택한다.

```text
제약: 4a + 3b <= n, a >= 0, b >= 0

1순위: assigned = 4a + 3b 최대화
2순위: a 최대화
```

이 계산 결과인 팀 크기 multiset을 알고리즘 실행 전에 고정한다.

| n | 팀 크기 | 배정 | 미배정 |
|---:|---|---:|---:|
| 1 | 없음 | 0 | 1 |
| 2 | 없음 | 0 | 2 |
| 3 | 3 | 3 | 0 |
| 4 | 4 | 4 | 0 |
| 5 | 4 | 4 | 1 |
| 6 | 3+3 | 6 | 0 |
| 7 | 4+3 | 7 | 0 |
| 8 | 4+4 | 8 | 0 |
| 9 | 3+3+3 | 9 | 0 |
| 10 | 4+3+3 | 10 | 0 |
| 11 | 4+4+3 | 11 | 0 |
| 12 | 4+4+4 | 12 | 0 |
| 13 | 4+3+3+3 | 13 | 0 |
| 14 | 4+4+3+3 | 14 | 0 |
| 15 | 4+4+4+3 | 15 | 0 |

6명 이상이면 3과 4의 조합으로 전원 배정할 수 있다. 5명은 4인 팀 한 개와 미배정 한 명이다.

### 4.2 4인 우선 선택 이유

- 기존 서비스의 팀장 선출·채팅·리뷰·협업거리 정책이 4인을 기본으로 한다.
- 3인은 미배정자를 줄이기 위한 보완 팀으로 목적을 제한할 수 있다.
- 팀원이 적을수록 유사성 점수를 맞추기 쉬워 자유 선택 시 3인 팀으로 과도하게 쏠릴 수 있다.
- 12명을 `3+3+3+3`으로 나눠 팀·채팅방 수가 불필요하게 증가하는 것을 막는다.
- 팀 크기를 미리 고정하면 알고리즘은 팀원 조합 최적화에만 집중할 수 있다.

### 4.3 미배정자

팀 크기 계획이 허용하는 배정 수보다 남는 사람은 알고리즘의 전체 결과 비교 기준에 따라 결정한다.
미배정 신청도 `MatchingBatch`를 연결하고 `FAILED`로 저장한다. 카테고리나 인접 점수 그룹을 넘어
임의로 보충하지 않는다.

## 5. 알고리즘 입력과 출력

> **구현 코드:** 신청 엔티티를 입력 모델로 바꾸는 코드는 [`MatchingCandidate.from()`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/model/pool/MatchingCandidate.java#L50-L68), 실행 입력 검증은 [`MatchingPoolInput`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/model/pool/MatchingPoolInput.java#L15-L48), 결과 지표 계산은 [`MatchingPlan.create()`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/model/result/MatchingPlan.java#L50-L81)에 있다.

### 5.1 MatchingCandidate

```text
MatchingCandidate
  applicationId
  memberId
  profileId
  appliedAt
  category
  skillScore
  leaderPreference
  reassignmentPriority
  goalPreferenceScore
  workStyleScore
  communicationStyleScore
  agreeablenessScore
  conscientiousnessScore
  honestyHumilityScore
  extroversionType
```

점수는 신청 당시 스냅샷을 사용한다. 알고리즘 계산 중 프로필·설문·협업거리 원본을 다시 조회하지
않는다.

### 5.2 MatchingPoolInput

```text
MatchingPoolInput
  applicationDate
  category
  poolOrdinal
  candidates
  teamSizes             // 예: [4, 4, 3]
  seed
  searchDeadline
```

`groupingMode`, `sourceQuartileFrom`, `sourceQuartileTo`는 알고리즘 실행 입력이 아니라
`MatchingPoolDefinition`과 `MatchingBatch`가 보존하는 풀 생성 메타데이터다.

### 5.3 MatchingPlan

```text
MatchingPlan
  teams                 // 각 팀은 size=3 또는 4
  unassignedCandidates
  averageTeamScore
  minimumTeamScore
  teamScoreVariance
  selectedAlgorithm
  seed
  elapsedTime
  evaluatedCombinationCount
  exploredBranchCount
  greedyRestartCount
  fallbackOccurred
```

`assignedCount`, `assignedReassignmentCount`, `assignedWantsCount`, `canonicalApplicationOrder`는 저장 필드가
아니라 plan에서 계산하는 메서드다. 최초 선택 알고리즘과 fallback 이후 최종 알고리즘은
`MatchingBatch.initiallySelectedAlgorithm`, `MatchingBatch.finalAlgorithm`에 각각 저장한다.

## 6. 팀 궁합 점수

> **구현 코드:** 전체 계산은 [`TeamCompatibilityCalculator.calculate()`](../../src/main/java/org/cotato/gongmozip/domains/matching/score/TeamCompatibilityCalculator.java#L28-L65), 리더 점수는 [`leaderHarmony()`](../../src/main/java/org/cotato/gongmozip/domains/matching/score/TeamCompatibilityCalculator.java#L68-L105), 유사성 분산은 [`similarity()`](../../src/main/java/org/cotato/gongmozip/domains/matching/score/TeamCompatibilityCalculator.java#L108-L126), 외향성 표는 [`extroversionComplement()`](../../src/main/java/org/cotato/gongmozip/domains/matching/score/TeamCompatibilityCalculator.java#L129-L174)에 있다.

### 6.1 구성요소

| 구성요소 | 배점 |
|---|---:|
| 리더 희망 조화 | 10 |
| 프로젝트 목표 유사성 | 10 |
| 업무방식 유사성 | 10 |
| 소통방식 유사성 | 10 |
| 우호성 유사성 | 20 |
| 성실성 유사성 | 10 |
| 정직겸손성 유사성 | 10 |
| 외향성 상보성 | 20 |
| 합계 | 100 |

궁합 계산기는 정확히 3명 또는 4명만 허용한다. 다른 인원수는 프로그래밍 오류로 처리한다.

### 6.2 리더 희망 조화: 4인 팀

`WANTS=네`, `NEUTRAL=필요하면`, `DOES_NOT_WANT=아니요`로 대응한다.

| WANTS 인원 | NEUTRAL 인원 | 실제 선출 흐름 | 점수 |
|---:|---:|---|---:|
| 1 | 0~3 | 자동 선출 | 10 |
| 0 | 4 | 추천 후보풀 4명 | 8 |
| 0 | 3 | 추천 후보풀 3명 | 7 |
| 0 | 2 | 추천 후보풀 2명 | 6 |
| 0 | 1 | 추천 후보풀 1명 | 5 |
| 0 | 0 | 후보 없음, 무작위 임시 팀장 | 0 |
| 2 | 0~2 | 확정 후보 2명 경쟁 투표 | 7 |
| 3 | 0~1 | 확정 후보 3명 경쟁 투표 | 3 |
| 4 | 0 | 확정 후보 4명 경쟁 투표 | 0 |

WANTS가 1명 이상이면 표의 WANTS 행이 우선하며 NEUTRAL은 점수에 추가하지 않는다.

### 6.3 리더 희망 조화: 3인 팀

| WANTS 인원 | NEUTRAL 인원 | 실제 선출 흐름 | 점수 |
|---:|---:|---|---:|
| 1 | 0~2 | 자동 선출 | 10 |
| 0 | 3 | 추천 후보풀 3명 | 8 |
| 0 | 2 | 추천 후보풀 2명 | 7 |
| 0 | 1 | 추천 후보풀 1명 | 5 |
| 0 | 0 | 후보 없음, 무작위 임시 팀장 | 0 |
| 2 | 0~1 | 확정 후보 2명 경쟁 투표 | 5 |
| 3 | 0 | 확정 후보 3명 경쟁 투표 | 0 |

### 6.4 모집단 분산과 정규화

팀원 수를 `k`라고 할 때 `k`는 3 또는 4다.

```text
평균 μ = Σxi / k
모집단 분산 = Σ(xi - μ)^2 / k
유사성 점수 = (1 - min(모집단 분산 / 4, 1)) × 배점
```

표본분산처럼 `k-1`로 나누지 않는다. 1~5점 척도의 이론상 최대 분산 4를 공통 정규화 기준으로
사용하며 `/100`은 사용하지 않는다.

#### `/4` 선택 이유

4인 팀의 `1·1·5·5`는 분산 4다. `/4`이면 유사성 0점이지만 `/100`이면 배점의 96%를 받아
극단적으로 다른 팀도 거의 같은 팀으로 평가된다. 3인도 같은 1~5 척도이므로 팀 크기별로 수식을
바꾸지 않고 `/4`를 사용한다.

### 6.5 원하는 팀 성향 30점

세 문항을 합쳐 한 번에 분산내지 않고 각각 최대 10점으로 계산한다.

```text
목표 유사성 = (1 - min(목표 점수 모집단 분산 / 4, 1)) × 10
업무 유사성 = (1 - min(업무 점수 모집단 분산 / 4, 1)) × 10
소통 유사성 = (1 - min(소통 점수 모집단 분산 / 4, 1)) × 10
```

모든 응답은 1~5점 척도이며 분산이 작을수록 높은 점수를 받는다.

### 6.6 성격 유사성 40점

역채점까지 반영해 신청서에 저장된 회원별 차원 평균점수를 사용한다.

```text
우호성 = (1 - min(우호성 평균점수 모집단 분산 / 4, 1)) × 20
성실성 = (1 - min(성실성 평균점수 모집단 분산 / 4, 1)) × 10
정직겸손성 = (1 - min(정직겸손성 평균점수 모집단 분산 / 4, 1)) × 10
```

문항 원본을 알고리즘에서 다시 채점하지 않는다.

### 6.7 외향성 상보성: 4인 팀

| 조합 | 점수 | 조합 | 점수 |
|---|---:|---|---:|
| E1 A2 I1 | 20 | E1 A1 I2 | 18 |
| E2 A1 I1 | 18 | A4 | 17 |
| E1 A3 | 16 | A3 I1 | 16 |
| E2 I2 | 15 | E1 I3 | 14 |
| E2 A2 | 13 | A2 I2 | 13 |
| E3 A1 | 10 | E3 I1 | 10 |
| A1 I3 | 9 | E4 | 5 |
| I4 | 5 | | |

세 유형 인원 합이 4인 15개 전체 분포를 포함한다.

### 6.8 외향성 상보성: 3인 팀

| 조합 | 점수 | 조합 | 점수 |
|---|---:|---|---:|
| E1 A1 I1 | 20 | A3 | 18 |
| E1 A2 | 16 | A2 I1 | 15 |
| E1 I2 | 14 | E2 A1 | 11 |
| A1 I2 | 10 | E2 I1 | 8 |
| I3 | 5 | E3 | 3 |

세 유형 인원 합이 3인 10개 전체 분포를 포함한다.

### 6.9 팀장 추천과의 분리

외향적 리더와 내향적 팔로워의 연구 결과는 실제 리더가 확정돼야 적용할 수 있다. 매칭 단계에서는
선호만 존재하므로 위 분포 점수만 사용한다. “이 팀에서는 외향형 팀원이 리더를 맡는 것이 좋다”와
같은 메시지는 별도 팀장 추천 AI에서만 사용한다.

### 6.10 반올림

- 계산은 `BigDecimal`을 사용한다.
- 항목별 점수와 총점은 소수 둘째 자리 `HALF_UP`이다.
- 팀 평균·최저는 둘째 자리, 팀 점수 분산은 넷째 자리 `HALF_UP`이다.
- 저장되는 반올림 결과와 plan comparator가 사용하는 결과가 달라지지 않도록 한 정책을 공유한다.

## 7. 전체 결과 비교

> **구현 코드:** [`MatchingPlanComparator.compare()`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/MatchingPlanComparator.java#L16-L43). 반환값이 양수이면 왼쪽 계획이 더 좋은 결과다.

팀 크기 목록은 입력마다 이미 고정되어 있다. 후보 계획은 다음 사전식 순서로 비교한다.

1. 전체 배정 인원수 최대화
2. 재배정 대상자 배정 수 최대화
3. 전체 팀 평균 궁합 최대화
4. `WANTS` 신청자 배정 수 최대화
5. 최저 팀 궁합 최대화
6. 팀 점수 분산 최소화
7. 팀 경계와 미배정 경계를 포함한 정규화 신청 ID 목록의 사전식 순서

위 항목이 다르면 아래 항목은 비교하지 않는다. 4인 팀 수는 `TeamSizePlanner`에서 이미 최대화했으므로
comparator에 중복 추가하지 않는다.

팀 평균은 팀 크기와 무관하게 생성된 팀 점수의 산술평균이다. 3인 팀과 4인 팀 모두 최대 100점의
확정 표를 사용한다.

## 8. Brute Force

> **구현 코드:** 탐색 시작은 [`match()`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/BruteForceMatchingAlgorithm.java#L40-L64), 재귀 탐색은 [`search()`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/BruteForceMatchingAlgorithm.java#L67-L130), 시간 제한 검사는 [`checkDeadline()`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/BruteForceMatchingAlgorithm.java#L182-L187)에 있다.

### 8.1 목적

작은 유효 풀에서 `teamSizes`가 요구하는 모든 팀 분할과 미배정자 선택을 탐색해 공통 comparator의
최적 결과를 구한다.

### 8.2 탐색

```text
search(남은 후보, 남은 팀 크기 multiset, 현재 팀 목록):
  남은 팀 크기가 없으면 plan 평가
  필요한 최소 인원보다 후보가 적으면 중단
  현재 anchor를 포함할 수 있는 각 팀 크기 k에 대해:
    나머지 후보 중 k-1명 조합 생성
    팀 점수 계산
    선택한 k명을 제거하고 재귀
  미배정 허용 수가 남아 있으면 anchor를 미배정하고 재귀
```

같은 크기의 팀은 순서가 없는 집합이므로 중복 분할을 제거한다. 3인 키와 4인 키를 각각 정렬된 신청
ID로 캐시한다.

### 8.3 가지치기

- 남은 인원으로 요구 팀 크기를 채울 수 없는 분기 제거
- 재배정 대상자 최대 가능 수가 best보다 작은 분기 제거
- 이론상 남은 팀이 모두 100점이어도 평균을 이길 수 없는 분기 제거

가지치기는 공통 comparator 결과를 바꾸지 않아야 한다.

### 8.4 시간 제한

검색 마감에 도달하면 부분 최적 결과를 반환하지 않고 `MatchingSearchTimeoutException`을 발생시킨다.
selector는 같은 후보 전체와 같은 팀 크기 목록을 Multi-start Greedy에 전달한다.

## 9. Multi-start Greedy

> **구현 코드:** 전체 반복은 [`match()`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/MultiStartGreedyMatchingAlgorithm.java#L42-L82), 한 번의 Greedy는 [`runOnce()`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/MultiStartGreedyMatchingAlgorithm.java#L85-L108), 지역 개선은 [`improve()`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/MultiStartGreedyMatchingAlgorithm.java#L146-L188), 시작 순서 생성은 [`createStarts()`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/MultiStartGreedyMatchingAlgorithm.java#L191-L227)에 있다.

### 9.1 앵커 우선순위

```text
재배정 대상자
  > WANTS
  > NEUTRAL·DOES_NOT_WANT
```

각 우선순위 버킷 내부에서만 순서를 바꾼다. 낮은 우선순위가 높은 우선순위보다 먼저 앵커가 될 수
없다.

### 9.2 시작점

- `appliedAt ASC`, ID tie-break
- `skillScore DESC`, ID tie-break
- `skillScore ASC`, ID tie-break
- 배치 고정 시드 기반 버킷 내부 셔플 `N`회

3인·4인 혼합 크기가 있으면 각 후보 순서에 대해 다음 팀 크기 순서도 시작점으로 포함한다.

- 4인 크기 먼저, 그다음 3인
- 3인 크기 먼저, 그다음 4인

같은 크기만 있으면 한 순서만 사용한다. 각 시작 결과는 공통 comparator로 비교한다.

### 9.3 한 번의 Greedy

1. 현재 순서에서 가장 앞선 후보를 앵커로 선택한다.
2. 현재 처리할 팀 크기 `k`를 확인한다.
3. 남은 후보 중 `k-1`명 조합을 모두 평가한다.
4. 앵커를 포함한 최고 궁합 팀을 확정한다.
5. 선택한 후보를 제거하고 다음 팀 크기로 반복한다.
6. 팀 크기 목록이 끝나면 남은 후보를 미배정으로 둔다.

팀 하나의 동률은 정렬된 신청 ID로 결정한다.

### 9.4 지역 개선

Greedy 결과가 나온 뒤 미배정자 한 명과 배정자 한 명을 교환한다. 교환 후에도 해당 팀 크기는
그대로 3명 또는 4명이다.

- 공통 comparator에서 전체 plan이 엄격히 좋아지는 교환만 반영
- 재배정 대상자 수를 줄이는 교환 거부
- 평균까지 같을 때 WANTS 수가 줄어드는 교환 거부
- 개선이 없을 때 종료

미배정자가 없으면 이 단계는 실행하지 않는다.

### 9.5 결정성

배치 기본 시드는 다음 정보를 안정적으로 조합해 만든다.

```text
applicationDate + category + groupingMode + poolOrdinal + sourceQuartileFrom + sourceQuartileTo
```

각 Greedy 재시작 시드는 위 기본 시드와 재시작 순번을 결정적으로 조합한다. Java의 런타임 기본 hash나
현재 시각을 사용하지 않는다.

## 10. 알고리즘 선택과 시간 예산

> **구현 코드:** 선택·fallback은 [`MatchingAlgorithmSelector.match()`](../../src/main/java/org/cotato/gongmozip/domains/matching/algorithm/MatchingAlgorithmSelector.java#L27-L55), 운영 기본값은 [`application.yml`](../../src/main/resources/application.yml#L100-L107)에 있다.

### 10.1 선택

- 병합 전 분위 인원이 아니라 최종 유효 풀 인원을 사용한다.
- `poolSize <= bruteforceMaxPoolSize`: Brute Force
- 그 외: Multi-start Greedy
- 3명 미만 풀: 알고리즘 없이 전원 미배정

기본 임계값은 12이고, Greedy는 고정 순서 3개와 시드 기반 셔플 50개를 사용한다. 3인·4인 팀 크기가
섞이면 각 후보 순서에 정방향·역방향 팀 크기 순서를 모두 적용하므로 실제 시작점 수는 최대 106개다.
현재 정책 벤치마크 결과는 16.1절에 기록한다.

### 10.2 fallback

fallback 판단과 재계산 단위는 개별 유효 풀이다. 한 풀의 Brute Force가 timeout되면 해당 풀에서 계산한
부분 결과만 폐기하고, 같은 풀의 후보 전체와 같은 팀 크기 목록을 Greedy로 다시 계산한다. 다른 풀의
알고리즘 선택과 결과에는 영향을 주지 않는다. 단, Brute Force와 Greedy의 절대 마감시각은 일일 전체
배치가 공통으로 사용하므로 뒤에 처리되는 풀일수록 남은 시간이 짧다.

```text
14:00 전체 배치 시작
15:20 Brute Force 검색 최종 중단 예산 예시
15:30 새 풀 시작을 중단하는 전체 배치 기준 시각
16:00 결과 공개
```

`fallbackReserve=10분`을 빼서 Brute Force deadline을 15시 20분으로 만들고, Greedy를 직접 선택하거나
fallback할 때 그 10분을 다시 더해 15시 30분을 Greedy deadline으로 사용한다. 저장 시간을 위해 별도의
예산을 남기지는 않는다.

15시 30분은 hard timeout이 아니다. 오케스트레이터는 새 풀을 선점하기 전에만 전체 마감을 확인하고,
Greedy는 최초 시작점 결과를 하나 보장한 뒤 다음 시작점 사이에서만 deadline을 확인한다. 이미 시작한
한 번의 Greedy 계산·지역 개선과 그 뒤의 DB 저장은 15시 30분을 넘길 수 있다. 따라서 현재 구현이
보장하는 것은 모든 계산·저장의 정각 완료가 아니라 마감 이후 새 풀을 시작하지 않고, 이미 시작한 풀은
최초 결과 이후의 추가 Greedy 시작점을 best-effort로 중단하는 것이다.

## 11. 결과 저장

> **구현 코드:** 풀 준비는 [`MatchingPoolPreparationService.prepare()`](../../src/main/java/org/cotato/gongmozip/domains/matching/service/MatchingPoolPreparationService.java#L40-L68), 배치 선점은 [`MatchingBatchClaimService.claim()`](../../src/main/java/org/cotato/gongmozip/domains/matching/service/MatchingBatchClaimService.java#L38-L66), 성공·실패 저장은 [`MatchingResultPersistenceService`](../../src/main/java/org/cotato/gongmozip/domains/matching/service/MatchingResultPersistenceService.java#L45-L89)에 있다.

### 11.1 관계

```text
MatchingBatch
  1 ─── N MatchingGroup
              1 ─── 3..4 MatchingGroupMember
                              N ─── 1 MatchingApplication

MatchingGroup
  1 ─── 0..1 MatchingReason
  1 ─── 0..1 Team

MatchingApplication
  1 ─── 0..N MatchingApplication  // sourceApplication 기반 재배정 이력
```

### 11.2 MatchingBatch

```text
MatchingBatch
  matchingBatchId
  applicationDate
  category
  groupingMode
  poolOrdinal
  sourceQuartileFrom
  sourceQuartileTo
  status
  initiallySelectedAlgorithm
  finalAlgorithm
  randomSeed
  elapsedMillis
  startedAt
  completedAt
  retryCount
  failureMessage
```

중복 실행 방지 기준은 다음과 같다.

```text
UNIQUE(application_date, category, skill_group)
```

V22에서는 기존 `matching_batches.skill_group` 물리 컬럼을 논리적 `poolOrdinal` 저장소로 재사용한다.
애플리케이션 필드와 조회 기준은 `poolOrdinal`이므로 고정 점수구간 의미로 사용하지 않는다. 기존 컬럼을
바로 이름 변경하지 않은 이유는 데이터 복사와 유니크 인덱스 재생성 위험 없이 호환 마이그레이션을
적용하기 위해서다.

`SUCCEEDED`는 재실행하지 않고, `RUNNING`은 중복 실행을 거부하며, `FAILED`만 같은 시드로 재시도한다.
재시도 전 신청 상태가 바뀌지 않았다면 입력도 같지만, 계산 중 패스한 신청이 배치에서 제외되면 남은
입력으로 다시 실행한다.

### 11.3 MatchingGroup

```text
MatchingGroup
  matchingBatch
  teamSize                 // 3 또는 4
  matchingScore
  leaderHarmonyScore
  goalSimilarityScore
  workStyleSimilarityScore
  communicationSimilarityScore
  agreeablenessSimilarityScore
  conscientiousnessSimilarityScore
  honestyHumilitySimilarityScore
  extroversionComplementScore
  status
  responseDeadlineAt
  confirmedAt
  canceledAt
  expiredAt
  confirmedTeamSize
  team
```

`teamSize`와 실제 `MatchingGroupMember` 수가 일치해야 한다. 기존 category와 skillGroup 호환 필드는
즉시 제거하지 않으며, 새 결과에서는 배치의 category와 최종 `poolOrdinal`을 복사한다.
DB의 `chk_matching_groups_team_size`는 `teamSize`를 3 또는 4로 제한한다. 저장 전 `MatchedTeam`과
`MatchingPlan`이 팀 크기·중복·배정자와 미배정자의 분리를 검증하고, 결과 조회 시에는 저장된
`teamSize`와 실제 `MatchingGroupMember` 수가 같은지 다시 확인한다.

새 그룹은 `PROPOSED`와 `responseDeadlineAt=applicationDate D+1 12:00`으로 시작한다. 응답 결과에 따라
실제 Team이 만들어지면 `CONFIRMED`, 패스로 유효 인원이 3명 미만이면 `CANCELED`, 마감에도 Team을
만들지 못하면 `EXPIRED`로 닫힌다. 확정·취소·만료 시각과 실제 확정 인원은 해당 스냅샷 필드에 남긴다.

### 11.4 MatchingGroupMember와 신청 상태

```text
배정 신청
  application.matchingBatch = batch
  application.status = PROPOSED
  MatchingGroupMember 존재

미배정 신청
  application.matchingBatch = batch
  application.status = FAILED
  MatchingGroupMember 없음
```

그룹원은 정확히 3명 또는 4명이고 모두 서로 다른 신청이어야 한다.
`MatchingGroupMember.member`와 `matchingApplication.member`는 같아야 한다.

배정 직후 `MatchingGroupMember.responseStatus`는 `PENDING`이다. 사용자가 응답하면 `ACCEPTED` 또는
`PASSED`, 마감 작업이 미응답자를 처리하면 `EXPIRED`가 되며 `responseSource`에 `USER` 또는
`DEADLINE_JOB`을 저장한다. 패스와 자동 만료는 계산된 `passPenalty`도 응답 스냅샷으로 남긴다.
수동 패스와 자동 만료는 최근 7일의 기존 `PASSED` 횟수를 기준으로 `3, 5, 7, 9, 11` 순서의 같은
감점 정책을 사용하고 11에서 상한을 둔다. 양수 감점값은 그룹원 응답에 저장하고 협업거리 원장에는
음수 변화량으로 기록한다. 결과가 생성된 `PROPOSED` 신청은 기존 자정 철회 제한 대신 그룹의
`responseDeadlineAt`까지 패스할 수 있다.

신청 상태는 그룹 응답에 따라 다음처럼 이어진다.

```text
PROPOSED
  ├─ 최종 Team 구성원으로 확정: MATCHED
  ├─ 직접 패스 또는 자동 만료: PASSED
  └─ 다른 구성원의 패스·미응답으로 재배정 대상이 됨: REASSIGN_PENDING
```

자동 재배정은 원본 신청의 배치·그룹 이력을 덮어쓰지 않고 `sourceApplication`, `reassignmentCount`,
`reassignmentReason`을 가진 새 `WAITING` 신청을 생성한다.

### 11.5 트랜잭션

```text
짧은 준비 트랜잭션
  matchingBatch가 없는 당일 WAITING 신청 lock
  카테고리별 백분위·병합 계산
  유효 풀별 MatchingBatch 생성
  신청에 batch와 유효 poolOrdinal 연결, 상태는 WAITING 유지

짧은 풀 선점 트랜잭션
  PENDING 또는 FAILED 배치 lock
  배치에 연결된 WAITING 신청 lock
  현재 인원으로 팀 크기 목록 확정
  배치 RUNNING, 신청 MATCHING 전이

트랜잭션 밖
  궁합 점수·Brute Force 또는 Multi-start Greedy 계산

짧은 결과 저장 트랜잭션
  배치 상태 재검증
  그룹·그룹원·점수 저장
  배정 PROPOSED, 미배정 FAILED
  배치 SUCCEEDED
```

실패 저장은 별도 `REQUIRES_NEW` 트랜잭션으로 처리해 해당 풀 신청을 안전하게 복구한다. 한 풀 실패가
다른 카테고리나 풀의 성공 결과를 롤백하지 않는다.

수락·패스는 한 쓰기 트랜잭션에서 `MatchingGroup → MatchingGroupMember ID 오름차순 → Member` 순으로
잠근다. 마지막 응답에서 Team 생성이 필요하면 `TeamService` 저장과 신청·그룹 상태 전이도 같은
트랜잭션에 참여하므로 일부 상태만 커밋되지 않는다. 마감 작업은 그룹마다 `REQUIRES_NEW`로 분리하고,
자동 재배정 신청 생성도 해당 그룹 응답 트랜잭션에 함께 참여한다.

### 11.6 오늘의 본인 결과 조회

> **구현 코드:** 응답 계약은 [`MatchingResultResponse`](../../src/main/java/org/cotato/gongmozip/domains/matching/dto/response/MatchingResultResponse.java#L18-L53), 상태 판정은 [`MatchingResultQueryService`](../../src/main/java/org/cotato/gongmozip/domains/matching/service/MatchingResultQueryService.java#L35-L161), 당일 조회는 [`MatchingApplicationRepository.findResultApplication()`](../../src/main/java/org/cotato/gongmozip/domains/matching/repository/MatchingApplicationRepository.java#L27-L41), 이전 결과 fallback과 그룹 조회는 [`MatchingGroupMemberRepository`](../../src/main/java/org/cotato/gongmozip/domains/matching/repository/MatchingGroupMemberRepository.java#L55-L106)에 있다.

```http
GET /api/matching/results/me/today
```

URL로 다른 회원이나 그룹 ID를 받지 않는다. 먼저 인증 회원 ID와 한국 시간 기준 오늘 날짜로 신청을
찾고, 오늘 신청이 없을 때만 그룹 상태가 `PROPOSED` 또는 `CONFIRMED`인 과거 결과 중 가장 최근
신청 하나를 fallback으로 찾는다. fallback에는 전날로 한정하는 날짜 조건이 없으며 신청일과 신청 ID
내림차순으로 한 건만 선택한다. 조회는 `readOnly` 트랜잭션이며 결과 상태를 바꾸지 않는다.

| `resultStatus` | 의미 | 팀·점수 공개 |
|---|---|---|
| `NOT_APPLIED` | 오늘 신청도 없고 fallback 가능한 이전 결과도 없음 | 공개하지 않음 |
| `WITHDRAWN` | 신청을 취소했거나 패스함 | 결과 생성 전 취소·패스는 비공개, 결과 생성 후 패스는 공개 시각 이후 기존 그룹·점수 공개 |
| `NOT_PUBLISHED` | 결과가 저장됐더라도 정책으로 계산한 `publishedAt` 전 | 공개하지 않음 |
| `PROCESSING` | 공개 시각이 지났지만 배치가 아직 처리 중 | 공개하지 않음 |
| `UNMATCHED` | 팀 크기 계획에서 미배정되어 신청이 `FAILED` | 공개하지 않음 |
| `MATCHED` | 공개된 `PROPOSED`, `MATCHED` 또는 피해자의 `REASSIGN_PENDING` 신청 | 3명 또는 4명의 제안 그룹과 저장 점수 공개 |

공개 시각은 `MatchingTimePolicy`가 신청일과 YML의 `result-publish-time`으로 계산하며, 판정은
`now >= publishedAt`이다. 일반 신청은 공개 전에 `MatchingGroupMember`를 조회하지 않는다. 다만
`PASSED` 신청은 결과 생성 전 패스인지 결과 생성 후 패스인지 구분하기 위해 본인 membership 존재 여부를
먼저 확인한다. 어떤 경우에도 공개 전에는 그룹원 목록과 점수를 반환하지 않는다.

배정 결과는 다음 순서로 읽는다.

```text
인증 회원 + 오늘 날짜로 MatchingApplication 조회
  ├─ 존재: 오늘 신청 사용
  └─ 없음: 과거 PROPOSED·CONFIRMED 그룹의 최신 신청 한 건 fallback
  → MatchingTimePolicy로 신청일의 공개 시각 계산·검증
  → 본인 MatchingGroupMember로 MatchingGroup 확인
  → 같은 그룹의 3명 또는 4명 신청·회원·선택 프로필 fetch join
  → 저장된 matchingScore와 항목별 점수 반환
```

팀원 응답에는 `memberId`, 신청에 사용한 `profileId`와 닉네임, 신청 당시 `characterType`,
`leaderPreference`, 현재 `responseStatus`, 본인 여부를 포함한다. 궁합 점수는 현재 프로필이나 설문으로
재계산하지 않고 `MatchingGroup`에 저장된 총점과 8개 세부 점수를 그대로 사용한다.

최상위 응답에는 `responseDeadlineAt`, `groupStatus`, 본인의 `myResponseStatus`,
`confirmedTeamSize`, 실제 Team이 생성된 경우 `teamId`도 포함한다.

결과 조회 자체는 수락·패스·재배정·실제 `Team` 생성을 수행하지 않는다. 이 상태 전이는
`MatchingResponseService`, `MatchingResponseDeadlineService`, `MatchingGroupCompletionService`,
`MatchingReassignmentService`의 쓰기 트랜잭션으로 분리되어 있다.

### 11.7 결과 응답과 Team 확정

```http
GET  /api/matching/groups/{matchingGroupId}/responses
POST /api/matching/groups/{matchingGroupId}/accept
POST /api/matching/applications/{applicationId}/withdraw
```

응답 현황은 해당 그룹원만 공개 이후 조회할 수 있다. 수락은 공개 이후이면서 마감 전인 `PENDING`
응답만 허용하고, 같은 수락 요청의 재시도에는 기존 상태와 `teamId`를 멱등하게 반환한다. 수락은 패스로
번복할 수 없다.

통합 철회 API는 결과 그룹원이 없는 신청에는 기존 무료 취소·패널티 패스 정책을 적용하고, 결과 그룹원이
있으면 공개 전후와 관계없이 마감 전 `PASSED` 응답으로 처리한다. 같은 패스 요청의 재시도에는 저장된
`passPenalty`를 반환해 협업거리를 다시 차감하지 않는다. 무료 취소는 신청일 14:00 전까지 가능하므로
16:00 이후 접수된 익일 신청은 신청일 전날에도 무료 취소로 철회할 수 있다.

3인 제안에서 한 명이 패스하거나 4인 제안에서 두 명이 패스하면 유효 인원이 3명 미만이므로 그룹을
`CANCELED`로 닫고 피해자만 자동 재배정한다. 4인 제안에서 한 명이 패스한 경우에는 남은 3명의 응답을
계속 기다리며, 세 명이 모두 수락하면 3인 Team으로 확정한다.

## 12. 스케줄러와 중복 실행 방지

> **구현 코드:** 실행 시각과 락 이름은 [`MatchingSchedulerJobs`](../../src/main/java/org/cotato/gongmozip/domains/scheduler/MatchingSchedulerJobs.java#L21-L27), JDBC 락 공급자는 [`SchedulingConfig`](../../src/main/java/org/cotato/gongmozip/global/config/SchedulingConfig.java#L16-L27), 테이블은 [`V21__implement_matching_batch.sql`](../../src/main/resources/db/migration/V21__implement_matching_batch.sql#L86-L91)에 있다.

### 12.1 시간

- cron: `0 0 14 * * *`
- zone: `Asia/Seoul`
- 새 풀 시작 중단 기준: 15:30(best effort)
- 공개 시각: 16:00
- 신청 접수: ~14:00 당일 신청, 14:00~16:00 신청 불가, 16:00~ 다음 날 신청
- 응답 마감: 신청일 다음 날 12:00, 마감 대상 재조회 주기 5분

### 12.2 JDBC ShedLock

- 락 이름: `matching-daily-batch`
- 초기 `lockAtMostFor`: `PT100M`
- DB 시각 사용
- 스케줄러 메서드는 오케스트레이터가 대상 풀 처리를 마치거나 마감 기준으로 중단한 뒤 반환

ShedLock은 여러 인스턴스의 동시 진입을 막고, 배치 유니크 제약과 상태 검증은 정상 실행·수동 재실행·
락 만료 뒤의 중복 저장을 막는다.

Redis 분산 락을 사용하지 않는 이유는 하루 한 번의 저경합 배치, MySQL 결과 저장 의존성, 장기 TTL
연장과 소유권 관리 복잡성, DB 유니크 제약이 어차피 필요하다는 점 때문이다.

### 12.3 응답 마감 작업

> **구현 코드:** [`MatchingResponseDeadlineJobs`](../../src/main/java/org/cotato/gongmozip/domains/scheduler/MatchingResponseDeadlineJobs.java#L17-L42)와 [`MatchingResponseDeadlineService`](../../src/main/java/org/cotato/gongmozip/domains/matching/service/MatchingResponseDeadlineService.java#L43-L116).

- cron: `0 */5 * * * *`
- zone: `Asia/Seoul`
- 락 이름: `matching-response-deadline`
- `lockAtMostFor`: `PT10M`
- 조회 조건: `PROPOSED`이면서 `responseDeadlineAt <= now`

대상 ID는 읽기 트랜잭션으로 먼저 조회하고, 그룹마다 독립된 `REQUIRES_NEW` 트랜잭션에서 그룹·그룹원·
회원 순으로 잠가 처리한다. 한 그룹이 실패해도 다음 그룹 처리는 계속하며, 실패한 `PROPOSED` 그룹은
다음 5분 주기에 다시 조회한다.

### 12.4 현재 장애 복구 제한

배치를 `RUNNING`으로 커밋한 뒤 알고리즘 계산과 결과 저장은 트랜잭션 밖에서 이어진다. 이 구간에서
프로세스가 강제 종료되면 배치가 `RUNNING`으로 남을 수 있다. 현재 처리 대상 조회는 `PENDING`과
`FAILED`만 포함하고, 오래된 `RUNNING`을 자동으로 `FAILED`로 전환하는 복구 작업은 없다.

또한 다음 날 14시 스케줄은 다음 날짜의 신청을 처리하므로 전날 남은 `PENDING`·`FAILED` 배치를 자동으로
재시도하지 않는다. 현재 코드에는 마감이 지난 신청일을 복구하는 별도 진입점도 없다. 운영자는 DB 상태를
확인할 수 있을 뿐이며, stale `RUNNING` 판별·상태 복구와 이전 신청일 재처리 진입점은 후속 운영 기능으로
구현해야 한다.

## 13. 테스트와 벤치마크

> **실행 방법:** 일반 검증은 `./gradlew clean build`, 성능·품질 측정은 `./gradlew matchingBenchmark --rerun-tasks`로 분리한다. 일반 `test`는 [`build.gradle`](../../build.gradle#L71-L89)에서 `matching-benchmark` 태그를 제외한다.

| 검증 대상 | 테스트 코드 |
|---|---|
| 신청 시점 역량 스냅샷 | [`MatchingApplicationServiceTest.applyStoresSnapshot()`](../../src/test/java/org/cotato/gongmozip/domains/matching/service/MatchingApplicationServiceTest.java#L184-L230) |
| 역량 점수 가중치와 경계 | [`SkillScoreCalculatorTest`](../../src/test/java/org/cotato/gongmozip/domains/matching/score/SkillScoreCalculatorTest.java#L19-L72) |
| 저장된 프로젝트 평가 사용 | [`StoredProjectScoreProviderTest`](../../src/test/java/org/cotato/gongmozip/domains/matching/score/StoredProjectScoreProviderTest.java#L34-L67) |
| 4분할·6명 미만 병합 | [`MatchingPoolPartitionerTest`](../../src/test/java/org/cotato/gongmozip/domains/matching/algorithm/MatchingPoolPartitionerTest.java#L18-L91) |
| 3·4인 팀 크기 계획 | [`TeamSizePlannerTest`](../../src/test/java/org/cotato/gongmozip/domains/matching/algorithm/TeamSizePlannerTest.java#L14-L38) |
| 궁합 점수표와 분산 | [`TeamCompatibilityCalculatorTest`](../../src/test/java/org/cotato/gongmozip/domains/matching/score/TeamCompatibilityCalculatorTest.java#L20-L124) |
| 계획 비교 우선순위 | [`MatchingPlanComparatorTest`](../../src/test/java/org/cotato/gongmozip/domains/matching/algorithm/MatchingPlanComparatorTest.java#L23-L41) |
| Brute Force·Greedy·결정성 | [`MatchingAlgorithmsTest`](../../src/test/java/org/cotato/gongmozip/domains/matching/algorithm/MatchingAlgorithmsTest.java#L43-L107) |
| 알고리즘 선택·fallback | [`MatchingAlgorithmSelectorTest`](../../src/test/java/org/cotato/gongmozip/domains/matching/algorithm/MatchingAlgorithmSelectorTest.java#L40-L62) |
| 준비·선점·성공·실패 저장 | [`MatchingBatchIntegrationTest`](../../src/test/java/org/cotato/gongmozip/domains/matching/service/MatchingBatchIntegrationTest.java#L82-L230) |
| 결과 공개 시각·당일/이전 결과 fallback·응답 매핑 | [`MatchingResultQueryServiceTest`](../../src/test/java/org/cotato/gongmozip/domains/matching/service/MatchingResultQueryServiceTest.java#L61-L276) |
| 결과 조회 컨트롤러 응답 | [`MatchingResultControllerTest`](../../src/test/java/org/cotato/gongmozip/domains/matching/controller/MatchingResultControllerTest.java#L42-L84) |
| 저장 결과의 실제 조회 쿼리 | [`MatchingBatchIntegrationTest`](../../src/test/java/org/cotato/gongmozip/domains/matching/service/MatchingBatchIntegrationTest.java#L86-L154) |
| 수락·패스·3/4인 Team 확정 | [`MatchingResponseServiceTest`](../../src/test/java/org/cotato/gongmozip/domains/matching/service/MatchingResponseServiceTest.java), [`MatchingResponseIntegrationTest`](../../src/test/java/org/cotato/gongmozip/domains/matching/service/MatchingResponseIntegrationTest.java) |
| 응답 마감·패널티·재배정 | [`MatchingResponseDeadlineServiceTest`](../../src/test/java/org/cotato/gongmozip/domains/matching/service/MatchingResponseDeadlineServiceTest.java), [`MatchingReassignmentServiceTest`](../../src/test/java/org/cotato/gongmozip/domains/matching/service/MatchingReassignmentServiceTest.java) |
| 마감 스케줄러의 그룹별 실패 격리 | [`MatchingResponseDeadlineJobsTest`](../../src/test/java/org/cotato/gongmozip/domains/scheduler/MatchingResponseDeadlineJobsTest.java) |
| 품질 손실률·성능 | [`MatchingAlgorithmBenchmarkTest`](../../src/test/java/org/cotato/gongmozip/domains/matching/benchmark/MatchingAlgorithmBenchmarkTest.java#L31-L140) |

### 13.1 분류

- 11명 카테고리 단일 풀 대표 사례
- 12·13·20·21·23명 분할과 병합 경계
- 24명 4개 분위와 25명 균등 분할
- 동점자의 appliedAt·ID 분리
- `6·5·5·5 → 11·10`
- 그룹 순서와 병합 범위 재현

### 13.2 팀 크기

- 0~15명 표 전체
- 5명은 4인 팀과 미배정 1명
- 9명은 3인 팀 세 개
- 12명은 4인 팀 세 개
- 15명은 4+4+4+3

### 13.3 점수

- 3인·4인 리더 표 전 행
- 3인 외향성 10개와 4인 외향성 15개
- 이상적인 3인·4인 팀의 총점 100점
- 최대 모집단 분산에서 목표·우호성 유사도 0점
- 후보 순서에 대한 점수 불변

목표·업무·소통 각 항목의 독립 경계, 모든 결과의 0~100 범위와 반올림 경계는 현재 테스트에서 별도로
열거하지 않는다.

### 13.4 알고리즘

- Brute Force의 8명 `4+4`, 7명 `4+3`, 5명 `4+미배정 1명`
- 평균 점수보다 재배정 대상자 배정을 우선하는 사례
- Brute Force deadline 도달 시 timeout 발생
- Greedy의 같은 입력·시드 결과 재현
- Brute timeout 후 전체 입력 Greedy fallback
- 유효 풀 크기 2·3·16·17명의 알고리즘 선택 경계

공통 comparator의 7개 우선순위 중 전용 단위 테스트는 `재배정 수 > 평균`, `평균 > WANTS 수` 관계를
검증한다. 나머지 비교 순서는 구현에는 존재하지만 각각을 분리한 테스트는 아직 없다.

### 13.5 저장·배치

- 카테고리 단일 풀의 배치 선점과 4인 팀 저장·조회
- 3인 팀과 세 명의 원본 신청 연결 저장
- 실패 후 같은 배치·시드·변경 없는 입력 재선점
- 16시 전 팀원·점수 비공개와 공개 이후 3명·4명 결과 조회
- 오늘 신청 없음·확정된 이전 결과 fallback·처리 중·미배정·철회 상태 응답과 컨트롤러 매핑
- 결과 생성 후 패스의 공개 전 차단과 공개 후 기존 그룹 정보 반환
- 수락·패스 멱등성, 4인 제안에서 3인 확정, 3명 미만 그룹 취소와 피해자 재배정
- 12시 미응답 패널티, Team 확정 또는 그룹 만료, 실패 그룹의 다음 주기 재시도

DB 유니크 충돌, 병합 풀 메타데이터 저장, 계산 중 패스, 15시 30분 hard deadline, stale `RUNNING`,
ShedLock 다중 인스턴스 동작은 현재 통합 테스트 범위에 포함되지 않는다.

### 13.6 품질 기준

동일한 현재 정책 입력을 Brute Force와 Greedy에 실행해 먼저 다음 정확성 조건을 확인한다.

- 배정 인원수 동일
- 재배정 대상자 배정 수 동일
- 사전 확정된 팀 크기 multiset 동일
- 중복 배정 없음

그다음 평균 궁합 손실률을 계산한다.

```text
손실률 = (Brute Force 평균 - Greedy 평균) / Brute Force 평균 × 100
```

- 게이트 구간(17·18명 fixture) 최대 손실률 5% 이하
- 참고 구간(3~16명)은 손실률 로그만 남기고 assertion 없음
- 운영 전체 배치 목표 90분 이내

전용 `@Tag("matching-benchmark")` 테스트와 `matchingBenchmark` Gradle 태스크를 사용한다. 과거 고정
점수 구간·4인 고정 팀 fixture의 측정값은 현재 정책의 성능 근거로 인정하지 않고 비교용 기록으로만 남긴다.
2026-08-16 재구성 후 벤치마크는 참고 구간(3~16명), 게이트 구간(17~18명), Greedy 전용(20·100명)으로
나뉜다. 참고 구간은 Brute Force 상한 이하라 운영에서 항상 Brute Force가 처리하므로 손실률을 로그로만
남기고 assertion은 걸지 않는다. 게이트 구간은 Greedy가 담당하는 크기 중 Brute Force와 비교 가능한
최대 범위이며 최악 손실률 5% 이하만 검증한다. 24개 풀 순수 알고리즘 시간은 16명 기준으로 별도
측정한다. 90분 목표는 assertion이 아니며, DB 조회·저장, 잠금과 네트워크 시간은 벤치마크에 포함되지
않는다.

## 14. 사용자 노출 범위

세부 백분위 경계, 병합 순서, 알고리즘 반복 횟수, 점수표는 사용자에게 상세 노출하지 않는다.
사용자에게는 다음 수준만 설명한다.

- **심리학 기반 성격·협업 스타일 분석**: HEXACO 기반 자체 검사로 협업에 중요한 특성을 분석
- **성격·협업 스타일 조합 분석**: 유사성뿐 아니라 외향성의 상보성도 함께 고려
- **팀 시너지 최적화**: 여러 팀 조합을 반복 분석해 전체 팀의 평균 시너지를 높이는 방향으로 매칭
- **매칭 사유**: 결과 팀의 강점과 공통점을 별도 AI 설명으로 제공

알고리즘 점수는 결정적 도메인 결과이고, AI 매칭 사유는 그 결과를 설명하는 선택적 콘텐츠다. AI
설명 생성이 실패해도 팀 결과는 보존한다.

## 15. 확정 정책 요약

- 카테고리 6개를 1차 분리 기준으로 사용
- 24명 이상은 백분위 4개 그룹
- 12~23명은 4등분 후 6명 미만 그룹 반복 병합
- 12명 미만은 카테고리 단일 풀
- 동점자도 신청 시각·ID 순서로 균등 분할 가능
- 병합 대상 그룹은 최소 인원 우선, 동률이면 높은 점수 그룹 우선
- 병합 이웃은 작은 인원 우선, 동률이면 높은 점수 이웃 우선
- 배정 인원 최대화 후 4인 팀 수 최대화
- 팀 크기는 탐색 전에 고정
- Brute Force와 Multi-start Greedy 혼합 방식 유지
- Brute Force timeout 시 부분 결과 폐기 후 Greedy fallback
- 리더 점수는 3인·4인 WANTS/NEUTRAL 실제 인원수 표 사용
- 유사성 모집단 분산은 `/4`로 정규화
- 외향성은 3인 10개·4인 15개 분포표 사용
- Greedy 앵커는 재배정 > WANTS > 그 외
- 전체 plan은 배정 수 > 재배정 수 > 평균 > WANTS 수 > 최저점 > 분산 > ID 순으로 비교
- 14시 실행, 15시 30분 새 작업 시작 중단 기준(best effort), 16시 공개
- 공개 전 결과 차단, 오늘 신청 우선 후 열린·확정된 이전 본인 결과 fallback
- JDBC ShedLock과 DB 유니크 제약 사용, Redis 분산 락 미사용
- stale `RUNNING`과 이전 신청일 미처리 배치의 자동 복구는 후속 운영 범위
- 신청일 다음 날 12시까지 수락·패스, 이후 미응답 자동 만료와 패널티
- 유효 수락자 3명 또는 4명이 모두 응답하면 실제 Team 생성과 그룹 `CONFIRMED`
- 패스·미응답으로 Team을 만들 수 없으면 피해자를 새 신청으로 자동 재배정
- 알림은 별도 담당 범위

## 16. 벤치마크 기록

2026-08-02 이전 구현은 고정 점수 구간과 4인 고정 팀을 기준으로 했다. 당시 합성 fixture에서는 12명
Brute Force 약 493ms, 16명 Greedy 약 313ms, 20명 Greedy 약 465ms였고 비교 fixture의 Greedy
손실률은 0%였다.

2026-08-03 정책은 탐색 공간과 풀 분포를 바꾸므로 위 수치를 운영 기본값의 최종 근거로 사용하지
않는다.

### 16.1 현재 정책 구현 후 재측정

2026-08-05 현재 정책의 합성 fixture와 개발 장비에서 다시 측정한 결과는 다음과 같다.

| 입력 | 결과 |
|---|---|
| 3~10명 | Greedy 평균 궁합 손실률 0% |
| 11명 | Greedy 평균 궁합 손실률 0.38% |
| 12명 | Greedy 평균 궁합 손실률 0% |
| 13·14·15·16·20명 Greedy | 약 192~653ms |
| 12명 유효 풀 24개 순차 Brute Force | 약 4,249ms |

모든 비교 입력에서 배정 인원수와 재배정 대상자 배정 수가 Brute Force와 같았고, 10개 fixture에서
p95 1%·최대 3% 품질 기준을 만족했다. 벤치마크 테스트 2개의 순수 테스트 실행시간은 약 7.4초였다.
따라서
`MATCHING_BRUTEFORCE_MAX_POOL_SIZE=12`,
`MATCHING_GREEDY_RESTART_COUNT=50`, `MATCHING_FALLBACK_RESERVE=PT10M` 기본값을 유지한다.

이 수치는 합성 fixture의 회귀 기준이지 운영 최대 부하 보장은 아니다. 운영 신청 분포와 실행 지표가
쌓이면 같은 `matchingBenchmark` 태스크로 임계값과 반복 횟수를 다시 검증한다.

### 16.2 자리 단위 Sequential Greedy 전환 후 재측정

2026-08-16 `refactor/#149`에서 알고리즘을 자리 단위 순차 선택으로 교체하고, Brute Force 상한을
12명에서 16명으로 확대했다. 개발 장비에서 재측정한 결과는 다음과 같다.

| 입력 | 결과 |
|---|---|
| 3~5명 참고 | Greedy 손실률 0% |
| 6~16명 참고 | Greedy 손실률 3.36~22.28% (Brute Force가 처리하므로 서비스 영향 없음) |
| 17명 게이트 | Greedy 손실률 3.36%, Brute Force 약 15초, Greedy 약 574ms |
| 18명 게이트 | Greedy 손실률 4.09%, Brute Force 약 62초, Greedy 약 585ms |
| 20명 Greedy 전용 | 약 549ms, 평균점수 88.80 |
| 100명 Greedy 전용 | 약 9.3초, 평균점수 88.72 |
| 1000명 Greedy 전용 | 약 16분 40초, 평균점수 88.67 |
| 16명 유효 풀 24개 순차 Brute Force | 약 68초 |

게이트 구간(17·18명) 최악 손실률이 4.09%로 5% 이하 기준을 만족했다. 순차 선택 알고리즘은 팀 하나
뽑을 때 조합을 전수검사하지 않으므로 이전 구현이 정지하던 100명 이상 풀도 감당한다. 다만 부분 점수가
리더 조화도·외향성 정책표를 반영하지 못해 작은 풀(6~16명) 손실률이 커지는 특성이 있어 이 구간은
확대된 Brute Force 상한이 최적해로 흡수한다.

이 결정에 따라 `MATCHING_BRUTEFORCE_MAX_POOL_SIZE=16`으로 기본값을 변경하고, `MATCHING_GREEDY_
RESTART_COUNT=50`, `MATCHING_FALLBACK_RESERVE=PT10M`은 그대로 유지한다.
