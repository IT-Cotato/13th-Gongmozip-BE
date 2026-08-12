package org.cotato.gongmozip.domains.collaboration.repository;

import java.time.LocalDateTime;
import org.cotato.gongmozip.domains.collaboration.entity.CollaborationPointHistory;
import org.cotato.gongmozip.domains.collaboration.enums.CollaborationPointReason;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollaborationPointHistoryRepository extends JpaRepository<CollaborationPointHistory, Long> {

    boolean existsByMember_MemberIdAndTeam_TeamIdAndReasonCode(
            Long memberId, Long teamId, CollaborationPointReason reasonCode);

    // 변경 이력이 하나도 없는 회원을 첫 매칭 사용자로 판정할 때 사용한다
    boolean existsByMember(Member member);

    java.util.List<CollaborationPointHistory> findAllByMemberOrderByCreatedAtDesc(Member member);

    org.springframework.data.domain.Page<CollaborationPointHistory> findAllByMemberOrderByCreatedAtDesc(
            Member member, org.springframework.data.domain.Pageable pageable);

    // 음수 delta만 양수 손실량으로 뒤집어 최근 협업거리 감소 합계를 계산한다
    @Query(
            """
            SELECT COALESCE(SUM(-h.delta), 0)
            FROM CollaborationPointHistory h
            WHERE h.member = :member
              AND h.delta < 0
              AND h.createdAt >= :since
            """)
    long sumLossSince(@Param("member") Member member, @Param("since") LocalDateTime since);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM CollaborationPointHistory h WHERE h.member = :member")
    void deleteAllByMember(@Param("member") Member member);
}
