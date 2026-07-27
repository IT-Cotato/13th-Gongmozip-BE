package org.cotato.gongmozip.domains.auth.repository;

import java.util.Optional;
import org.cotato.gongmozip.domains.auth.entity.AuthAccount;
import org.cotato.gongmozip.domains.auth.enums.AuthProvider;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthAccountRepository extends JpaRepository<AuthAccount, Long> {

    Optional<AuthAccount> findByProviderAndProviderMemberId(AuthProvider provider, String providerMemberId);

    boolean existsByMemberAndProvider(Member member, AuthProvider provider);
}
