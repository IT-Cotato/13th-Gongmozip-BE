package org.cotato.gongmozip.domains.chat.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.chat.entity.Message;
import org.cotato.gongmozip.domains.chat.enums.MessageType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // 발신자 팀원의 profile/member까지 fetch join한다 — 안 그러면 메시지마다(닉네임, 아바타 조회용
    // memberId 각각) lazy load가 발생한다.
    @Query(
            """
            SELECT m FROM Message m
            LEFT JOIN FETCH m.senderTeamMember stm
            LEFT JOIN FETCH stm.profile
            LEFT JOIN FETCH stm.member
            WHERE m.team.teamId = :teamId
            ORDER BY m.createdAt DESC
            """)
    List<Message> findByTeam_TeamIdOrderByCreatedAtDesc(@Param("teamId") Long teamId, Pageable pageable);

    Optional<Message> findFirstByTeam_TeamIdOrderByCreatedAtDesc(Long teamId);

    Optional<Message> findFirstByTeam_TeamIdAndMessageTypeOrderByCreatedAtDesc(Long teamId, MessageType messageType);

    long countByTeam_TeamIdAndCreatedAtAfter(Long teamId, LocalDateTime after);

    // 채팅방 목록 조회에서 팀마다 따로 "마지막 메시지"를 조회하면 N+1이 되므로, 한 번에 묶어서 가져온다.
    // (같은 팀에서 createdAt이 완전히 같은 메시지가 여러 개면 그중 하나만 돌아올 수 있음 — 드문 케이스라 허용)
    @Query(
            value =
                    """
                    SELECT m.* FROM messages m
                    INNER JOIN (
                        SELECT team_id, MAX(created_at) AS max_created_at
                        FROM messages
                        WHERE team_id IN (:teamIds)
                        GROUP BY team_id
                    ) latest ON m.team_id = latest.team_id AND m.created_at = latest.max_created_at
                    """,
            nativeQuery = true)
    List<Message> findLatestMessagePerTeam(@Param("teamIds") List<Long> teamIds);
}
