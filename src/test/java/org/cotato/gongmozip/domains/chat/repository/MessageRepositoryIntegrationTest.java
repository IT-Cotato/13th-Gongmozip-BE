package org.cotato.gongmozip.domains.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.chat.entity.Message;
import org.cotato.gongmozip.domains.chat.enums.MessageSenderType;
import org.cotato.gongmozip.domains.chat.enums.MessageType;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.enums.LeaderSelectionMode;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link MessageRepository#findLatestMessagePerTeam}는 손으로 쓴 native query라 Mockito
 * 단위 테스트로는 SQL 자체의 정합성을 검증할 수 없다 — 실제 H2(MODE=MySQL)에 대고 돌려서 확인한다.
 */
@SpringBootTest
@Transactional
class MessageRepositoryIntegrationTest {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("메시지 목록 조회 시 createdAt이 완전히 같아도 messageId 내림차순으로 결정적으로 정렬된다.")
    @Test
    void 메시지_목록_조회_시_createdAt이_같아도_messageId로_결정적으로_정렬된다() {
        // given
        Team team = teamRepository.save(team());
        LocalDateTime sameInstant = LocalDateTime.now();
        Message first = saveMessageAt(team, "1번째", sameInstant);
        Message second = saveMessageAt(team, "2번째", sameInstant);
        Message third = saveMessageAt(team, "3번째", sameInstant);

        // when
        List<Message> latestFirst =
                messageRepository.findByTeamIdBeforeCursor(team.getTeamId(), null, PageRequest.of(0, 10));

        // then
        assertThat(latestFirst)
                .extracting(Message::getMessageId)
                .containsExactly(third.getMessageId(), second.getMessageId(), first.getMessageId());
    }

    @DisplayName("cursor를 넘기면 그 messageId보다 오래된 메시지만 최신순으로 반환한다.")
    @Test
    void cursor를_넘기면_그_messageId보다_오래된_메시지만_반환한다() {
        // given
        Team team = teamRepository.save(team());
        LocalDateTime now = LocalDateTime.now();
        Message first = saveMessageAt(team, "1번째", now.minusMinutes(3));
        Message second = saveMessageAt(team, "2번째", now.minusMinutes(2));
        Message third = saveMessageAt(team, "3번째", now.minusMinutes(1));

        // when
        List<Message> beforeThird = messageRepository.findByTeamIdBeforeCursor(
                team.getTeamId(), third.getMessageId(), PageRequest.of(0, 10));

        // then
        assertThat(beforeThird)
                .extracting(Message::getMessageId)
                .containsExactly(second.getMessageId(), first.getMessageId());
    }

    @DisplayName(
            "동시 저장으로 messageId 순서와 createdAt 순서가 어긋나도, 정렬과 cursor 필터 모두 messageId 기준이라 페이지 경계에서 메시지가 중복되거나 누락되지 않는다.")
    @Test
    void messageId와_createdAt_순서가_어긋나도_cursor_페이지네이션이_중복이나_누락_없이_이어진다() {
        // given: messageId(삽입 순서) 오름차순과 createdAt 오름차순이 정반대가 되도록 백데이트한다 —
        // 동시에 여러 메시지가 저장될 때 앱에서 찍는 createdAt과 DB IDENTITY 채번 순서가 어긋날 수
        // 있는 상황을 재현한다.
        Team team = teamRepository.save(team());
        LocalDateTime now = LocalDateTime.now();
        Message oldestByMessageIdButNewestByCreatedAt = saveMessageAt(team, "1번째 삽입", now);
        Message middle = saveMessageAt(team, "2번째 삽입", now.minusMinutes(1));
        Message newestByMessageIdButOldestByCreatedAt = saveMessageAt(team, "3번째 삽입", now.minusMinutes(2));

        // when: 페이지 크기 1로 첫 페이지를 조회하고, 받은 것 중 가장 오래된(messageId가 가장 작은)
        // 메시지의 messageId를 cursor로 다음 페이지를 이어서 조회한다.
        List<Message> firstPage =
                messageRepository.findByTeamIdBeforeCursor(team.getTeamId(), null, PageRequest.of(0, 1));
        List<Message> secondPage = messageRepository.findByTeamIdBeforeCursor(
                team.getTeamId(), firstPage.get(0).getMessageId(), PageRequest.of(0, 1));
        List<Message> thirdPage = messageRepository.findByTeamIdBeforeCursor(
                team.getTeamId(), secondPage.get(0).getMessageId(), PageRequest.of(0, 1));

        // then: createdAt 순서와 무관하게 messageId 내림차순으로만 페이지가 이어지고, 중복/누락이 없다.
        assertThat(firstPage)
                .extracting(Message::getMessageId)
                .containsExactly(newestByMessageIdButOldestByCreatedAt.getMessageId());
        assertThat(secondPage).extracting(Message::getMessageId).containsExactly(middle.getMessageId());
        assertThat(thirdPage)
                .extracting(Message::getMessageId)
                .containsExactly(oldestByMessageIdButNewestByCreatedAt.getMessageId());
    }

    @DisplayName("여러 팀에 걸친 메시지 중 각 팀의 가장 최근 메시지만 하나씩 반환한다.")
    @Test
    void 여러_팀에_걸친_메시지_중_각_팀의_가장_최근_메시지만_하나씩_반환한다() {
        // given
        Team teamA = teamRepository.save(team());
        Team teamB = teamRepository.save(team());
        Team teamWithNoMessages = teamRepository.save(team());

        saveMessageAt(teamA, "A-옛날", LocalDateTime.now().minusHours(2));
        Message latestA = saveMessageAt(teamA, "A-최근", LocalDateTime.now().minusMinutes(1));
        Message latestB = saveMessageAt(teamB, "B-유일", LocalDateTime.now().minusMinutes(30));

        // when
        List<Message> latestMessages = messageRepository.findLatestMessagePerTeam(
                List.of(teamA.getTeamId(), teamB.getTeamId(), teamWithNoMessages.getTeamId()));

        // then
        assertThat(latestMessages).hasSize(2);
        assertThat(latestMessages)
                .extracting(Message::getMessageId)
                .containsExactlyInAnyOrder(latestA.getMessageId(), latestB.getMessageId());
        assertThat(latestMessages).extracting(Message::getContent).containsExactlyInAnyOrder("A-최근", "B-유일");
    }

    @DisplayName("같은 팀에서 createdAt이 완전히 같은 메시지가 여러 개여도 message_id가 가장 큰(가장 나중에 삽입된) 메시지를 결정적으로 반환한다.")
    @Test
    void createdAt이_같아도_message_id가_가장_큰_메시지를_반환한다() {
        // given
        Team team = teamRepository.save(team());
        LocalDateTime sameInstant = LocalDateTime.now();
        saveMessageAt(team, "먼저 삽입", sameInstant);
        Message later = saveMessageAt(team, "나중 삽입", sameInstant);

        // when
        List<Message> latestMessages = messageRepository.findLatestMessagePerTeam(List.of(team.getTeamId()));

        // then
        assertThat(latestMessages).hasSize(1);
        assertThat(latestMessages.get(0).getMessageId()).isEqualTo(later.getMessageId());
        assertThat(latestMessages.get(0).getContent()).isEqualTo("나중 삽입");
    }

    @DisplayName("대상 팀 id 목록이 비어있으면 빈 결과를 반환한다.")
    @Test
    void 대상_팀_id_목록이_비어있으면_빈_결과를_반환한다() {
        // given
        Team team = teamRepository.save(team());
        saveMessageAt(team, "메시지", LocalDateTime.now());

        // when
        List<Message> latestMessages = messageRepository.findLatestMessagePerTeam(List.of());

        // then
        assertThat(latestMessages).isEmpty();
    }

    // BaseEntity.createdAt은 updatable=false라 저장 후 필드만 바꿔서는(ReflectionTestUtils 등)
    // DB에 반영되지 않는다 — native UPDATE로 직접 DB 값을 백데이트하고, 방금 저장한 엔티티만
    // 1차 캐시에서 detach해서 이후 조회가 stale 값이 아니라 DB에서 다시 읽도록 한다(team처럼
    // 다른 테스트에서 계속 쓰는 엔티티까지 clear()로 통째로 떼어내지 않기 위함).
    private Message saveMessageAt(Team team, String content, LocalDateTime createdAt) {
        Message saved = messageRepository.save(Message.builder()
                .team(team)
                .senderType(MessageSenderType.SYSTEM)
                .messageType(MessageType.SYSTEM_NOTICE)
                .content(content)
                .build());
        entityManager.flush();

        entityManager
                .createNativeQuery("UPDATE messages SET created_at = :createdAt WHERE message_id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", saved.getMessageId())
                .executeUpdate();
        entityManager.detach(saved);

        return saved;
    }

    private Team team() {
        return Team.builder()
                .status(TeamStatus.MATCHED)
                .preferredCategory(InterestCategory.IT_AI_TECH)
                .leaderSelectionMode(LeaderSelectionMode.OPEN_NOMINATION)
                .chatbotEnabled(true)
                .submitted(false)
                .build();
    }
}
