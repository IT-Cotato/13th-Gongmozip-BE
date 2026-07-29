package org.cotato.gongmozip.domains.character.repository;

import java.util.Optional;
import org.cotato.gongmozip.domains.character.entity.MemberCharacter;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberCharacterRepository extends JpaRepository<MemberCharacter, Long> {

    Optional<MemberCharacter> findByMember(Member member);
}
