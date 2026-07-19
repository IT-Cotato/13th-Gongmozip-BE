package org.cotato.gongmozip.domains.auth.repository;

import org.cotato.gongmozip.domains.auth.entity.AuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthAccountRepository extends JpaRepository<AuthAccount, Long> {
}
