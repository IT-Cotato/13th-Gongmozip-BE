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

    // 탈퇴 처리 시 배치 준비와 같은 신청을 두고 경합하지 않도록 행을 잠가 최신 상태를 읽는다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT ma
            FROM MatchingApplication ma
            WHERE ma.member = :member
              AND ma.status IN :statuses
            """)
    List<MatchingApplication> findAllByMemberAndStatusInWithLock(
            @Param("member") Member member, @Param("statuses") Collection<MatchingApplicationStatus> statuses);

    // 결과 조회에서 회원·선택 프로필·배치를 함께 읽어 지연 로딩과 추가 쿼리를 피한다.
    @Query(
            """
            SELECT ma
            FROM MatchingApplication ma
            JOIN FETCH ma.member
            JOIN FETCH ma.profile
            LEFT JOIN FETCH ma.matchingBatch
            WHERE ma.member.memberId = :memberId
              AND ma.applicationDate = :applicationDate
            """)
    Optional<MatchingApplication> findResultApplication(
            @Param("memberId") Long memberId, @Param("applicationDate") LocalDate applicationDate);

    // 배치와 연결된 전체 신청을 조회해 결과·테스트에서 배치 단위 상태를 확인한다.
    List<MatchingApplication> findAllByMatchingBatch(MatchingBatch matchingBatch);

    // 준비 작업끼리 같은 WAITING 신청을 분류하지 못하도록 아직 배치가 없는 행을 잠가 조회한다.
    // 탈퇴 시 WAITING 신청은 서비스에서 자동 취소되지만, 데이터 이상 시 탈퇴 회원이
    // 매칭풀에 섞이지 않도록 ACTIVE 회원 조건으로 한 번 더 방어한다.
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
              AND ma.member.status = org.cotato.gongmozip.domains.member.enums.MemberStatus.ACTIVE
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

    // 결과 공개 알림(MatchingResultNotificationJobs) — 해당 신청일에 확정 결과가 나온(제안/확정/재배정
    // 대기/실패) 신청자에게만 "매칭 결과가 공개되었어요" 알림을 남긴다. WAITING/MATCHING(계산 중),
    // CANCELED/PASSED(본인이 이미 철회)는 대상에서 제외한다. MemberWithdrawService가 MATCHED/FAILED
    // 상태의 탈퇴는 막지 않아 결과 확정 이후 탈퇴가 가능하므로, findUnpreparedWaitingWithLock과
    // 동일하게 ACTIVE 회원만 대상으로 한 번 더 방어한다.
    @Query(
            """
            SELECT ma
            FROM MatchingApplication ma
            JOIN FETCH ma.member
            WHERE ma.applicationDate = :applicationDate
              AND ma.status IN :statuses
              AND ma.member.status = org.cotato.gongmozip.domains.member.enums.MemberStatus.ACTIVE
            """)
    List<MatchingApplication> findAllByApplicationDateAndStatusInWithMember(
            @Param("applicationDate") LocalDate applicationDate,
            @Param("statuses") Collection<MatchingApplicationStatus> statuses);

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
