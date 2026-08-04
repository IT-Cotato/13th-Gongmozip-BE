package org.cotato.gongmozip.domains.matching.score;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.exception.MatchingException;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.profile.entity.ProjectEvaluation;
import org.cotato.gongmozip.domains.profile.entity.ProjectExperience;
import org.cotato.gongmozip.domains.profile.enums.AiSummaryStatus;
import org.cotato.gongmozip.domains.profile.repository.ProjectEvaluationRepository;
import org.springframework.stereotype.Component;

/**
 * 매칭 신청 중 외부 AI를 다시 호출하지 않고 이미 저장된 프로젝트 평가로 역량 점수를 계산하기 위해 만들었다.
 * 모든 프로젝트 평가가 완료되고 0~100 범위일 때만 평균을 반환해 불완전한 스냅샷이 매칭 입력으로 고정되는 것을 막는다.
 */
@Component
@RequiredArgsConstructor
public class StoredProjectScoreProvider implements ProjectScoreProvider {

    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE = new BigDecimal("100");

    private final ProjectEvaluationRepository projectEvaluationRepository;

    @Override
    public BigDecimal evaluate(List<ProjectExperience> projects) {
        if (projects.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }

        Map<Long, ProjectEvaluation> evaluations = getEvaluationsByProjectId(projects);
        // 누락된 평가가 하나라도 있으면 일부 프로젝트만으로 역량을 과소·과대 계산하지 않는다.
        if (evaluations.size() != projects.size()) {
            throw notReady();
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (ProjectExperience project : projects) {
            ProjectEvaluation evaluation = evaluations.get(project.getProjectId());
            if (!isCompletedAndValid(evaluation)) {
                throw notReady();
            }
            sum = sum.add(BigDecimal.valueOf(evaluation.getScore()));
        }
        return sum.divide(BigDecimal.valueOf(projects.size()), 2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean isReady(List<ProjectExperience> projects) {
        // 신청 가능 여부 조회에서 evaluate의 예외를 사용하지 않고 같은 준비 조건을 미리 확인한다.
        if (projects.isEmpty()) {
            return true;
        }
        Map<Long, ProjectEvaluation> evaluations = getEvaluationsByProjectId(projects);
        return evaluations.size() == projects.size()
                && projects.stream()
                        .map(project -> evaluations.get(project.getProjectId()))
                        .allMatch(this::isCompletedAndValid);
    }

    private Map<Long, ProjectEvaluation> getEvaluationsByProjectId(List<ProjectExperience> projects) {
        // 한 번에 조회해 프로젝트별 반복 쿼리를 피하고 이후 검증을 ID 기반으로 수행한다.
        return projectEvaluationRepository.findAllByProjectExperienceIn(projects).stream()
                .collect(Collectors.toMap(
                        evaluation -> evaluation.getProjectExperience().getProjectId(), Function.identity()));
    }

    private boolean isCompletedAndValid(ProjectEvaluation evaluation) {
        // 완료 상태더라도 과거 데이터의 null 또는 허용 범위 밖 점수는 매칭 입력으로 사용하지 않는다.
        if (evaluation == null
                || evaluation.getStatus() != AiSummaryStatus.COMPLETED
                || evaluation.getScore() == null) {
            return false;
        }
        BigDecimal score = BigDecimal.valueOf(evaluation.getScore());
        return score.compareTo(MIN_SCORE) >= 0 && score.compareTo(MAX_SCORE) <= 0;
    }

    private MatchingException notReady() {
        return new MatchingException(MatchingErrorCode.PROJECT_EVALUATION_NOT_READY);
    }
}
