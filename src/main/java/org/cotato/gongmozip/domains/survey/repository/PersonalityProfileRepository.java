package org.cotato.gongmozip.domains.survey.repository;

import java.util.Optional;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.survey.entity.PersonalityProfile;
import org.cotato.gongmozip.domains.survey.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalityProfileRepository extends JpaRepository<PersonalityProfile, Long> {

    Optional<PersonalityProfile> findByMemberAndSurveySubmission_Status(Member member, SubmissionStatus status);
}
