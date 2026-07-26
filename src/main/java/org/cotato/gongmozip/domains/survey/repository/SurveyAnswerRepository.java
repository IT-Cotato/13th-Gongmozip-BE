package org.cotato.gongmozip.domains.survey.repository;

import java.util.List;
import org.cotato.gongmozip.domains.survey.entity.SurveyAnswer;
import org.cotato.gongmozip.domains.survey.entity.SurveySubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SurveyAnswerRepository extends JpaRepository<SurveyAnswer, Long> {

    List<SurveyAnswer> findBySubmission(SurveySubmission submission);

    @Modifying
    @Query("DELETE FROM SurveyAnswer sa WHERE sa.submission = :submission")
    void deleteAllBySubmission(SurveySubmission submission);
}
