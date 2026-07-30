# 05. 사용자 신고

## 배경/목적

채팅방 내 팀원을 신고하는 기능.

## 엔티티 · 필드 정의

### Report

| 필드 | 설명 |
|---|---|
| reporter | `Member` FK |
| reportedMember | `Member` FK |
| team | FK, nullable — 신고가 발생한 팀 컨텍스트 |
| reasonCode | `ReportReason`: `FREE_RIDING`(무임승차), `GHOSTING`(잠수,연락두절), `ABUSIVE_LANGUAGE`(욕설,비하발언), `SPAM`(스팸), `FAKE_PROFILE`(허위프로필), `OTHER`(기타) |
| customReasonText | nullable, `OTHER`일 때만 필수 |
| createdAt | |

## 결정사항

- 신고 사유는 드롭다운 단일 선택. `OTHER` 선택 시 하단에 텍스트 입력 필드 노출, 제출 시 필수값.
- 신고 후 별도 상태(PENDING/REVIEWED) 관리는 이번 스코프에 포함하지 않음 — 필요 시 후속으로
  `Report.status` 컬럼 추가.

## 구현 현황 (Phase 2 완료)

- 엔티티/리포지토리: `domains/report/entity/Report.java`, `ReportRepository.java`
- 서비스: `domains/report/service/ReportService.java` — 본인 신고 차단, `OTHER` 사유 시
  직접입력 필수 검증 포함
- 컨트롤러: `POST /api/reports`
- 테스트: `ReportServiceTest`

## 미정 / 추후 확인 필요

- 신고 접수 후 운영 프로세스(관리자 확인 화면 등)는 이번 스코프 밖.

## 관련 화면

5.1.2.2 팀원 신고, 팀원 신고_직접입력
