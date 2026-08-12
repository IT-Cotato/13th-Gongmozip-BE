package org.cotato.gongmozip.domains.member.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmail(String email);

    Optional<Member> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m WHERE m.memberId = :id")
    Optional<Member> findByIdWithLock(@Param("id") Long id);

    // 재가입 제한 기간이 지난 익명화 대상 탈퇴 회원 조회
    List<Member> findAllByStatusAndAnonymizedFalseAndWithdrawnAtBefore(MemberStatus status, LocalDateTime before);
}
