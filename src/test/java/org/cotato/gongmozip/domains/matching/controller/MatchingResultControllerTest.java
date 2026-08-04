package org.cotato.gongmozip.domains.matching.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.cotato.gongmozip.domains.matching.dto.response.MatchingResultResponse.MatchingResultMemberResponse;
import org.cotato.gongmozip.domains.matching.dto.response.MatchingResultResponse.MatchingScoreBreakdown;
import org.cotato.gongmozip.domains.matching.dto.response.MatchingResultResponse.TodayMatchingResultResponse;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupMemberStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingResultStatus;
import org.cotato.gongmozip.domains.matching.service.MatchingResultQueryService;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchingResultControllerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 3);

    @Mock
    private MatchingResultQueryService matchingResultQueryService;

    @InjectMocks
    private MatchingResultController matchingResultController;

    private final Member member =
            Member.builder().memberId(1L).email("member@example.com").build();
    private final CustomUserDetails userDetails = new CustomUserDetails(member);

    @DisplayName("컨트롤러는 인증 회원의 오늘 결과를 표준 매칭 응답으로 반환한다")
    @Test
    void getTodayResult() {
        var score = new MatchingScoreBreakdown(
                decimal("10.00"),
                decimal("10.00"),
                decimal("10.00"),
                decimal("10.00"),
                decimal("20.00"),
                decimal("10.00"),
                decimal("10.00"),
                decimal("10.00"));
        var members = List.of(new MatchingResultMemberResponse(
                1L,
                10L,
                "나의 프로필",
                CharacterType.LEAD_RUNNER,
                LeaderPreference.WANTS,
                MatchingGroupMemberStatus.PENDING,
                true));
        var serviceResponse = new TodayMatchingResultResponse(
                MatchingResultStatus.MATCHED,
                100L,
                TODAY,
                MatchingApplicationStatus.PROPOSED,
                TODAY.atTime(16, 0),
                InterestCategory.IT_AI_TECH,
                20L,
                3,
                decimal("90.00"),
                score,
                members,
                TODAY.plusDays(1).atTime(12, 0),
                MatchingGroupStatus.PROPOSED,
                MatchingGroupMemberStatus.PENDING,
                null,
                null);
        given(matchingResultQueryService.getTodayResult(1L)).willReturn(serviceResponse);

        var responseEntity = matchingResultController.getTodayResult(userDetails);

        assertThat(responseEntity.getStatusCode().value()).isEqualTo(200);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getCode()).isEqualTo("MATCHING_200_7");
        assertThat(responseEntity.getBody().getData()).isEqualTo(serviceResponse);
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
