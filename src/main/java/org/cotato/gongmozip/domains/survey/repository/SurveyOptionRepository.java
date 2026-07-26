package org.cotato.gongmozip.domains.survey.repository;

import java.util.List;
import org.cotato.gongmozip.domains.survey.entity.SurveyOption;
import org.cotato.gongmozip.domains.survey.entity.SurveyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SurveyOptionRepository extends JpaRepository<SurveyOption, Long> {

    @Query(
            """
            SELECT o
            FROM SurveyOption o
            JOIN FETCH o.question q
            WHERE q IN :questions
            ORDER BY q.displayOrder ASC, o.displayOrder ASC
            """)
    List<SurveyOption> findAllByQuestions(@Param("questions") List<SurveyQuestion> questions);
}
