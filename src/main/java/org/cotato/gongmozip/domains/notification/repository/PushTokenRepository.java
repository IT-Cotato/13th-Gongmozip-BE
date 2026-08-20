package org.cotato.gongmozip.domains.notification.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.notification.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    Optional<PushToken> findByToken(String token);

    // 챗봇/매칭 알림, 일반 채팅 메시지 모두 여러 회원에게 한 번에 발송하므로 회원 여러 명의 토큰을
    // 한 번에 조회한다(회원마다 따로 조회하는 N+1을 피함).
    List<PushToken> findAllByMember_MemberIdIn(Collection<Long> memberIds);

    void deleteByMember_MemberIdAndToken(Long memberId, String token);
}
