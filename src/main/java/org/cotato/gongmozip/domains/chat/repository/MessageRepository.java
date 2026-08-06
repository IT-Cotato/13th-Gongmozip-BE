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
    // memberId 각각) lazy load가 발생한다. createdAt이 같은 메시지가 여러 개면 순서가 흔들릴 수
    // 있어 messageId를 2차 정렬 기준으로 추가해 결과를 항상 결정적으로 만든다.
    @Query(
            """
            SELECT m FROM Message m
            LEFT JOIN FETCH m.senderTeamMember stm
            LEFT JOIN FETCH stm.profile
            LEFT JOIN FETCH stm.member
            WHERE m.team.teamId = :teamId
            ORDER BY m.createdAt DESC, m.messageId DESC
            """)
    List<Message> findByTeam_TeamIdOrderByCreatedAtDesc(@Param("teamId") Long teamId, Pageable pageable);

    Optional<Message> findFirstByTeam_TeamIdOrderByCreatedAtDesc(Long teamId);

    Optional<Message> findFirstByTeam_TeamIdAndMessageTypeOrderByCreatedAtDesc(Long teamId, MessageType messageType);

    long countByTeam_TeamIdAndCreatedAtAfter(Long teamId, LocalDateTime after);

    // 채팅방 목록 조회에서 팀마다 따로 "마지막 메시지"를 조회하면 N+1이 되므로, 한 번에 묶어서 가져온다.
    // created_at으로 매칭하면 같은 팀에서 시각이 완전히 같은 메시지가 여러 개일 때 어느 게 돌아올지
    // 결정적이지 않아서, IDENTITY 컬럼이라 항상 삽입 순서와 일치하는 message_id의 MAX로 묶는다
    // (created_at 동석 문제를 아예 없애면서 쿼리도 더 단순해짐).
    //
    // teamIds가 비어있으면 DB까지 안 가고 바로 빈 리스트를 반환한다 — native query의 "IN ()"은
    // MySQL에서 SQL 문법 오류다(H2는 관대하게 통과시켜서 테스트만으로는 못 잡는 차이였음).
    default List<Message> findLatestMessagePerTeam(List<Long> teamIds) {
        if (teamIds == null || teamIds.isEmpty()) {
            return List.of();
        }
        return findLatestMessagePerTeamByTeamIds(teamIds);
    }

    @Query(
            value =
                    """
                    SELECT m.* FROM messages m
                    INNER JOIN (
                        SELECT team_id, MAX(message_id) AS latest_message_id
                        FROM messages
                        WHERE team_id IN (:teamIds)
                        GROUP BY team_id
                    ) latest ON m.team_id = latest.team_id AND m.message_id = latest.latest_message_id
                    """,
            nativeQuery = true)
    List<Message> findLatestMessagePerTeamByTeamIds(@Param("teamIds") List<Long> teamIds);
}
