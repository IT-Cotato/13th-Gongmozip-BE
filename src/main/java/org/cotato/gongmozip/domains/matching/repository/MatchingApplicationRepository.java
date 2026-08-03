package org.cotato.gongmozip.domains.matching.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.entity.MatchingBatch;
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

    // 배치와 연결된 전체 신청을 조회해 결과·테스트에서 배치 단위 상태를 확인한다.
    List<MatchingApplication> findAllByMatchingBatch(MatchingBatch matchingBatch);

    // 준비 작업끼리 같은 WAITING 신청을 분류하지 못하도록 아직 배치가 없는 행을 잠가 조회한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT ma
            FROM MatchingApplication ma
            JOIN FETCH ma.member
            JOIN FETCH ma.profile
            WHERE ma.applicationDate = :applicationDate
              AND ma.status = :status
              AND ma.matchingBatch IS NULL
            ORDER BY ma.contestCategory, ma.skillScore, ma.createdAt, ma.matchingApplicationId
            """)
    List<MatchingApplication> findUnpreparedWaitingWithLock(
            @Param("applicationDate") LocalDate applicationDate, @Param("status") MatchingApplicationStatus status);

    // 배치 선점 시 WAITING 신청만 잠그고 알고리즘에 필요한 회원·프로필을 한 번에 로딩한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT ma
            FROM MatchingApplication ma
            JOIN FETCH ma.member
            JOIN FETCH ma.profile
            WHERE ma.matchingBatch = :batch
              AND ma.status = :status
            ORDER BY ma.matchingApplicationId
            """)
    List<MatchingApplication> findAllByMatchingBatchAndStatusWithLock(
            @Param("batch") MatchingBatch batch, @Param("status") MatchingApplicationStatus status);

    // 결과 저장 시 배치의 모든 신청을 잠가 계산 도중 일어난 상태 변경을 검증한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT ma
            FROM MatchingApplication ma
            JOIN FETCH ma.member
            JOIN FETCH ma.profile
            WHERE ma.matchingBatch = :batch
            ORDER BY ma.matchingApplicationId
            """)
    List<MatchingApplication> findAllByMatchingBatchWithLock(@Param("batch") MatchingBatch batch);

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
