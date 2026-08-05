package org.cotato.gongmozip.domains.contest.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.domains.contest.dto.response.ContestResponse.ContestSummaryResponse;
import org.cotato.gongmozip.domains.contest.dto.response.RecommendationResponse.RecommendationReasonResponse;
import org.cotato.gongmozip.domains.contest.entity.Contest;
import org.cotato.gongmozip.domains.contest.exception.ContestException;
import org.cotato.gongmozip.domains.contest.exception.codes.ContestErrorCode;
import org.cotato.gongmozip.domains.contest.repository.ContestRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.profile.exception.ProfileException;
import org.cotato.gongmozip.domains.profile.exception.codes.ProfileErrorCode;
import org.cotato.gongmozip.domains.profile.repository.ProfileRepository;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
import org.cotato.gongmozip.domains.survey.repository.SurveySubmissionRepository;
import org.cotato.gongmozip.global.ai.AiClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContestRecommendationService {

    private final MemberRepository memberRepository;
    private final ProfileRepository profileRepository;
    private final ContestRepository contestRepository;
    private final SurveySubmissionRepository surveySubmissionRepository;
    private final AiClient aiClient;

    public List<ContestSummaryResponse> getHomeRecommendations(Long memberId) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        Profile representProfile = getRepresentProfile(member);
        return getRecommendationsForProfile(representProfile);
    }

    public List<ContestSummaryResponse> getProfileRecommendations(Long profileId, Long memberId) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        Profile profile = profileRepository
                .findById(profileId)
                .orElseThrow(() -> new ProfileException(ProfileErrorCode.PROFILE_NOT_FOUND));
        if (!profile.getMember().getMemberId().equals(member.getMemberId())) {
            throw new ProfileException(ProfileErrorCode.PROFILE_ACCESS_DENIED);
        }
        return getRecommendationsForProfile(profile);
    }

    public RecommendationReasonResponse getRecommendationReason(Long contestId, Long memberId) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        Contest contest = contestRepository
                .findById(contestId)
                .orElseThrow(() -> new ContestException(ContestErrorCode.CONTEST_NOT_FOUND));

        Profile representProfile = getRepresentProfile(member);

        List<ContestSummaryResponse> recommendations = getRecommendationsForProfile(representProfile);
        boolean isRecommended =
                recommendations.stream().anyMatch(r -> r.contestId().equals(contestId));
        if (!isRecommended) {
            throw new ContestException(ContestErrorCode.NOT_RECOMMENDED_CONTEST);
        }

        InterestCategory category =
                representProfile.getInterestCategories().stream().findFirst().orElse(InterestCategory.IT_AI_TECH);

        String characterName = surveySubmissionRepository
                .findByMember(member)
                .filter(s -> s.getStatus() == org.cotato.gongmozip.domains.survey.enums.SubmissionStatus.SUBMITTED)
                .map(s -> s.getCharacterType())
                .map(this::getKoreanCharacterName)
                .orElse("알 수 없는 러너");

        String reason = String.format(
                "이 공모전은 귀하의 선호 분야인 '%s' 카테고리에 속해 있으며, " + "대학생 팀 매칭 설문을 통해 분석된 귀하의 협업 캐릭터 '%s'의 성향과 목표에 "
                        + "매우 부합하여 AI에 의해 강력하게 추천되었습니다. 팀을 빌딩하여 프로젝트의 완성도를 높여보세요!",
                getKoreanCategoryName(category), characterName);

        return new RecommendationReasonResponse(reason);
    }

    private Profile getRepresentProfile(Member member) {
        List<Profile> profiles = profileRepository.findAllByMemberOrderByUpdatedAtDesc(member);
        if (profiles.isEmpty()) {
            throw new ProfileException(ProfileErrorCode.PROFILE_NOT_FOUND);
        }
        return profiles.get(0);
    }

    private List<ContestSummaryResponse> getRecommendationsForProfile(Profile profile) {
        InterestCategory category =
                profile.getInterestCategories().stream().findFirst().orElse(InterestCategory.IT_AI_TECH);

        List<Contest> openContests = contestRepository
                .findAllWithFilterAndDeadlineAsc(null, category, "OPEN", LocalDateTime.now(), PageRequest.of(0, 10))
                .getContent();

        List<Long> contestIds = openContests.stream().map(Contest::getContestId).toList();
        List<Long> recommendedIds = aiClient.recommendContests(category, contestIds);

        List<Contest> recommendedContests = openContests.stream()
                .filter(c -> recommendedIds.contains(c.getContestId()))
                .limit(3)
                .toList();

        return recommendedContests.stream()
                .map(c -> org.cotato.gongmozip.domains.contest.converter.ContestConverter.toContestSummaryResponse(
                        c, LocalDateTime.now()))
                .toList();
    }

    private String getKoreanCategoryName(InterestCategory category) {
        if (category == null) return "미지정";
        return switch (category) {
            case IT_AI_TECH -> "IT/AI/기술";
            case MARKETING_AD_BRANDING -> "마케팅/광고/브랜딩";
            case IDEA_PLANNING -> "아이디어/기획";
            case ART_DESIGN -> "미술/디자인";
            case PHOTO_VIDEO -> "사진/영상";
            case DATA_ANALYSIS -> "데이터 분석";
        };
    }

    private String getKoreanCharacterName(CharacterType type) {
        if (type == null) return "알 수 없는 러너";
        return switch (type) {
            case LEAD_RUNNER -> "리드러너";
            case TRACK_RUNNER -> "트랙러너";
            case BOOST_RUNNER -> "부스트러너";
            case FREE_RUNNER -> "프리러너";
        };
    }
}
