package org.cotato.gongmozip.domains.matching.score;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.cotato.gongmozip.domains.matching.exception.MatchingException;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.profile.entity.ProjectEvaluation;
import org.cotato.gongmozip.domains.profile.entity.ProjectExperience;
import org.cotato.gongmozip.domains.profile.enums.AiSummaryStatus;
import org.cotato.gongmozip.domains.profile.repository.ProjectEvaluationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoredProjectScoreProviderTest {

    @Mock
    private ProjectEvaluationRepository repository;

    private StoredProjectScoreProvider provider;

    @BeforeEach
    void setUp() {
        provider = new StoredProjectScoreProvider(repository);
    }

    @Test
    @DisplayName("등록된 프로젝트가 없으면 프로젝트 역량 점수는 0점이다")
    void noProjectsScoreZero() {
        assertThat(provider.evaluate(List.of())).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("완료된 프로젝트 AI 평가점수는 산술평균하고 소수 둘째 자리에서 반올림한다")
    void completedScoresAreAveragedHalfUp() {
        ProjectExperience first = project(1L);
        ProjectExperience second = project(2L);
        ProjectExperience third = project(3L);
        List<ProjectExperience> projects = List.of(first, second, third);
        given(repository.findAllByProjectExperienceIn(projects))
                .willReturn(List.of(
                        evaluation(first, 80, AiSummaryStatus.COMPLETED),
                        evaluation(second, 81, AiSummaryStatus.COMPLETED),
                        evaluation(third, 81, AiSummaryStatus.COMPLETED)));

        assertThat(provider.evaluate(projects)).isEqualByComparingTo("80.67");
        assertThat(provider.isReady(projects)).isTrue();
    }

    @Test
    @DisplayName("완료되지 않은 프로젝트 AI 평가가 있으면 매칭 신청을 제한한다")
    void incompleteEvaluationBlocksMatching() {
        ProjectExperience project = project(1L);
        List<ProjectExperience> projects = List.of(project);
        given(repository.findAllByProjectExperienceIn(projects))
                .willReturn(List.of(evaluation(project, null, AiSummaryStatus.FAILED)));

        assertThatThrownBy(() -> provider.evaluate(projects))
                .isInstanceOf(MatchingException.class)
                .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.PROJECT_EVALUATION_NOT_READY);
        assertThat(provider.isReady(projects)).isFalse();
    }

    private ProjectExperience project(Long id) {
        return ProjectExperience.builder().projectId(id).build();
    }

    private ProjectEvaluation evaluation(ProjectExperience project, Integer score, AiSummaryStatus status) {
        return ProjectEvaluation.builder()
                .projectExperience(project)
                .score(score)
                .status(status)
                .build();
    }
}
