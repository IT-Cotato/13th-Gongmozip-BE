package org.cotato.gongmozip.domains.survey.repository;

import java.util.List;
import org.cotato.gongmozip.domains.survey.entity.SurveyAnswer;
import org.cotato.gongmozip.domains.survey.entity.SurveySubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyAnswerRepository extends JpaRepository<SurveyAnswer, Long> {

    List<SurveyAnswer> findBySubmission(SurveySubmission submission);

    void deleteAllBySubmission(SurveySubmission submission);
}
