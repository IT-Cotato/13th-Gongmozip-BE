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
import org.springframework.test.util.ReflectionTestUtils;
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
        entityManager.flush();

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

    @DisplayName("대상 팀 id 목록이 비어있으면 빈 결과를 반환한다.")
    @Test
    void 대상_팀_id_목록이_비어있으면_빈_결과를_반환한다() {
        // given
        Team team = teamRepository.save(team());
        saveMessageAt(team, "메시지", LocalDateTime.now());
        entityManager.flush();

        // when
        List<Message> latestMessages = messageRepository.findLatestMessagePerTeam(List.of());

        // then
        assertThat(latestMessages).isEmpty();
    }

    private Message saveMessageAt(Team team, String content, LocalDateTime createdAt) {
        Message saved = messageRepository.save(Message.builder()
                .team(team)
                .senderType(MessageSenderType.SYSTEM)
                .messageType(MessageType.SYSTEM_NOTICE)
                .content(content)
                .build());
        ReflectionTestUtils.setField(saved, "createdAt", createdAt);
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
