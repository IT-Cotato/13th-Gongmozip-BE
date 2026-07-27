package org.cotato.gongmozip.domains.profile.repository;

import java.util.List;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    List<Profile> findAllByMemberOrderByUpdatedAtDesc(Member member);

    int countByMember(Member member);

    boolean existsByNickname(String nickname);
}
