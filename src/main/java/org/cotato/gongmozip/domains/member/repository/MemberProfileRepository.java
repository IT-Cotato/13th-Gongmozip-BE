package org.cotato.gongmozip.domains.member.repository;

import org.cotato.gongmozip.domains.member.entity.MemberProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {}
