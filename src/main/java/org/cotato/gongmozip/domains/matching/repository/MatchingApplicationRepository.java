package org.cotato.gongmozip.domains.matching.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchingApplicationRepository extends JpaRepository<MatchingApplication, Long> {

    // 매칭 신청에 사용된 프로필은 스냅샷 참조가 남아 있으므로 삭제를 제한한다
    boolean existsByProfile(Profile profile);

    // 취소·패스를 포함해 신청 행이 하나라도 있으면 같은 날 재신청을 막는다
    boolean existsByMemberAndApplicationDate(Member member, LocalDate applicationDate);

    Optional<MatchingApplication> findByMemberAndApplicationDate(Member member, LocalDate applicationDate);

    long countByApplicationDateAndStatusIn(LocalDate applicationDate, Collection<MatchingApplicationStatus> statuses);

    // 최근 패스 횟수로 다음 협업거리 감점(3~11m)을 계산한다
    long countByMemberAndStatusAndCanceledAtGreaterThanEqual(
            Member member, MatchingApplicationStatus status, LocalDateTime since);

    // 본인 신청에 대한 동시 철회만 잠가 패널티 중복 적용과 타인 신청의 불필요한 락 경합을 막는다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT ma
            FROM MatchingApplication ma
            WHERE ma.matchingApplicationId = :applicationId
              AND ma.member.memberId = :memberId
            """)
    Optional<MatchingApplication> findByIdAndMemberIdWithLock(
            @Param("applicationId") Long applicationId, @Param("memberId") Long memberId);
}
