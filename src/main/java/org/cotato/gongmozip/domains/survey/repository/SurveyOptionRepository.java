package org.cotato.gongmozip.domains.survey.repository;

import java.util.List;
import org.cotato.gongmozip.domains.survey.entity.SurveyOption;
import org.cotato.gongmozip.domains.survey.entity.SurveyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyOptionRepository extends JpaRepository<SurveyOption, Long> {

    List<SurveyOption> findByQuestionOrderByDisplayOrderAsc(SurveyQuestion question);
}
