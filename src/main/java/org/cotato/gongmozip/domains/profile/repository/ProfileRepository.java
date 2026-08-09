package org.cotato.gongmozip.domains.profile.repository;

import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    List<Profile> findAllByMemberOrderByUpdatedAtDesc(Member member);

    Optional<Profile> findByProfileIdAndMember(Long profileId, Member member);

    int countByMember(Member member);

    boolean existsByNickname(String nickname);

    boolean existsByNicknameAndProfileIdNot(String nickname, Long profileId);
}
