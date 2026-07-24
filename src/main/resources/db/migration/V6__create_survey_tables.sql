CREATE TABLE survey_questions
(
    question_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_key      VARCHAR(100)  NOT NULL,
    question_text     TEXT          NOT NULL,
    question_type     VARCHAR(30)   NOT NULL,
    is_required       BOOLEAN       NOT NULL DEFAULT TRUE,
    is_reverse_scored BOOLEAN       NOT NULL DEFAULT FALSE,
    display_order     INT           NOT NULL,
    help_text         TEXT          NULL,
    placeholder_text  VARCHAR(255)  NULL,
    min_value         DECIMAL(12, 2) NULL,
    max_value         DECIMAL(12, 2) NULL,
    created_at        TIMESTAMP     NOT NULL,
    updated_at        TIMESTAMP     NOT NULL
);

CREATE TABLE survey_options
(
    option_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id     BIGINT         NOT NULL,
    option_key      VARCHAR(100)   NOT NULL,
    option_label    VARCHAR(255)   NOT NULL,
    option_value    VARCHAR(255)   NOT NULL,
    display_order   INT            NOT NULL,
    score_weight    DECIMAL(12, 2) NULL,
    is_other_option BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP      NOT NULL,
    updated_at      TIMESTAMP      NOT NULL,
    CONSTRAINT fk_survey_options_question
        FOREIGN KEY (question_id) REFERENCES survey_questions (question_id)
);

CREATE TABLE survey_submissions
(
    survey_submission_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id            BIGINT      NOT NULL,
    status               VARCHAR(30) NOT NULL,
    submitted_at         TIMESTAMP   NULL,
    created_at           TIMESTAMP   NOT NULL,
    updated_at           TIMESTAMP   NOT NULL,
    CONSTRAINT fk_survey_submissions_member
        FOREIGN KEY (member_id) REFERENCES member (member_id)
);

CREATE TABLE survey_answers
(
    survey_answer_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id      BIGINT         NOT NULL,
    question_id        BIGINT         NOT NULL,
    selected_option_id BIGINT         NULL,
    created_at         TIMESTAMP      NOT NULL,
    updated_at         TIMESTAMP      NOT NULL,
    CONSTRAINT fk_survey_answers_submission
        FOREIGN KEY (submission_id) REFERENCES survey_submissions (survey_submission_id),
    CONSTRAINT fk_survey_answers_question
        FOREIGN KEY (question_id) REFERENCES survey_questions (question_id),
    CONSTRAINT fk_survey_answers_option
        FOREIGN KEY (selected_option_id) REFERENCES survey_options (option_id)
);

CREATE TABLE personality_profiles
(
    profile_id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id                 BIGINT         NOT NULL,
    contest_category          VARCHAR(50)    NOT NULL,
    skill_score               DECIMAL(5, 2)  NOT NULL,
    skill_group               INT            NOT NULL,
    collaboration_distance    INT            NOT NULL DEFAULT 100,
    agreeableness_score       DECIMAL(5, 2)  NOT NULL,
    conscientiousness_score   DECIMAL(5, 2)  NOT NULL,
    honesty_humility_score    DECIMAL(5, 2)  NOT NULL,
    extroversion_score        DECIMAL(5, 2)  NOT NULL,
    goal_preference_score     DECIMAL(5, 2)  NOT NULL,
    work_style_score          DECIMAL(5, 2)  NOT NULL,
    communication_style_score DECIMAL(5, 2)  NOT NULL,
    leader_preference_score   DECIMAL(5, 2)  NOT NULL,
    extroversion_type         VARCHAR(1)     NOT NULL,
    character_type            VARCHAR(50)    NOT NULL,
    created_at                TIMESTAMP      NOT NULL,
    updated_at                TIMESTAMP      NOT NULL,
    CONSTRAINT fk_personality_profiles_member
        FOREIGN KEY (member_id) REFERENCES member (member_id)
);
