package org.cotato.gongmozip.domains.member.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.cotato.gongmozip.domains.member.entity.Member;
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
}
