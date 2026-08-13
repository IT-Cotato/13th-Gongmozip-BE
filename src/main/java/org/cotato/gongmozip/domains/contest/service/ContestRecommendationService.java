package org.cotato.gongmozip.domains.contest.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
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
    private final TeamMemberRepository teamMemberRepository;
    private final AiClient aiClient;

    public List<ContestSummaryResponse> getHomeRecommendations(Long memberId) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        List<Profile> profiles = profileRepository.findAllByMemberOrderByUpdatedAtDesc(member);
        if (profiles.isEmpty()) {
            return getRecentRandomRecommendations();
        }
        return getRecommendationsForProfile(profiles.get(0));
    }

    private List<ContestSummaryResponse> getRecentRandomRecommendations() {
        var page = contestRepository.findAllWithFilterAndNewest(
                null, null, "OPEN", LocalDateTime.now(), PageRequest.of(0, 10));
        List<Contest> recentContests = page != null ? page.getContent() : Collections.emptyList();

        List<Contest> mutableRecent = new ArrayList<>(recentContests);
        Collections.shuffle(mutableRecent);

        return mutableRecent.stream()
                .limit(3)
                .map(c -> org.cotato.gongmozip.domains.contest.converter.ContestConverter.toContestSummaryResponse(
                        c, LocalDateTime.now()))
                .toList();
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

        List<TeamMember> completedMemberships = teamMemberRepository.findCompletedProjectsAll(
                member.getMemberId(), TeamMemberStatus.ACTIVE, List.of(TeamStatus.SUBMITTED, TeamStatus.COMPLETED));

        List<String> completedTitles = completedMemberships.stream()
                .map(tm -> tm.getTeam().getContest())
                .filter(java.util.Objects::nonNull)
                .map(Contest::getTitle)
                .toList();

        String completedPart = "";
        if (!completedTitles.isEmpty()) {
            completedPart = String.format("이전 완주 프로젝트인 '%s' 등의 경험을 바탕으로 ", completedTitles.get(0));
        }

        String reason = String.format(
                "이 공모전은 귀하의 선호 분야인 '%s' 카테고리에 속해 있으며, %s대학생 팀 매칭 설문을 통해 분석된 귀하의 협업 캐릭터 '%s'의 성향과 목표에 "
                        + "매우 부합하여 AI에 의해 강력하게 추천되었습니다. 팀을 빌딩하여 프로젝트의 완성도를 높여보세요!",
                getKoreanCategoryName(category), completedPart, characterName);

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

        List<Contest> openContests = new ArrayList<>(contestRepository
                .findAllWithFilterAndDeadlineAsc(null, category, "OPEN", LocalDateTime.now(), PageRequest.of(0, 10))
                .getContent());

        if (openContests.size() < 3) {
            var page = contestRepository.findAllWithFilterAndNewest(
                    null, null, "OPEN", LocalDateTime.now(), PageRequest.of(0, 10));
            if (page != null) {
                List<Contest> fallbackContests = page.getContent();
                for (Contest fallback : fallbackContests) {
                    if (openContests.size() >= 10) {
                        break;
                    }
                    if (openContests.stream().noneMatch(c -> c.getContestId().equals(fallback.getContestId()))) {
                        openContests.add(fallback);
                    }
                }
            }
        }

        List<Long> contestIds = openContests.stream().map(Contest::getContestId).toList();

        List<TeamMember> completedMemberships = teamMemberRepository.findCompletedProjectsAll(
                profile.getMember().getMemberId(),
                TeamMemberStatus.ACTIVE,
                List.of(TeamStatus.SUBMITTED, TeamStatus.COMPLETED));

        List<String> completedContestTitles = completedMemberships.stream()
                .map(tm -> tm.getTeam().getContest())
                .filter(java.util.Objects::nonNull)
                .map(Contest::getTitle)
                .toList();

        List<Long> recommendedIds = aiClient.recommendContests(category, contestIds, completedContestTitles);

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
