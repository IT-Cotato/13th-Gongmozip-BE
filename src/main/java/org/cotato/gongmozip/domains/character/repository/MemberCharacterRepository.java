package org.cotato.gongmozip.domains.character.repository;

import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.character.entity.MemberCharacter;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MemberCharacterRepository extends JpaRepository<MemberCharacter, Long> {

    Optional<MemberCharacter> findByMember(Member member);

    List<MemberCharacter> findByMemberIn(List<Member> members);

    @Modifying
    @Query("DELETE FROM MemberCharacter mc WHERE mc.member = :member")
    void deleteAllByMember(Member member);
}
