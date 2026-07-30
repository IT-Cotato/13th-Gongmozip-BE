package org.cotato.gongmozip.domains.chat.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.chat.entity.Message;
import org.cotato.gongmozip.domains.chat.enums.MessageType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByTeam_TeamIdOrderByCreatedAtDesc(Long teamId, Pageable pageable);

    Optional<Message> findFirstByTeam_TeamIdOrderByCreatedAtDesc(Long teamId);

    Optional<Message> findFirstByTeam_TeamIdAndMessageTypeOrderByCreatedAtDesc(Long teamId, MessageType messageType);

    long countByTeam_TeamIdAndCreatedAtAfter(Long teamId, LocalDateTime after);
}
