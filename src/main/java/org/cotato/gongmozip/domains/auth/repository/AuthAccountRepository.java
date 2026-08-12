package org.cotato.gongmozip.domains.auth.repository;

import java.util.Optional;
import org.cotato.gongmozip.domains.auth.entity.AuthAccount;
import org.cotato.gongmozip.domains.auth.enums.AuthProvider;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AuthAccountRepository extends JpaRepository<AuthAccount, Long> {

    Optional<AuthAccount> findByProviderAndProviderMemberId(AuthProvider provider, String providerMemberId);

    boolean existsByMemberAndProvider(Member member, AuthProvider provider);

    @Modifying
    @Query("DELETE FROM AuthAccount aa WHERE aa.member = :member")
    void deleteAllByMember(Member member);
}
