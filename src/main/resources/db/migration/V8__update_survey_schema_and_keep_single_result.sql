-- 설문 제출 시점에는 아직 입력되지 않는 매칭 관련 칼럼을 nullable로 변경한다.
ALTER TABLE personality_profiles
    MODIFY COLUMN contest_category VARCHAR(50) NULL;

ALTER TABLE personality_profiles
    MODIFY COLUMN skill_score DECIMAL(5, 2) NULL;

ALTER TABLE personality_profiles
    MODIFY COLUMN skill_group INT NULL;

ALTER TABLE personality_profiles
    MODIFY COLUMN collaboration_distance INT NULL;

-- 캐릭터 유형 판정에 사용하는 X/Y축 점수를 추가한다.
ALTER TABLE personality_profiles
    ADD COLUMN character_x_score DECIMAL(5, 2) NULL;

ALTER TABLE personality_profiles
    ADD COLUMN character_y_score DECIMAL(5, 2) NULL;

-- 리더 희망 여부는 공모전 신청 API에서 별도로 입력받으므로 설문 문항과 결과 칼럼에서 제거한다.
DELETE FROM survey_options
WHERE question_id = (SELECT question_id FROM survey_questions WHERE question_key = 'LEADER_PREFERENCE');

DELETE FROM survey_questions
WHERE question_key = 'LEADER_PREFERENCE';

ALTER TABLE personality_profiles
    DROP COLUMN leader_preference_score;

-- 캐릭터 축 계산에 필요한 개별 외향성 점수를 추가한다.
ALTER TABLE personality_profiles
    ADD COLUMN extroversion_2_score DECIMAL(5, 2) NULL;

ALTER TABLE personality_profiles
    ADD COLUMN extroversion_3_score DECIMAL(5, 2) NULL;

-- 회원별 가장 최근 설문 제출만 남긴다. 기존 답변은 제출 행을 삭제하기 전에 먼저 제거한다.
DELETE FROM survey_answers
WHERE submission_id NOT IN (
    SELECT survey_submission_id
    FROM (
        SELECT MAX(survey_submission_id) AS survey_submission_id
        FROM survey_submissions
        GROUP BY member_id
    ) latest_submissions
);

DELETE FROM survey_submissions
WHERE survey_submission_id NOT IN (
    SELECT survey_submission_id
    FROM (
        SELECT MAX(survey_submission_id) AS survey_submission_id
        FROM survey_submissions
        GROUP BY member_id
    ) latest_submissions
);

-- 과거 SUPERSEDED 값이 남지 않도록 유지할 제출을 현재 제출 상태로 정리한다.
UPDATE survey_submissions
SET status = 'SUBMITTED',
    submitted_at = COALESCE(submitted_at, updated_at, created_at);

-- 회원별 가장 최근 성향 결과만 남긴다.
DELETE FROM personality_profiles
WHERE profile_id NOT IN (
    SELECT profile_id
    FROM (
        SELECT MAX(profile_id) AS profile_id
        FROM personality_profiles
        GROUP BY member_id
    ) latest_profiles
);

-- 애플리케이션 규칙을 DB 제약으로도 보장한다.
ALTER TABLE survey_submissions
    ADD CONSTRAINT uq_survey_submissions_member UNIQUE (member_id);

ALTER TABLE survey_answers
    ADD CONSTRAINT uq_survey_answers_submission_question UNIQUE (submission_id, question_id);
