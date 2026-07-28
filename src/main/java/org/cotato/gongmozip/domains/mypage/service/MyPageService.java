package org.cotato.gongmozip.domains.mypage.service;

import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.CurrentCharacterResponse;
import org.cotato.gongmozip.domains.character.service.CharacterService;
import org.cotato.gongmozip.domains.contest.entity.ContestScrap;
import org.cotato.gongmozip.domains.contest.repository.ContestScrapRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.mypage.converter.MyPageConverter;
import org.cotato.gongmozip.domains.mypage.dto.response.MyPageResponse.*;
import org.cotato.gongmozip.domains.mypage.exception.MyPageException;
import org.cotato.gongmozip.domains.mypage.exception.codes.MyPageErrorCode;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.repository.ProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final MemberRepository memberRepository;
    private final ProfileRepository profileRepository;
    private final ContestScrapRepository contestScrapRepository;
    private final CharacterService characterService;

    public MyPageMainResponse getMyPageMain(Long memberId) {
        Member member = getMember(memberId);

        // 대표 프로필 조회 (isMain = true), 없을 경우 첫 번째 프로필 조회
        Profile mainProfile = profileRepository
                .findByMemberAndIsMainTrue(member)
                .orElseGet(() -> profileRepository
                        .findFirstByMemberOrderByCreatedAtAsc(member)
                        .orElse(null));

        int scrapCount = contestScrapRepository.countByMember(member);
        int ongoingProjectCount = 0;
        int completedProjectCount = 0;
        int reviewCount = 0;
        CurrentCharacterResponse character =
                characterService.findCurrentCharacter(member).orElse(null);

        return MyPageConverter.toMyPageMainResponse(
                mainProfile, character, scrapCount, ongoingProjectCount, completedProjectCount, reviewCount);
    }

    public OngoingProjectsResponse getOngoingProjects(Long memberId, Integer page, Integer size) {
        validateMemberExists(memberId);
        validatePagingParameters(page, size);

        return MyPageConverter.toOngoingProjectsResponse(page, size);
    }

    public CompletedProjectsResponse getCompletedProjects(Long memberId, Integer page, Integer size) {
        validateMemberExists(memberId);
        validatePagingParameters(page, size);

        return MyPageConverter.toCompletedProjectsResponse(page, size);
    }

    public ReviewStatisticsResponse getReviewStatistics(Long memberId) {
        validateMemberExists(memberId);

        return MyPageConverter.toReviewStatisticsResponse();
    }

    public ScrappedContestsResponse getScrappedContests(Long memberId, Integer page, Integer size) {
        Member member = getMember(memberId);
        validatePagingParameters(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ContestScrap> scrapPage = contestScrapRepository.findAllByMemberWithContest(member, pageable);

        return MyPageConverter.toScrappedContestsResponse(scrapPage);
    }

    private Member getMember(Long memberId) {
        return memberRepository
                .findById(memberId)
                .orElseThrow(() -> new MyPageException(MyPageErrorCode.MEMBER_NOT_FOUND));
    }

    private void validateMemberExists(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new MyPageException(MyPageErrorCode.MEMBER_NOT_FOUND);
        }
    }

    private void validatePagingParameters(Integer page, Integer size) {
        if (page == null || size == null || page < 0 || size <= 0) {
            throw new MyPageException(MyPageErrorCode.INVALID_PAGE_INFO);
        }
    }
}
