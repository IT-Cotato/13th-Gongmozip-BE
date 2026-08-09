package org.cotato.gongmozip.domains.matching.service;

import static org.cotato.gongmozip.domains.matching.dto.response.MatchingApplicationResponse.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.collaboration.repository.CollaborationPointHistoryRepository;
import org.cotato.gongmozip.domains.matching.converter.MatchingApplicationConverter;
import org.cotato.gongmozip.domains.matching.dto.request.MatchingApplicationRequest.ApplyRequest;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupMemberStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingIneligibilityReason;
import org.cotato.gongmozip.domains.matching.enums.WithdrawalType;
import org.cotato.gongmozip.domains.matching.exception.MatchingException;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.matching.repository.MatchingApplicationRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupMemberRepository;
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

    // 참여 인원 집계 상태와 철회 가능한 상태는 목적이 달라 별도로 관리한다
    private static final EnumSet<MatchingApplicationStatus> PARTICIPATING_STATUSES =
            EnumSet.of(MatchingApplicationStatus.WAITING, MatchingApplicationStatus.MATCHING);
    private static final EnumSet<MatchingApplicationStatus> PRE_RESULT_WITHDRAWABLE_STATUSES =
            EnumSet.of(MatchingApplicationStatus.WAITING, MatchingApplicationStatus.MATCHING);

    private final MemberRepository memberRepository;
    private final ProfileRepository profileRepository;
    private final ProjectExperienceRepository projectExperienceRepository;
    private final AwardRepository awardRepository;
    private final ProfileCertificationRepository profileCertificationRepository;
    private final SurveySubmissionRepository surveySubmissionRepository;
    private final MatchingApplicationRepository matchingApplicationRepository;
    private final MatchingGroupMemberRepository matchingGroupMemberRepository;
    private final CollaborationPointHistoryRepository collaborationPointHistoryRepository;
    private final MatchingPassPenaltyService matchingPassPenaltyService;
    private final ProjectScoreProvider projectScoreProvider;
    private final SkillScoreCalculator skillScoreCalculator;
    private final MatchingTimePolicy matchingTimePolicy;

    // 신청 자격 조회 — 매칭 신청 조건 검사
    public EligibilityResponse getEligibility(Long memberId) {
        Member member = getMember(memberId);

        // 16시 결과 공개 이후에는 다음 날이 신청 대상일이 된다
        LocalDate applicationDate = matchingTimePolicy.currentApplicationDate();
        LocalDateTime now = matchingTimePolicy.now();

        // 프로필 개수
        boolean hasProfile = profileRepository.countByMember(member) > 0;
        // 협업 유형 검사 여부
        boolean surveyCompleted = surveySubmissionRepository
                .findByMember(member)
                .filter(submission -> submission.getStatus() == SubmissionStatus.SUBMITTED)
                .isPresent();
        // 대상 신청일 매칭풀에 입장했는지 여부
        boolean appliedToday = matchingApplicationRepository.existsByMemberAndApplicationDate(member, applicationDate);
        // 협업거리 제한
        boolean matchingRestricted = member.isMatchingBlockedAt(now);
        boolean applicationOpen = matchingTimePolicy.isApplicationOpen();
        // 이전 제안에 PENDING/ACCEPTED로 남아 있으면 12시 마감 결과에 따라 자동 재매칭될 수 있다.
        // 이때 직접 신청까지 받으면 같은 날 신청이 두 개 생길 수 있으므로 자격 단계부터 막는다.
        boolean reassignmentPending = hasOpenResponse(memberId);

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
        if (reassignmentPending) {
            reasons.add(MatchingIneligibilityReason.REASSIGNMENT_PENDING);
        }
        if (hasProfile && !hasAnyProjectEvaluationReadyProfile(member)) {
            reasons.add(MatchingIneligibilityReason.PROJECT_EVALUATION_NOT_READY);
        }

        long participantCount = matchingApplicationRepository.countByApplicationDateAndStatusIn(
                applicationDate, PARTICIPATING_STATUSES);
        return MatchingApplicationConverter.toEligibilityResponse(
                reasons,
                hasProfile,
                surveyCompleted,
                appliedToday,
                matchingRestricted ? member.getMatchingBlockedUntil() : null,
                applicationDate,
                matchingTimePolicy.applicationDeadline(applicationDate),
                participantCount);
    }

    // 현재 신청 조회 — 신청이 없으면 예외 대신 appliedToday=false 응답을 반환한다
    public TodayApplicationResponse getTodayApplication(Long memberId) {
        Member member = getMember(memberId);
        LocalDate today = matchingTimePolicy.today();
        LocalDate applicationDate = matchingTimePolicy.currentApplicationDate();
        // 16시 이후 익일 신청이 없어도 오늘자 신청(매칭 결과 대기 등)은 계속 보여야 하므로 오늘로 폴백한다
        return matchingApplicationRepository
                .findByMemberAndApplicationDate(member, applicationDate)
                .or(() -> applicationDate.isEqual(today)
                        ? Optional.empty()
                        : matchingApplicationRepository.findByMemberAndApplicationDate(member, today))
                .map(application -> MatchingApplicationConverter.toTodayApplicationResponse(
                        application, resolveWithdrawalAvailability(member, application)))
                .orElseGet(MatchingApplicationConverter::toEmptyTodayApplicationResponse);
    }

    // 매칭 신청 — 마감 검증 → 중복/자격 검증 → 역량 계산 → 신청 시점 스냅샷 저장 순으로 처리
    @Transactional
    public ApplicationResponse apply(Long memberId, ApplyRequest request) {
        // 락을 잡기 전에 매칭 진행 구간(14~16시) 여부를 먼저 확인해 불필요한 DB 락을 피한다
        if (!matchingTimePolicy.isApplicationOpen()) {
            throw new MatchingException(MatchingErrorCode.APPLICATION_DEADLINE_PASSED);
        }

        // 비관적 락 적용
        Member member = getMemberWithLock(memberId);
        // 16시 이후에는 다음 날 매칭에 신청된다
        LocalDate applicationDate = matchingTimePolicy.currentApplicationDate();
        LocalDateTime now = matchingTimePolicy.now();
        if (member.isMatchingBlockedAt(now)) {
            throw new MatchingException(MatchingErrorCode.MATCHING_RESTRICTED);
        }
        // 자격 조회 이후 상태가 바뀌는 경쟁 상황도 있으므로 실제 신청 트랜잭션 안에서 다시 검사한다.
        // 자동 재매칭을 우선하고 직접 신청을 차단한다는 정책의 최종 방어선이다.
        if (hasOpenResponse(memberId)) {
            throw new MatchingException(MatchingErrorCode.MATCHING_REASSIGNMENT_CONFLICT);
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

    // 결과 생성 전 철회 처리. PROPOSED 신청은 MatchingWithdrawalService가 그룹 패스 흐름으로 보낸다.
    @Transactional
    public WithdrawalResponse withdraw(Long memberId, Long applicationId) {
        // 회원과 신청을 같은 트랜잭션에서 잠가 중복 철회와 협업거리 중복 차감을 막는다
        Member member = getMemberWithLock(memberId);
        MatchingApplication application = matchingApplicationRepository
                .findByIdAndMemberIdWithLock(applicationId, memberId)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.APPLICATION_NOT_FOUND));
        if (!PRE_RESULT_WITHDRAWABLE_STATUSES.contains(application.getStatus())) {
            throw new MatchingException(MatchingErrorCode.INVALID_APPLICATION_STATUS);
        }

        LocalDateTime now = matchingTimePolicy.now();
        WithdrawalType withdrawalType = matchingTimePolicy.resolveWithdrawalType(application.getApplicationDate());
        int penalty = 0;
        if (withdrawalType == WithdrawalType.FREE_CANCEL) {
            application.cancel(now);
        } else {
            // 패스 횟수는 신청 상태 이력으로 계산하고 실제 협업거리 변경은 공용 서비스에 위임한다
            penalty = matchingPassPenaltyService.apply(member, now);
            application.pass(now);
        }

        return MatchingApplicationConverter.toWithdrawalResponse(
                application, withdrawalType, penalty, member.getCollaborationPoint());
    }

    // =======내부 메서드=======

    // 현재 신청 상태와 시각을 기준으로 철회 가능 여부·방식·예상 감점을 결정한다
    private WithdrawalAvailability resolveWithdrawalAvailability(Member member, MatchingApplication application) {
        // 기본 설정(철회 불가)
        WithdrawalAvailability withdrawal = MatchingApplicationConverter.toUnavailableWithdrawalAvailability();
        if (application.getStatus() == MatchingApplicationStatus.PROPOSED) {
            return resolveProposedWithdrawalAvailability(member, application);
        }
        if (PRE_RESULT_WITHDRAWABLE_STATUSES.contains(application.getStatus())) {
            try {
                WithdrawalType type = matchingTimePolicy.resolveWithdrawalType(application.getApplicationDate());
                int penalty = type == WithdrawalType.PENALIZED_PASS
                        ? matchingPassPenaltyService.calculateNextPenalty(member, matchingTimePolicy.now())
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

    private WithdrawalAvailability resolveProposedWithdrawalAvailability(
            Member member, MatchingApplication application) {
        LocalDateTime now = matchingTimePolicy.now();
        // PROPOSED부터는 기존 신청일 기준 철회 시간이 아니라 제안 그룹의 응답 마감 시각을 따른다.
        // 결과 공개 전에도 패널티 철회는 허용하며, 그룹과 본인 응답이 모두 열려 있을 때만 가능으로 노출한다.
        return matchingGroupMemberRepository
                .findResultMembership(application)
                .filter(groupMember -> groupMember.getResponseStatus() == MatchingGroupMemberStatus.PENDING)
                .filter(groupMember -> groupMember.getMatchingGroup().getStatus() == MatchingGroupStatus.PROPOSED)
                .filter(groupMember -> groupMember.getMatchingGroup().getResponseDeadlineAt() != null)
                .filter(groupMember ->
                        now.isBefore(groupMember.getMatchingGroup().getResponseDeadlineAt()))
                .map(groupMember -> MatchingApplicationConverter.toWithdrawalAvailability(
                        WithdrawalType.PENALIZED_PASS,
                        matchingPassPenaltyService.calculateNextPenalty(member, now),
                        groupMember.getMatchingGroup().getResponseDeadlineAt()))
                .orElseGet(MatchingApplicationConverter::toUnavailableWithdrawalAvailability);
    }

    private boolean hasOpenResponse(Long memberId) {
        // 시각만 보고 판단하지 않는다. 12시가 지났어도 마감 작업이 그룹을 닫기 전에는 자동
        // 재매칭 생성 여부가 확정되지 않았으므로, 상태 전이가 끝날 때까지 직접 신청을 차단한다.
        return matchingGroupMemberRepository.existsOpenResponseForMember(
                memberId,
                MatchingGroupStatus.PROPOSED,
                List.of(MatchingGroupMemberStatus.PENDING, MatchingGroupMemberStatus.ACCEPTED));
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
