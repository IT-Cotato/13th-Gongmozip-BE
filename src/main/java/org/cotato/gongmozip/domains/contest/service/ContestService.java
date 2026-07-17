package org.cotato.gongmozip.domains.contest.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.contest.converter.ContestConverter;
import org.cotato.gongmozip.domains.contest.dto.request.ContestRequest.CreateContestRequest;
import org.cotato.gongmozip.domains.contest.dto.request.ContestRequest.UpdateContestRequest;
import org.cotato.gongmozip.domains.contest.dto.response.ContestResponse.*;
import org.cotato.gongmozip.domains.contest.entity.Contest;
import org.cotato.gongmozip.domains.contest.entity.ContestScrap;
import org.cotato.gongmozip.domains.contest.enums.ContestStatus;
import org.cotato.gongmozip.domains.contest.exception.ContestException;
import org.cotato.gongmozip.domains.contest.exception.codes.ContestErrorCode;
import org.cotato.gongmozip.domains.contest.repository.ContestRepository;
import org.cotato.gongmozip.domains.contest.repository.ContestScrapRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContestService {

    private final ContestRepository contestRepository;
    private final ContestScrapRepository contestScrapRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ContestCreateResponse createContest(CreateContestRequest request) {
        validateContestInput(
                request.isTeamParticipation(),
                request.minTeamSize(),
                request.maxTeamSize(),
                request.applyStartAt(),
                request.applyEndAt());

        // 중복 등록 방지
        if (contestRepository.existsByTitleAndApplyEndAt(request.title(), request.applyEndAt())) {
            throw new ContestException(ContestErrorCode.DUPLICATE_CONTEST);
        }

        Contest contest = ContestConverter.toContest(request);
        Contest saved = contestRepository.save(contest);
        return ContestConverter.toContestCreateResponse(saved);
    }

    @Transactional
    public ContestUpdateResponse updateContest(Long contestId, UpdateContestRequest request) {
        Contest contest = contestRepository
                .findById(contestId)
                .orElseThrow(() -> new ContestException(ContestErrorCode.CONTEST_NOT_FOUND));

        // 업데이트될 값 기준으로 유효성 검증 수행
        Boolean isTeam =
                request.isTeamParticipation() != null ? request.isTeamParticipation() : contest.isTeamParticipation();
        Integer minTeam = request.minTeamSize() != null ? request.minTeamSize() : contest.getMinTeamSize();
        Integer maxTeam = request.maxTeamSize() != null ? request.maxTeamSize() : contest.getMaxTeamSize();
        LocalDateTime startAt = request.applyStartAt() != null ? request.applyStartAt() : contest.getApplyStartAt();
        LocalDateTime endAt = request.applyEndAt() != null ? request.applyEndAt() : contest.getApplyEndAt();

        validateContestInput(isTeam, minTeam, maxTeam, startAt, endAt);

        InterestCategory category = null;
        if (request.category() != null) {
            category = ContestConverter.toInterestCategory(request.category());
        }

        ContestStatus status = null;
        if (request.status() != null) {
            status = ContestConverter.toContestStatus(request.status());
        }

        contest.update(
                request.title(),
                request.summary(),
                request.description(),
                category,
                status,
                request.hostName(),
                request.applyStartAt(),
                request.applyEndAt(),
                request.eligibilityText(),
                request.prizeText(),
                request.locationText(),
                request.thumbnailUrl(),
                request.sourceUrl(),
                request.isTeamParticipation(),
                request.minTeamSize(),
                request.maxTeamSize());

        return ContestConverter.toContestUpdateResponse(contest);
    }

    @Transactional
    public void deleteContest(Long contestId) {
        Contest contest = contestRepository
                .findById(contestId)
                .orElseThrow(() -> new ContestException(ContestErrorCode.CONTEST_NOT_FOUND));

        // TODO: 진행 중인 프로젝트 또는 팀에서 사용 중인 공모전인지 여부 검증 (추후 프로젝트/팀 도메인 연동 시 409 Conflict 처리 추가)

        contestScrapRepository.deleteAllByContest(contest);
        contestRepository.delete(contest);
    }

    @Transactional
    public ContestDetailResponse getContestDetail(Long contestId) {
        Contest contest = contestRepository
                .findById(contestId)
                .orElseThrow(() -> new ContestException(ContestErrorCode.CONTEST_NOT_FOUND));

        contestRepository.incrementViewCount(contestId);

        Contest updatedContest = contestRepository
                .findById(contestId)
                .orElseThrow(() -> new ContestException(ContestErrorCode.CONTEST_NOT_FOUND));

        return ContestConverter.toContestDetailResponse(updatedContest, LocalDateTime.now());
    }

    public ContestListResponse getContests(
            String keyword, String categoryStr, String statusStr, String sort, Integer page, Integer size) {
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;

        if (pageNum < 0 || pageSize < 1) {
            throw new ContestException(ContestErrorCode.INVALID_CONTEST_INPUT);
        }

        // 정렬 기준 유효성 검사 및 기본값 설정
        String sortCriteria = sort != null ? sort.trim() : "deadlineAsc";
        if (!sortCriteria.equals("deadlineAsc")
                && !sortCriteria.equals("deadlineDesc")
                && !sortCriteria.equals("newest")
                && !sortCriteria.equals("popular")) {
            throw new ContestException(ContestErrorCode.INVALID_CONTEST_INPUT);
        }

        // 카테고리 매핑 및 유효성 검사
        InterestCategory category = null;
        if (categoryStr != null && !categoryStr.trim().isEmpty()) {
            category = ContestConverter.toInterestCategory(categoryStr);
        }

        // 모집 상태 유효성 검사
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            String statusUpper = statusStr.trim().toUpperCase();
            if (!statusUpper.equals("UPCOMING") && !statusUpper.equals("OPEN") && !statusUpper.equals("CLOSED")) {
                throw new ContestException(ContestErrorCode.INVALID_CONTEST_INPUT);
            }
            statusStr = statusUpper;
        }

        Pageable pageable = PageRequest.of(pageNum, pageSize);
        LocalDateTime now = LocalDateTime.now();

        Page<Contest> contestPage =
                switch (sortCriteria) {
                    case "deadlineDesc" -> contestRepository.findAllWithFilterAndDeadlineDesc(
                            keyword, category, statusStr, now, pageable);
                    case "newest" -> contestRepository.findAllWithFilterAndNewest(
                            keyword, category, statusStr, now, pageable);
                    case "popular" -> contestRepository.findAllWithFilterAndPopular(
                            keyword, category, statusStr, now, pageable);
                    default -> contestRepository.findAllWithFilterAndDeadlineAsc(
                            keyword, category, statusStr, now, pageable);
                };

        return ContestConverter.toContestListResponse(contestPage, now);
    }

    @Transactional
    public ScrapResponse scrapContest(Long contestId, Long memberId) {
        Contest contest = contestRepository
                .findById(contestId)
                .orElseThrow(() -> new ContestException(ContestErrorCode.CONTEST_NOT_FOUND));

        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new org.cotato.gongmozip.domains.member.exception.MemberException(
                        org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode.MEMBER_NOT_FOUND));

        if (contestScrapRepository.existsByMemberAndContest(member, contest)) {
            throw new ContestException(ContestErrorCode.ALREADY_SCRAPPED);
        }

        ContestScrap scrap =
                ContestScrap.builder().member(member).contest(contest).build();

        ContestScrap saved = contestScrapRepository.save(scrap);
        return ContestConverter.toScrapResponse(saved);
    }

    @Transactional
    public void unscrapContest(Long contestId, Long memberId) {
        Contest contest = contestRepository
                .findById(contestId)
                .orElseThrow(() -> new ContestException(ContestErrorCode.CONTEST_NOT_FOUND));

        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new org.cotato.gongmozip.domains.member.exception.MemberException(
                        org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode.MEMBER_NOT_FOUND));

        ContestScrap scrap = contestScrapRepository
                .findByMemberAndContest(member, contest)
                .orElseThrow(() -> new ContestException(ContestErrorCode.SCRAP_NOT_FOUND));

        contestScrapRepository.delete(scrap);
    }

    public ScrapStatusResponse getScrapStatus(Long contestId, Long memberId) {
        Contest contest = contestRepository
                .findById(contestId)
                .orElseThrow(() -> new ContestException(ContestErrorCode.CONTEST_NOT_FOUND));

        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new org.cotato.gongmozip.domains.member.exception.MemberException(
                        org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode.MEMBER_NOT_FOUND));

        return contestScrapRepository
                .findByMemberAndContest(member, contest)
                .map(scrap -> ContestConverter.toScrapStatusResponse(contest, true, scrap.getCreatedAt()))
                .orElseGet(() -> ContestConverter.toScrapStatusResponse(contest, false, null));
    }

    public SharePreviewResponse getSharePreview(Long contestId) {
        Contest contest = contestRepository
                .findById(contestId)
                .orElseThrow(() -> new ContestException(ContestErrorCode.CONTEST_NOT_FOUND));

        return ContestConverter.toSharePreviewResponse(contest, LocalDateTime.now());
    }

    private void validateContestInput(
            boolean isTeam, Integer minTeam, Integer maxTeam, LocalDateTime startAt, LocalDateTime endAt) {
        if (isTeam) {
            if (minTeam != null && minTeam < 1) {
                throw new ContestException(ContestErrorCode.INVALID_CONTEST_INPUT);
            }
            if (minTeam != null && maxTeam != null && minTeam > maxTeam) {
                throw new ContestException(ContestErrorCode.INVALID_CONTEST_INPUT);
            }
        }
        if (startAt != null && endAt != null && startAt.isAfter(endAt)) {
            throw new ContestException(ContestErrorCode.INVALID_CONTEST_INPUT);
        }
    }
}
