package org.cotato.gongmozip.domains.survey.repository;

import java.util.List;
import org.cotato.gongmozip.domains.survey.entity.SurveyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyQuestionRepository extends JpaRepository<SurveyQuestion, Long> {

    List<SurveyQuestion> findAllByOrderByDisplayOrderAsc();
}
