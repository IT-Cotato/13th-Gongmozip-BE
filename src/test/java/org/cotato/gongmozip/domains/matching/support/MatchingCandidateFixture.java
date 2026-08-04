package org.cotato.gongmozip.domains.matching.support;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.cotato.gongmozip.domains.matching.algorithm.model.pool.MatchingCandidate;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;

public final class MatchingCandidateFixture {

    private MatchingCandidateFixture() {}

    public static MatchingCandidate candidate(long id) {
        return candidate(id, LeaderPreference.DOES_NOT_WANT, false, ExtroversionType.A, "3.00");
    }

    public static MatchingCandidate candidate(
            long id,
            LeaderPreference leaderPreference,
            boolean reassignment,
            ExtroversionType extroversionType,
            String score) {
        BigDecimal value = new BigDecimal(score);
        return new MatchingCandidate(
                id,
                1000L + id,
                2000L + id,
                LocalDateTime.of(2026, 8, 2, 12, 0).plusSeconds(id),
                InterestCategory.IT_AI_TECH,
                BigDecimal.valueOf(50 + id),
                leaderPreference,
                reassignment,
                value,
                value,
                value,
                value,
                value,
                value,
                extroversionType);
    }
}
