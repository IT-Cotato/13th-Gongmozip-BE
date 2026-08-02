package org.cotato.gongmozip.domains.matching.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cotato.gongmozip.domains.matching.converter.MatchingConverter;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.LeaderCandidateResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.LeaderRecommendationCreateResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.LeaderRecommendationDetailResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.MatchingExplanationResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.MatchingReasonCreateResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.MatchingReasonDetailResponse;
import org.cotato.gongmozip.domains.matching.entity.LeaderRecommendation;
import org.cotato.gongmozip.domains.matching.entity.MatchingExplanation;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroup;
import org.cotato.gongmozip.domains.matching.entity.MatchingReason;
import org.cotato.gongmozip.domains.matching.exception.MatchingException;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.matching.repository.LeaderRecommendationRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingExplanationRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupMemberRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingReasonRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.enums.AiSummaryStatus;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchingService {

    private final MatchingExplanationRepository matchingExplanationRepository;
    private final MatchingGroupRepository matchingGroupRepository;
    private final MatchingGroupMemberRepository matchingGroupMemberRepository;
    private final MatchingReasonRepository matchingReasonRepository;
    private final LeaderRecommendationRepository leaderRecommendationRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MatchingAiWorker matchingAiWorker;

    public MatchingExplanationResponse getMatchingExplanation() {
        MatchingExplanation explanation = matchingExplanationRepository
                .findFirstByOrderByCreatedAtDesc()
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.EXPLANATION_NOT_FOUND));
        return MatchingConverter.toExplanationResponse(explanation);
    }

    @Transactional
    public MatchingReasonCreateResponse createMatchingReason(Long matchingGroupId, Member member) {
        MatchingGroup matchingGroup = matchingGroupRepository
                .findById(matchingGroupId)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.MATCHING_GROUP_NOT_FOUND));

        // 해당 매칭 그룹에 접근 권한이 있는지 체크 (그룹 멤버 여부)
        boolean isMember = matchingGroupMemberRepository.existsByMatchingGroupAndMember(matchingGroup, member);
        if (!isMember) {
            throw new MatchingException(MatchingErrorCode.MATCHING_GROUP_ACCESS_DENIED);
        }

        MatchingReason reason =
                matchingReasonRepository.findByMatchingGroup(matchingGroup).orElse(null);

        if (reason != null) {
            if (reason.getStatus() == AiSummaryStatus.PROCESSING) {
                throw new MatchingException(MatchingErrorCode.MATCHING_REASON_IN_PROGRESS);
            }
            reason.reset();
        } else {
            reason = MatchingReason.builder()
                    .matchingGroup(matchingGroup)
                    .status(AiSummaryStatus.PENDING)
                    .build();
        }

        MatchingReason savedReason = matchingReasonRepository.save(reason);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                matchingAiWorker.generateMatchingReasonAsync(savedReason.getMatchingReasonId());
            }
        });

        return MatchingConverter.toReasonCreateResponse(savedReason);
    }

    public MatchingReasonDetailResponse getMatchingReason(Long matchingGroupId, Member member) {
        MatchingGroup matchingGroup = matchingGroupRepository
                .findById(matchingGroupId)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.MATCHING_GROUP_NOT_FOUND));

        boolean isMember = matchingGroupMemberRepository.existsByMatchingGroupAndMember(matchingGroup, member);
        if (!isMember) {
            throw new MatchingException(MatchingErrorCode.MATCHING_GROUP_ACCESS_DENIED);
        }

        MatchingReason reason = matchingReasonRepository
                .findByMatchingGroup(matchingGroup)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.MATCHING_REASON_NOT_FOUND));

        return MatchingConverter.toReasonDetailResponse(reason);
    }

    @Transactional
    public LeaderRecommendationCreateResponse createLeaderRecommendation(Long teamId, Member member) {
        Team team = teamRepository
                .findById(teamId)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.LEADER_RECOMMENDATION_NOT_FOUND));

        // 팀원 권한 체크
        TeamMember teamMember = teamMemberRepository
                .findByTeam_TeamIdAndMember_MemberId(teamId, member.getMemberId())
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.MATCHING_GROUP_ACCESS_DENIED));

        if (teamMember.getStatus() != TeamMemberStatus.ACTIVE) {
            throw new MatchingException(MatchingErrorCode.MATCHING_GROUP_ACCESS_DENIED);
        }

        LeaderRecommendation rec =
                leaderRecommendationRepository.findByTeam(team).orElse(null);

        if (rec != null) {
            if (rec.getStatus() == AiSummaryStatus.PROCESSING) {
                throw new MatchingException(MatchingErrorCode.LEADER_RECOMMENDATION_IN_PROGRESS);
            }
            rec.reset();
        } else {
            rec = LeaderRecommendation.builder()
                    .team(team)
                    .status(AiSummaryStatus.PENDING)
                    .build();
        }

        LeaderRecommendation savedRec = leaderRecommendationRepository.save(rec);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                matchingAiWorker.generateLeaderRecommendationAsync(savedRec.getLeaderRecommendationId());
            }
        });

        return MatchingConverter.toLeaderRecCreateResponse(savedRec);
    }

    public LeaderRecommendationDetailResponse getLeaderRecommendation(Long teamId, Member member) {
        Team team = teamRepository
                .findById(teamId)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.LEADER_RECOMMENDATION_NOT_FOUND));

        TeamMember teamMember = teamMemberRepository
                .findByTeam_TeamIdAndMember_MemberId(teamId, member.getMemberId())
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.MATCHING_GROUP_ACCESS_DENIED));

        if (teamMember.getStatus() != TeamMemberStatus.ACTIVE) {
            throw new MatchingException(MatchingErrorCode.MATCHING_GROUP_ACCESS_DENIED);
        }

        LeaderRecommendation rec = leaderRecommendationRepository
                .findByTeam(team)
                .orElseThrow(() -> new MatchingException(MatchingErrorCode.LEADER_RECOMMENDATION_NOT_FOUND));

        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);

        String recommendedNickname = null;
        if (rec.getRecommendedMember() != null) {
            recommendedNickname = activeMembers.stream()
                    .filter(tm -> tm.getMember()
                            .getMemberId()
                            .equals(rec.getRecommendedMember().getMemberId()))
                    .map(tm -> tm.getProfile().getNickname())
                    .findFirst()
                    .orElse(null);
        }

        List<LeaderCandidateResponse> resolvedCandidates = null;
        if (rec.getCandidates() != null) {
            resolvedCandidates = rec.getCandidates().stream()
                    .map(c -> {
                        String nickname = activeMembers.stream()
                                .filter(tm -> tm.getMember().getMemberId().equals(c.memberId()))
                                .map(tm -> tm.getProfile().getNickname())
                                .findFirst()
                                .orElse(c.nickname());
                        return new LeaderCandidateResponse(c.memberId(), nickname, c.rank(), c.score(), c.reason());
                    })
                    .toList();
        }

        return MatchingConverter.toLeaderRecDetailResponse(rec, recommendedNickname, resolvedCandidates);
    }
}
