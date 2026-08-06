package org.cotato.gongmozip.domains.team.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.enums.LeaderSelectionMode;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ChatbotOrchestrationService.forceAdvanceGreetingIfDue(스케줄러)와 recordGreetingAndAdvance(팀원
 * 메시지 트리거)가 같은 팀을 동시에 GREETING에서 전이시키는 경합(이슈 #62)을 막기 위해 도입한
 * Team.version(@Version) 낙관적 잠금을 검증한다. 각 경로를 별도 트랜잭션으로 흉내내 두 번째로
 * 커밋을 시도하는 쪽이 stale한 버전 때문에 실패하는지 확인한다.
 */
@SpringBootTest
class TeamOptimisticLockingIntegrationTest {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    @DisplayName("두 트랜잭션이 같은 팀을 각자 읽어 수정한 뒤, 나중에 커밋하는 쪽은 낙관적 잠금 예외로 실패한다.")
    @Test
    void 두_트랜잭션이_같은_팀을_동시에_수정하면_나중_커밋이_실패한다() {
        txTemplate = new TransactionTemplate(transactionManager);

        // given: 스케줄러와 팀원 메시지 트리거가 거의 동시에 같은 팀을 읽었다고 가정한다.
        Long teamId = txTemplate.execute(status -> teamRepository.save(team()).getTeamId());
        Team readByGreetingTrigger =
                txTemplate.execute(status -> teamRepository.findById(teamId).orElseThrow());
        Team readByScheduler =
                txTemplate.execute(status -> teamRepository.findById(teamId).orElseThrow());

        // when: 팀원 메시지 트리거가 먼저 LEADER_SELECTING으로 전이시키고 커밋한다.
        txTemplate.executeWithoutResult(status -> {
            readByGreetingTrigger.advanceStatus(TeamStatus.LEADER_SELECTING);
            teamRepository.save(readByGreetingTrigger);
        });

        // then: 스케줄러가 자신이 읽은(이제는 stale한) 버전으로 같은 전이를 시도하면 실패한다.
        assertThatThrownBy(() -> txTemplate.executeWithoutResult(status -> {
                    readByScheduler.advanceStatus(TeamStatus.LEADER_SELECTING);
                    teamRepository.save(readByScheduler);
                }))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        Team finalState =
                txTemplate.execute(status -> teamRepository.findById(teamId).orElseThrow());
        assertThat(finalState.getStatus()).isEqualTo(TeamStatus.LEADER_SELECTING);
    }

    private Team team() {
        return Team.builder()
                .status(TeamStatus.GREETING)
                .preferredCategory(InterestCategory.IT_AI_TECH)
                .leaderSelectionMode(LeaderSelectionMode.OPEN_NOMINATION)
                .chatbotEnabled(true)
                .submitted(false)
                .build();
    }
}
