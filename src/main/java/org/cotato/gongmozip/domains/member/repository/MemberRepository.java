package org.cotato.gongmozip.domains.member.repository;

import java.util.Optional;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmail(String email);

    Optional<Member> findByEmail(String email);
}
