package org.cotato.gongmozip.domains.survey.repository;

import java.util.Optional;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.survey.entity.SurveySubmission;
import org.cotato.gongmozip.domains.survey.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveySubmissionRepository extends JpaRepository<SurveySubmission, Long> {

    Optional<SurveySubmission> findByMemberAndStatus(Member member, SubmissionStatus status);
}
