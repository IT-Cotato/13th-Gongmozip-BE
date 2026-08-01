-- 팀원 매칭 신청 API
-- 레거시 matching_applications 행은 application_date를 NULL로 유지해 신규 '하루 1회' 제약과 충돌하지 않게 한다.

ALTER TABLE member
    MODIFY COLUMN collaboration_point INT NOT NULL DEFAULT 100;

UPDATE member m
SET m.collaboration_point = 100
WHERE m.collaboration_point = 0
  AND NOT EXISTS (
      SELECT 1
      FROM collaboration_point_histories h
      WHERE h.member_id = m.member_id
  );

ALTER TABLE member
    ADD COLUMN matching_blocked_until TIMESTAMP NULL;

ALTER TABLE matching_applications
    ADD COLUMN application_date DATE NULL;

ALTER TABLE matching_applications
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'MATCHED';

ALTER TABLE matching_applications
    ADD COLUMN leader_preference VARCHAR(30) NOT NULL DEFAULT 'NEUTRAL';

ALTER TABLE matching_applications
    ADD COLUMN first_matching BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE matching_applications
    ADD COLUMN canceled_at TIMESTAMP NULL;

ALTER TABLE matching_applications
    ADD COLUMN gpa_score DECIMAL(5, 2) NULL;

ALTER TABLE matching_applications
    ADD COLUMN project_score DECIMAL(5, 2) NULL;

ALTER TABLE matching_applications
    ADD COLUMN award_score DECIMAL(5, 2) NULL;

ALTER TABLE matching_applications
    ADD COLUMN certification_score DECIMAL(5, 2) NULL;

ALTER TABLE matching_applications
    ADD COLUMN collaboration_score DECIMAL(5, 2) NULL;

ALTER TABLE matching_applications
    ADD CONSTRAINT uq_matching_applications_member_date UNIQUE (member_id, application_date);

CREATE INDEX idx_matching_applications_date_status
    ON matching_applications (application_date, status);

CREATE INDEX idx_matching_applications_member_passed_at
    ON matching_applications (member_id, status, canceled_at);
