package org.cotato.gongmozip.domains.member.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.domains.auth.enums.AuthProvider;
import org.cotato.gongmozip.domains.auth.repository.AuthAccountRepository;
import org.cotato.gongmozip.domains.auth.service.AuthService;
import org.cotato.gongmozip.domains.character.repository.MemberCharacterRepository;
import org.cotato.gongmozip.domains.collaboration.repository.CollaborationPointHistoryRepository;
import org.cotato.gongmozip.domains.contest.repository.ContestScrapRepository;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
import org.cotato.gongmozip.domains.matching.repository.MatchingApplicationRepository;
import org.cotato.gongmozip.domains.member.dto.request.MemberRequest.WithdrawMemberRequest;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.entity.MemberWithdrawalReason;
import org.cotato.gongmozip.domains.member.enums.MemberStatus;
import org.cotato.gongmozip.domains.member.enums.WithdrawalReasonType;
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.member.repository.MemberWithdrawalReasonRepository;
import org.cotato.gongmozip.domains.profile.repository.ProfileRepository;
import org.cotato.gongmozip.domains.survey.repository.SurveyAnswerRepository;
import org.cotato.gongmozip.domains.survey.repository.SurveySubmissionRepository;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.domains.upload.service.S3Service;
import org.cotato.gongmozip.global.redis.RedisUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberWithdrawService {

    public static final int REJOIN_RESTRICTION_DAYS = 14;
    public static final String ANONYMIZED_NICKNAME = "알 수 없음";

    private static final String LOGIN_FAILURE_PREFIX = "login:failure:";

    // 배치가 계산 중이거나 결과 제안이 진행 중인 신청은 탈퇴로 건드리면 팀 조합이 깨지므로 탈퇴를 차단한다
    private static final List<MatchingApplicationStatus> ACTIVE_APPLICATION_STATUSES = List.of(
            MatchingApplicationStatus.WAITING,
            MatchingApplicationStatus.MATCHING,
            MatchingApplicationStatus.PROPOSED,
            MatchingApplicationStatus.REASSIGN_PENDING);

    private static final List<TeamStatus> FINISHED_TEAM_STATUSES = List.of(TeamStatus.SUBMITTED, TeamStatus.COMPLETED);

    private final MemberRepository memberRepository;
    private final MemberWithdrawalReasonRepository memberWithdrawalReasonRepository;
    private final AuthAccountRepository authAccountRepository;
    private final MatchingApplicationRepository matchingApplicationRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProfileRepository profileRepository;
    private final ContestScrapRepository contestScrapRepository;
    private final SurveySubmissionRepository surveySubmissionRepository;
    private final SurveyAnswerRepository surveyAnswerRepository;
    private final MemberCharacterRepository memberCharacterRepository;
    private final CollaborationPointHistoryRepository collaborationPointHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final RedisUtil redisUtil;
    private final S3Service s3Service;
    private final Clock clock;

    @Transactional
    // 회원 탈퇴 메서드
    public void withdraw(Long memberId, String accessToken, WithdrawMemberRequest request) {
        // 회원 행을 잠가 탈퇴 진행 중 매칭 신청 등 동시 요청이 끼어들지 못하게 한다
        Member member = memberRepository
                .findByIdWithLock(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 이미 탈퇴한 회원인 경우
        if (member.isWithdrawn()) {
            throw new MemberException(MemberErrorCode.WITHDRAWN_MEMBER);
        }

        validatePassword(member, request.password());
        validateNoOngoingTeam(memberId);
        cancelWaitingApplications(member);

        // 탈퇴 사유는 통계용으로 회원과 분리해 영구 보존한다
        memberWithdrawalReasonRepository.save(MemberWithdrawalReason.builder()
                .reason(request.reason())
                .reasonDetail(request.reason() == WithdrawalReasonType.ETC ? request.reasonDetail() : null)
                .build());

        deletePersonalData(member);

        String profileImageUrl = member.getProfileImageUrl();
        member.withdraw(LocalDateTime.now(clock));

        registerAfterCommitCleanup(memberId, accessToken, profileImageUrl);
    }

    // 재가입 제한 기간이 지난 탈퇴 회원의 개인정보를 익명화하는 배치 메서드
    @Transactional
    public int anonymizeExpiredWithdrawnMembers(LocalDateTime now) {
        List<Member> targets = memberRepository.findAllByStatusAndAnonymizedFalseAndWithdrawnAtBefore(
                MemberStatus.WITHDRAWN, now.minusDays(REJOIN_RESTRICTION_DAYS));

        for (Member member : targets) {
            member.anonymize();
            profileRepository
                    .findAllByMemberOrderByUpdatedAtDesc(member)
                    .forEach(profile -> profile.updateNickname(ANONYMIZED_NICKNAME));
        }
        return targets.size();
    }

    // 이메일 가입 회원만 비밀번호를 검증하고 소셜 전용 회원은 통과시킨다
    private void validatePassword(Member member, String password) {
        if (!authAccountRepository.existsByMemberAndProvider(member, AuthProvider.EMAIL)) {
            return;
        }
        if (password == null || password.isBlank()) {
            throw new MemberException(MemberErrorCode.WITHDRAW_PASSWORD_REQUIRED);
        }
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new MemberException(MemberErrorCode.WITHDRAW_INVALID_PASSWORD);
        }
    }

    // 진행 중인 팀 프로젝트가 있으면 탈퇴를 차단한다
    private void validateNoOngoingTeam(Long memberId) {
        int ongoingCount =
                teamMemberRepository.countOngoingProjects(memberId, TeamMemberStatus.ACTIVE, FINISHED_TEAM_STATUSES);
        if (ongoingCount > 0) {
            throw new MemberException(MemberErrorCode.WITHDRAWAL_BLOCKED_BY_ACTIVE_TEAM);
        }
    }

    // 대기 중인 매칭 신청은 자동 철회하고, 배치 진행 중인 신청이 있으면 탈퇴를 차단한다
    private void cancelWaitingApplications(Member member) {
        // 배치 준비·계산과 경합하지 않도록 행을 잠근 뒤 최신 상태로 차단 여부를 판단한다
        List<MatchingApplication> applications =
                matchingApplicationRepository.findAllByMemberAndStatusInWithLock(member, ACTIVE_APPLICATION_STATUSES);

        boolean hasBlockingApplication = applications.stream()
                .anyMatch(application -> application.getStatus() != MatchingApplicationStatus.WAITING);
        if (hasBlockingApplication) {
            throw new MemberException(MemberErrorCode.WITHDRAWAL_BLOCKED_BY_MATCHING);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        applications.forEach(application -> application.cancel(now));
    }

    // 팀·매칭 이력 등 다른 회원과 공유되는 데이터는 유지하고 순수 개인 데이터만 삭제한다
    private void deletePersonalData(Member member) {
        authAccountRepository.deleteAllByMember(member);
        contestScrapRepository.deleteAllByMember(member);
        memberCharacterRepository.deleteAllByMember(member);
        collaborationPointHistoryRepository.deleteAllByMember(member);
        surveySubmissionRepository.findByMember(member).ifPresent(submission -> {
            surveyAnswerRepository.deleteAllBySubmission(submission);
            surveySubmissionRepository.delete(submission);
        });

        // 프로필은 매칭 신청·팀원 스냅샷이 참조하고 있어 삭제 대신 비공개 처리한다
        profileRepository.findAllByMemberOrderByUpdatedAtDesc(member).forEach(profile -> profile.setPublic(false));
    }

    // DB 커밋 성공 후에만 토큰·S3 정리 (커밋 실패 시 세션이 의도치 않게 무효화되는 것을 방지)
    private void registerAfterCommitCleanup(Long memberId, String accessToken, String profileImageUrl) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 로그아웃과 동일하게 access 토큰 블랙리스트 등록 + refresh 토큰 삭제
                authService.logout(memberId, accessToken);
                redisUtil.delete(LOGIN_FAILURE_PREFIX + memberId);
                if (profileImageUrl != null) {
                    s3Service.deleteFile(profileImageUrl);
                }
            }
        });
    }
}
