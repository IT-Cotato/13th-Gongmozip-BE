-- 설문 계산 결과는 설문 제출과 생명주기가 같으므로 survey_submissions에 보관한다.
-- 기존 제출 행과의 호환성을 위해 신규 칼럼은 nullable로 추가한다.
ALTER TABLE survey_submissions
    ADD COLUMN agreeableness_score DECIMAL(5, 2) NULL;

ALTER TABLE survey_submissions
    ADD COLUMN conscientiousness_score DECIMAL(5, 2) NULL;

ALTER TABLE survey_submissions
    ADD COLUMN honesty_humility_score DECIMAL(5, 2) NULL;

ALTER TABLE survey_submissions
    ADD COLUMN extroversion_score DECIMAL(5, 2) NULL;

ALTER TABLE survey_submissions
    ADD COLUMN goal_preference_score DECIMAL(5, 2) NULL;

ALTER TABLE survey_submissions
    ADD COLUMN work_style_score DECIMAL(5, 2) NULL;

ALTER TABLE survey_submissions
    ADD COLUMN communication_style_score DECIMAL(5, 2) NULL;

ALTER TABLE survey_submissions
    ADD COLUMN extroversion_2_score DECIMAL(5, 2) NULL;

ALTER TABLE survey_submissions
    ADD COLUMN extroversion_3_score DECIMAL(5, 2) NULL;

ALTER TABLE survey_submissions
    ADD COLUMN extroversion_type VARCHAR(1) NULL;

ALTER TABLE survey_submissions
    ADD COLUMN character_type VARCHAR(50) NULL;

ALTER TABLE survey_submissions
    ADD COLUMN character_x_score DECIMAL(5, 2) NULL;

ALTER TABLE survey_submissions
    ADD COLUMN character_y_score DECIMAL(5, 2) NULL;

-- 회원별 가장 최근 설문 제출만 남긴다. 제출을 삭제하기 전에 연결된 답변부터 제거한다.
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

-- 매칭 신청 이력은 모두 유지하고, 가장 최근 신청의 성향 결과만 설문 제출로 옮긴다.
UPDATE survey_submissions
SET agreeableness_score = (
        SELECT pp.agreeableness_score
        FROM personality_profiles pp
        WHERE pp.member_id = survey_submissions.member_id
        ORDER BY pp.profile_id DESC
        LIMIT 1
    ),
    conscientiousness_score = (
        SELECT pp.conscientiousness_score
        FROM personality_profiles pp
        WHERE pp.member_id = survey_submissions.member_id
        ORDER BY pp.profile_id DESC
        LIMIT 1
    ),
    honesty_humility_score = (
        SELECT pp.honesty_humility_score
        FROM personality_profiles pp
        WHERE pp.member_id = survey_submissions.member_id
        ORDER BY pp.profile_id DESC
        LIMIT 1
    ),
    extroversion_score = (
        SELECT pp.extroversion_score
        FROM personality_profiles pp
        WHERE pp.member_id = survey_submissions.member_id
        ORDER BY pp.profile_id DESC
        LIMIT 1
    ),
    goal_preference_score = (
        SELECT pp.goal_preference_score
        FROM personality_profiles pp
        WHERE pp.member_id = survey_submissions.member_id
        ORDER BY pp.profile_id DESC
        LIMIT 1
    ),
    work_style_score = (
        SELECT pp.work_style_score
        FROM personality_profiles pp
        WHERE pp.member_id = survey_submissions.member_id
        ORDER BY pp.profile_id DESC
        LIMIT 1
    ),
    communication_style_score = (
        SELECT pp.communication_style_score
        FROM personality_profiles pp
        WHERE pp.member_id = survey_submissions.member_id
        ORDER BY pp.profile_id DESC
        LIMIT 1
    ),
    extroversion_type = (
        SELECT pp.extroversion_type
        FROM personality_profiles pp
        WHERE pp.member_id = survey_submissions.member_id
        ORDER BY pp.profile_id DESC
        LIMIT 1
    ),
    character_type = (
        SELECT pp.character_type
        FROM personality_profiles pp
        WHERE pp.member_id = survey_submissions.member_id
        ORDER BY pp.profile_id DESC
        LIMIT 1
    )
WHERE EXISTS (
    SELECT 1
    FROM personality_profiles pp
    WHERE pp.member_id = survey_submissions.member_id
);

-- 기존 답변에서 캐릭터 축 계산에 필요한 개별 문항 점수를 복원한다.
UPDATE survey_submissions
SET extroversion_2_score = (
        SELECT so.score_weight
        FROM survey_answers sa
        JOIN survey_questions sq ON sq.question_id = sa.question_id
        JOIN survey_options so ON so.option_id = sa.selected_option_id
        WHERE sa.submission_id = survey_submissions.survey_submission_id
          AND sq.question_key = 'EXTROVERSION_2'
    ),
    extroversion_3_score = (
        SELECT so.score_weight
        FROM survey_answers sa
        JOIN survey_questions sq ON sq.question_id = sa.question_id
        JOIN survey_options so ON so.option_id = sa.selected_option_id
        WHERE sa.submission_id = survey_submissions.survey_submission_id
          AND sq.question_key = 'EXTROVERSION_3'
    );

UPDATE survey_submissions
SET character_x_score = goal_preference_score + work_style_score + (
        SELECT so.score_weight
        FROM survey_answers sa
        JOIN survey_questions sq ON sq.question_id = sa.question_id
        JOIN survey_options so ON so.option_id = sa.selected_option_id
        WHERE sa.submission_id = survey_submissions.survey_submission_id
          AND sq.question_key = 'CONSCIENTIOUSNESS_1'
    ),
    character_y_score = communication_style_score + extroversion_2_score + extroversion_3_score;

-- 기존 성향 결과 행은 매칭 신청 스냅샷으로 유지한다.
-- V6 이후 추가된 계산 결과도 신청 시점의 전체 점수를 보관할 수 있도록 확장한다.
ALTER TABLE personality_profiles
    ADD COLUMN extroversion_2_score DECIMAL(5, 2) NULL;

ALTER TABLE personality_profiles
    ADD COLUMN extroversion_3_score DECIMAL(5, 2) NULL;

ALTER TABLE personality_profiles
    ADD COLUMN character_x_score DECIMAL(5, 2) NULL;

ALTER TABLE personality_profiles
    ADD COLUMN character_y_score DECIMAL(5, 2) NULL;

UPDATE personality_profiles
SET extroversion_2_score = (
        SELECT ss.extroversion_2_score
        FROM survey_submissions ss
        WHERE ss.member_id = personality_profiles.member_id
    ),
    extroversion_3_score = (
        SELECT ss.extroversion_3_score
        FROM survey_submissions ss
        WHERE ss.member_id = personality_profiles.member_id
    ),
    character_x_score = (
        SELECT ss.character_x_score
        FROM survey_submissions ss
        WHERE ss.member_id = personality_profiles.member_id
    ),
    character_y_score = (
        SELECT ss.character_y_score
        FROM survey_submissions ss
        WHERE ss.member_id = personality_profiles.member_id
    )
WHERE EXISTS (
    SELECT 1
    FROM survey_submissions ss
    WHERE ss.member_id = personality_profiles.member_id
);

ALTER TABLE personality_profiles
    MODIFY COLUMN extroversion_2_score DECIMAL(5, 2) NOT NULL;

ALTER TABLE personality_profiles
    MODIFY COLUMN extroversion_3_score DECIMAL(5, 2) NOT NULL;

ALTER TABLE personality_profiles
    MODIFY COLUMN character_x_score DECIMAL(5, 2) NOT NULL;

ALTER TABLE personality_profiles
    MODIFY COLUMN character_y_score DECIMAL(5, 2) NOT NULL;

-- 과거 SUPERSEDED 값을 제거하고 유지할 제출을 현재 제출 상태로 정리한다.
UPDATE survey_submissions
SET status = 'SUBMITTED',
    submitted_at = COALESCE(submitted_at, updated_at, created_at);

-- 리더 희망 여부는 매칭 신청 API에서 별도로 입력받으므로 설문 문항에서 제거한다.
DELETE FROM survey_options
WHERE question_id = (SELECT question_id FROM survey_questions WHERE question_key = 'LEADER_PREFERENCE');

DELETE FROM survey_questions
WHERE question_key = 'LEADER_PREFERENCE';

-- 모든 매칭 신청에는 각 신청 시점의 설문 점수 스냅샷을 유지하고 이름만 역할에 맞게 변경한다.
ALTER TABLE personality_profiles
    DROP COLUMN leader_preference_score;

ALTER TABLE personality_profiles
    RENAME TO matching_applications;

ALTER TABLE matching_applications
    RENAME COLUMN profile_id TO matching_application_id;

-- 모든 점수 데이터가 채워진 뒤 NOT NULL 제약을 적용한다.
ALTER TABLE survey_submissions
    MODIFY COLUMN agreeableness_score      DECIMAL(5, 2) NOT NULL,
    MODIFY COLUMN conscientiousness_score  DECIMAL(5, 2) NOT NULL,
    MODIFY COLUMN honesty_humility_score   DECIMAL(5, 2) NOT NULL,
    MODIFY COLUMN extroversion_score       DECIMAL(5, 2) NOT NULL,
    MODIFY COLUMN goal_preference_score    DECIMAL(5, 2) NOT NULL,
    MODIFY COLUMN work_style_score         DECIMAL(5, 2) NOT NULL,
    MODIFY COLUMN communication_style_score DECIMAL(5, 2) NOT NULL,
    MODIFY COLUMN extroversion_2_score     DECIMAL(5, 2) NOT NULL,
    MODIFY COLUMN extroversion_3_score     DECIMAL(5, 2) NOT NULL,
    MODIFY COLUMN extroversion_type        VARCHAR(1)    NOT NULL,
    MODIFY COLUMN character_type           VARCHAR(50)   NOT NULL,
    MODIFY COLUMN character_x_score        DECIMAL(5, 2) NOT NULL,
    MODIFY COLUMN character_y_score        DECIMAL(5, 2) NOT NULL;

-- 애플리케이션의 단일 제출 및 문항별 단일 답변 규칙을 DB 제약으로도 보장한다.
ALTER TABLE survey_submissions
    ADD CONSTRAINT uq_survey_submissions_member UNIQUE (member_id);

ALTER TABLE survey_answers
    ADD CONSTRAINT uq_survey_answers_submission_question UNIQUE (submission_id, question_id);
