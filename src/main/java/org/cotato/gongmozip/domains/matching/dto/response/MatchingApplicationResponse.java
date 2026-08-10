package org.cotato.gongmozip.domains.matching.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.matching.enums.MatchingIneligibilityReason;
import org.cotato.gongmozip.domains.matching.enums.WithdrawalType;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;

public class MatchingApplicationResponse {

    private MatchingApplicationResponse() {}

    // 홈 화면에 표시하는 현재 매칭 신청 인원과 카운트다운 기준 시각
    public record ParticipantCountResponse(
            // 대상 신청일 매칭풀에서 대기하거나 매칭 계산 중인 신청 수
            long participantCount,
            // 대상 신청일의 신청 마감 시각(14시) — 마감 카운트다운 기준
            LocalDateTime applicationDeadlineAt,
            // 대상 신청일의 매칭 결과 공개 시각(16시) — 결과 발표 카운트다운 기준
            LocalDateTime resultPublishAt,
            // 응답 생성 시점의 서버 시각 — 클라이언트 시계 오차 보정용
            LocalDateTime serverTime) {}

    // 매칭 신청 화면에 들어가기 전 필요한 자격 조건과 현재 매칭풀 현황
    public record EligibilityResponse(
            // 아래 신청 조건을 모두 만족하는 경우에만 true
            boolean eligible,
            // 신청할 수 없을 때 만족하지 못한 조건들을 모두 반환하며 신청 가능하면 빈 목록
            List<MatchingIneligibilityReason> reasons,
            // 회원이 작성한 프로필을 하나 이상 가지고 있는지 여부
            boolean hasProfile,
            // 협업 유형 검사를 끝까지 제출했는지 여부
            boolean surveyCompleted,
            // 취소나 패스를 포함하여 대상 신청일에 이미 신청한 이력이 있는지 여부
            boolean appliedToday,
            // 최근 협업거리 감소로 매칭이 제한된 경우 제한 종료 시각, 제한 중이 아니면 null
            LocalDateTime matchingBlockedUntil,
            // 지금 신청하면 참여하게 되는 매칭 날짜 — 16시 결과 공개 이후에는 다음 날이 된다
            LocalDate applicationDate,
            // 대상 신청일의 매칭 신청을 받을 수 있는 마지막 시각
            LocalDateTime applicationDeadlineAt,
            // 대상 신청일 매칭풀에서 대기하거나 매칭 계산 중인 신청 수
            long participantCount) {}

    // 신청 완료 직후 저장된 매칭 신청의 스냅샷을 반환
    public record ApplicationResponse(
            // 생성된 매칭 신청 식별자
            Long applicationId,
            // 생성 직후 신청 상태이며 기본값은 WAITING
            String status,
            // 한국 시간을 기준으로 신청한 날짜
            LocalDate applicationDate,
            // 신청 시 선택한 공모전 카테고리
            InterestCategory contestCategory,
            // 신청 시 선택한 팀장 희망 여부
            LeaderPreference leaderPreference,
            // 프로필과 협업거리를 바탕으로 계산한 신청 시점 역량 총점
            BigDecimal skillScore,
            // 역량 총점에 따라 분류된 1~4 그룹
            Integer skillGroup,
            // 이후 회원 값이 바뀌어도 유지되는 신청 시점 협업거리
            int collaborationDistance,
            // 해당 신청일의 신청 마감 시각
            LocalDateTime applicationDeadlineAt) {}

    // 오늘 신청 내역과 현재 시각을 기준으로 가능한 철회 방식을 함께 반환
    // 신청하지 않은 경우 신청 정보 필드를 null로 반환
    public record TodayApplicationResponse(
            // 오늘 신청 이력이 있으면 true
            boolean appliedToday,
            // 오늘 신청이 없으면 null
            Long applicationId,
            // 신청이 없으면 NONE, 있으면 현재 MatchingApplicationStatus 이름
            String status,
            LocalDate applicationDate,
            InterestCategory contestCategory,
            LeaderPreference leaderPreference,
            BigDecimal skillScore,
            Integer skillGroup,
            Integer collaborationDistance,
            // 철회 가능 여부와 무료 취소 또는 패널티 패스 예상 결과
            WithdrawalAvailability withdrawal) {}

    // 철회 요청을 보내기 전에 프론트가 버튼 상태와 예상 페널티를 결정할 수 있는 정보
    public record WithdrawalAvailability(
            // 현재 철회 API를 호출할 수 있는 상태와 시간인지 여부
            boolean withdrawable, WithdrawalType type, int expectedPenalty, LocalDateTime deadlineAt) {}

    // 하나의 철회 API가 현재 시각에 따라 무료 취소 또는 패널티 패스를 처리한 결과
    public record WithdrawalResponse(
            Long applicationId,
            // 처리 후 CANCELED 또는 PASSED 상태
            String status,
            // 서버가 판정한 무료 취소 또는 패널티 패스 유형
            WithdrawalType withdrawalType,
            // 이번 요청으로 실제 차감된 협업거리이며 무료 취소이면 0
            int collaborationPenalty,
            // 차감 처리까지 반영된 회원의 현재 협업거리
            int currentCollaborationDistance) {}
}
