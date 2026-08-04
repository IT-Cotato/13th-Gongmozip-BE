package org.cotato.gongmozip.domains.matching.service;

import static org.cotato.gongmozip.domains.matching.dto.response.MatchingApplicationResponse.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.collaboration.enums.CollaborationPointReason;
import org.cotato.gongmozip.domains.collaboration.repository.CollaborationPointHistoryRepository;
import org.cotato.gongmozip.domains.collaboration.service.CollaborationPointService;
import org.cotato.gongmozip.domains.matching.converter.MatchingApplicationConverter;
import org.cotato.gongmozip.domains.matching.dto.request.MatchingApplicationRequest.ApplyRequest;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingIneligibilityReason;
import org.cotato.gongmozip.domains.matching.enums.WithdrawalType;
import org.cotato.gongmozip.domains.matching.exception.MatchingException;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.matching.repository.MatchingApplicationRepository;
import org.cotato.gongmozip.domains.matching.score.ProjectScoreProvider;
import org.cotato.gongmozip.domains.matching.score.SkillScoreCalculator;
import org.cotato.gongmozip.domains.matching.vo.SkillScoreSnapshot;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.entity.ProjectExperience;
import org.cotato.gongmozip.domains.profile.repository.AwardRepository;
import org.cotato.gongmozip.domains.profile.repository.ProfileCertificationRepository;
import org.cotato.gongmozip.domains.profile.repository.ProfileRepository;
import org.cotato.gongmozip.domains.profile.repository.ProjectExperienceRepository;
import org.cotato.gongmozip.domains.survey.entity.SurveySubmission;
import org.cotato.gongmozip.domains.survey.enums.SubmissionStatus;
import org.cotato.gongmozip.domains.survey.repository.SurveySubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchingApplicationService {

    // 패스 정책: 최근 7일 패스 횟수마다 2m씩 증가하고 최대 11m까지만 차감한다
    private static final int FIRST_PASS_PENALTY = 3;
    private static final int PASS_PENALTY_STEP = 2;
    private static final int MAX_PASS_PENALTY = 11;
    private static final int PASS_REPEAT_WINDOW_DAYS = 7;

    // 참여 인원 집계 상태와 철회 가능한 상태는 목적이 달라 별도로 관리한다
    private static final EnumSet<MatchingApplicationStatus> PARTICIPATING_STATUSES =
            EnumSet.of(MatchingApplicationStatus.WAITING, MatchingApplicationStatus.MATCHING);
    private static final EnumSet<MatchingApplicationStatus> WITHDRAWABLE_STATUSES = EnumSet.of(
            MatchingApplicationStatus.WAITING, MatchingApplicationStatus.MATCHING, MatchingApplicationStatus.PROPOSED);

    private final MemberRepository memberRepository;
    private final ProfileRepository profileRepository;
    private final ProjectExperienceRepository projectExperienceRepository;
    private final AwardRepository awardRepository;
    private final ProfileCertificationRepository profileCertificationRepository;
    private final SurveySubmissionRepository surveySubmissionRepository;
    private final MatchingApplicationRepository matchingApplicationRepository;
    private final CollaborationPointHistoryRepository collaborationPointHistoryRepository;
    private final CollaborationPointService collaborationPointService;
    private final ProjectScoreProvider projectScoreProvider;
    private final SkillScoreCalculator skillScoreCalculator;
    private final MatchingTimePolicy matchingTimePolicy;

    // 신청 자격 조회 — 매칭 신청 조건 검사
    public EligibilityResponse getEligibility(Long memberId) {
        Member member = getMember(memberId);

        // 신청 시간 관련(14시 이전)
        LocalDate today = matchingTimePolicy.today();
        LocalDateTime now = matchingTimePolicy.now();

        // 프로필 개수
        boolean hasProfile = profileRepository.countByMember(member) > 0;
        // 협업 유형 검사 여부
        boolean surveyCompleted = surveySubmissionRepository
                .findByMember(member)
                .filter(submission -> submission.getStatus() == SubmissionStatus.SUBMITTED)
                .isPresent();
        // 오늘 매칭풀 입장했는지 여부
        boolean appliedToday = matchingApplicationRepository.existsByMemberAndApplicationDate(member, today);
        // 협업거리 제한
        boolean matchingRestricted = member.isMatchingBlockedAt(now);
        boolean applicationOpen = matchingTimePolicy.isApplicationOpen();

        // 모든 신청 불가 사유를 List로 반환
        List<MatchingIneligibilityReason> reasons = new ArrayList<>();
        if (!hasProfile) {
            reasons.add(MatchingIneligibilityReason.PROFILE_REQUIRED);
        }
        if (!surveyCompleted) {
            reasons.add(MatchingIneligibilityReason.SURVEY_REQUIRED);
        }
        if (!applicationOpen) {
            reasons.add(MatchingIneligibilityReason.APPLICATION_DEADLINE_PASSED);
        }
        if (appliedToday) {
            reasons.add(MatchingIneligibilityReason.ALREADY_APPLIED_TODAY);
        }
        if (matchingRestricted) {
            reasons.add(MatchingIneligibilityReason.MATCHING_RESTRICTED);
        }
        if (hasProfile && !hasAnyProjectEvaluationReadyProfile(member)) {
            reasons.add(MatchingIneligibilityReason.PROJECT_EVALUATION_NOT_READY);
        }

        long participantCount =
                matchingApplicationRepository.countByApplicationDateAndStatusIn(today, PARTICIPATING_STATUSES);
        return MatchingApplicationConverter.toEligibilityResponse(
                reasons,
                hasProfile,
                surveyCompleted,
                appliedToday,
                matchingRestricted ? member.getMatchingBlockedUntil() : null,
                matchingTimePolicy.applicationDeadline(today),
                participantCount);
    }

    // 오늘 신청 조회 — 신청이 없으면 예외 대신 appliedToday=false 응답을 반환한다
    public TodayApplicationResponse getTodayApplication(Long memberId) {
        Member member = getMember(memberId);
        return matchingApplicationRepository
                .findByMemberAndApplicationDate(member, matchingTimePolicy.today())
                .map(application -> MatchingApplicationConverter.toTodayApplicationResponse(
                        application, resolveWithdrawalAvailability(member, application)))
                .orElseGet(MatchingApplicationConverter::toEmptyTodayApplicationResponse);
    }

    // 매칭 신청 — 마감 검증 → 중복/자격 검증 → 역량 계산 → 신청 시점 스냅샷 저장 순으로 처리
    @Transactional
    public ApplicationResponse apply(Long memberId, ApplyRequest request) {
        // 락을 잡기 전에 마감 여부를 먼저 확인해 마감 이후 불필요한 DB 락을 피한다
        if (!matchingTimePolicy.isApplicationOpen()) {
            throw new MatchingException(MatchingErrorCode.APPLICATION_DEADLINE_PASSED);
        }

        // 비관적 락 적용
        Member member = getMemberWithLock(memberId);
        LocalDate applicationDate = matchingTimePolicy.today();
        LocalDateTime now = matchingTimePolicy.now();
        if (member.isMatchingBlockedAt(now)) {
            throw new MatchingException(MatchingErrorCode.MATCHING_RESTRICTED);
        }
        if (matchingApplicationRepository.existsByMemberAndApplicationDate(member, applicationDate)) {
            throw new MatchingException(MatchingErrorCode.ALREADY_APPLIED_TODAY);
        }
        if (profileRepository.countByMember(member) == 0) {
            throw new MatchingException(MatchingErrorCode.PROFILE_REQUIRED);
        }

        // 요청한 프로필의 소유권과 제출 완료된 협업 유형 검사를 함께 확인한다
        Profile profile = profileRepository
                .findByProfileIdAndMember(request.profileId(), member)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.PROFILE_ACCESS_DENIED));
        SurveySubmission submission = surveySubmissionRepository
                .findByMember(member)
                .filter(saved -> saved.getStatus() == SubmissionStatus.SUBMITTED)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.SURVEY_REQUIRED));

        // 모든 항목을 0~100 원점수로 변환한 뒤 첫 매칭 여부에 맞는 가중치를 적용한다
        List<ProjectExperience> projects = projectExperienceRepository.findAllByProfile(profile);
        BigDecimal projectScore = projectScoreProvider.evaluate(projects);
        // 협업거리 변경 이력이 없으면 초기 사용자로 보고 프로젝트 50%, 협업거리 10%를 적용한다
        boolean firstMatching = !collaborationPointHistoryRepository.existsByMember(member);
        SkillScoreSnapshot skillScore = skillScoreCalculator.calculate(
                profile,
                projectScore,
                awardRepository.countByProfile(profile),
                profileCertificationRepository.countByProfile(profile),
                member.getCollaborationPoint(),
                firstMatching);

        // 이후 프로필·설문·협업거리가 바뀌어도 당일 매칭 입력은 변하지 않도록 신청에 복사
        MatchingApplication application = MatchingApplicationConverter.toMatchingApplication(
                submission,
                profile,
                request.contestCategory(),
                request.leaderPreference(),
                applicationDate,
                skillScore,
                member.getCollaborationPoint());
        matchingApplicationRepository.save(application);
        return MatchingApplicationConverter.toApplicationResponse(
                application, matchingTimePolicy.applicationDeadline(applicationDate));
    }

    // 통합 철회 — 현재 시각을 기준 무료 취소와 패널티 패스(협업 거리 감소)를 결정
    @Transactional
    public WithdrawalResponse withdraw(Long memberId, Long applicationId) {
        // 회원과 신청을 같은 트랜잭션에서 잠가 중복 철회와 협업거리 중복 차감을 막는다
        Member member = getMemberWithLock(memberId);
        MatchingApplication application = matchingApplicationRepository
                .findByIdAndMemberIdWithLock(applicationId, memberId)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.APPLICATION_NOT_FOUND));
        if (!WITHDRAWABLE_STATUSES.contains(application.getStatus())) {
            throw new MatchingException(MatchingErrorCode.INVALID_APPLICATION_STATUS);
        }

        LocalDateTime now = matchingTimePolicy.now();
        WithdrawalType withdrawalType = matchingTimePolicy.resolveWithdrawalType(application.getApplicationDate());
        int penalty = 0;
        if (withdrawalType == WithdrawalType.FREE_CANCEL) {
            application.cancel(now);
        } else {
            // 패스 횟수는 신청 상태 이력으로 계산하고 실제 협업거리 변경은 공용 서비스에 위임한다
            penalty = calculateNextPassPenalty(member, now);
            collaborationPointService.changePoint(
                    member, null, CollaborationPointReason.MATCHING_PASS_PENALTY, -penalty);
            application.pass(now);
            // TODO: 임시 팀/매칭 결과 구현 시 패스하지 않은 나머지 팀원을 재배정 풀로 복귀
        }

        return MatchingApplicationConverter.toWithdrawalResponse(
                application, withdrawalType, penalty, member.getCollaborationPoint());
    }

    // =======내부 메서드=======

    // 현재 신청 상태와 시각을 기준으로 철회 가능 여부·방식·예상 감점을 결정한다
    private WithdrawalAvailability resolveWithdrawalAvailability(Member member, MatchingApplication application) {
        // 기본 설정(철회 불가)
        WithdrawalAvailability withdrawal = MatchingApplicationConverter.toUnavailableWithdrawalAvailability();
        if (WITHDRAWABLE_STATUSES.contains(application.getStatus())) {
            try {
                WithdrawalType type = matchingTimePolicy.resolveWithdrawalType(application.getApplicationDate());
                int penalty = type == WithdrawalType.PENALIZED_PASS
                        ? calculateNextPassPenalty(member, matchingTimePolicy.now())
                        : 0;
                LocalDateTime deadline = type == WithdrawalType.FREE_CANCEL
                        ? matchingTimePolicy.applicationDeadline(application.getApplicationDate())
                        : matchingTimePolicy.withdrawalDeadline(application.getApplicationDate());
                withdrawal = MatchingApplicationConverter.toWithdrawalAvailability(type, penalty, deadline);
            } catch (MatchingException ignored) {
                // 철회 가능 시간이 아니면 unavailable 응답을 유지한다.
            }
        }
        return withdrawal;
    }

    // 현재 요청까지 포함했을 때 적용할 다음 패스 감점을 계산한다
    private int calculateNextPassPenalty(Member member, LocalDateTime now) {
        long recentPassCount = matchingApplicationRepository.countByMemberAndStatusAndCanceledAtGreaterThanEqual(
                member, MatchingApplicationStatus.PASSED, now.minusDays(PASS_REPEAT_WINDOW_DAYS));
        long calculatedPenalty = FIRST_PASS_PENALTY + recentPassCount * PASS_PENALTY_STEP;
        return (int) Math.min(calculatedPenalty, MAX_PASS_PENALTY);
    }

    private boolean hasAnyProjectEvaluationReadyProfile(Member member) {
        List<Profile> profiles = profileRepository.findAllByMemberOrderByUpdatedAtDesc(member);
        // 단위 테스트의 불완전한 mock과 레거시 불일치에는 실제 신청 단계의 강한 검증을 최종 방어로 둔다.
        if (profiles.isEmpty()) {
            return true;
        }
        return profiles.stream()
                .anyMatch(
                        profile -> projectScoreProvider.isReady(projectExperienceRepository.findAllByProfile(profile)));
    }

    private Member getMember(Long memberId) {
        return memberRepository
                .findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    // 신청/철회 쓰기 작업에서 사용할 회원 조회 — 동시 요청을 막기 위해 비관적 락을 사용한다
    private Member getMemberWithLock(Long memberId) {
        return memberRepository
                .findByIdWithLock(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
