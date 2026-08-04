-- 14시 매칭 배치, 알고리즘 결과 추적, JDBC ShedLock

CREATE TABLE matching_batches (
    matching_batch_id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_date               DATE          NOT NULL,
    category                       VARCHAR(50)   NOT NULL,
    skill_group                    INT           NOT NULL,
    status                         VARCHAR(30)   NOT NULL,
    initially_selected_algorithm   VARCHAR(40)   NULL,
    final_algorithm                VARCHAR(40)   NULL,
    random_seed                    BIGINT        NOT NULL,
    elapsed_millis                 BIGINT        NOT NULL DEFAULT 0,
    started_at                     TIMESTAMP     NULL,
    completed_at                   TIMESTAMP     NULL,
    published_at                   TIMESTAMP     NOT NULL,
    retry_count                    INT           NOT NULL DEFAULT 0,
    failure_message                TEXT          NULL,
    created_at                     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_matching_batches_pool
        UNIQUE (application_date, category, skill_group)
);

CREATE INDEX idx_matching_batches_status_publish
    ON matching_batches (status, published_at);

ALTER TABLE matching_groups
    ADD COLUMN matching_batch_id BIGINT NULL;

ALTER TABLE matching_groups
    ADD COLUMN leader_harmony_score DECIMAL(5, 2) NULL;

ALTER TABLE matching_groups
    ADD COLUMN goal_similarity_score DECIMAL(5, 2) NULL;

ALTER TABLE matching_groups
    ADD COLUMN work_style_similarity_score DECIMAL(5, 2) NULL;

ALTER TABLE matching_groups
    ADD COLUMN communication_similarity_score DECIMAL(5, 2) NULL;

ALTER TABLE matching_groups
    ADD COLUMN agreeableness_similarity_score DECIMAL(5, 2) NULL;

ALTER TABLE matching_groups
    ADD COLUMN conscientiousness_similarity_score DECIMAL(5, 2) NULL;

ALTER TABLE matching_groups
    ADD COLUMN honesty_humility_similarity_score DECIMAL(5, 2) NULL;

ALTER TABLE matching_groups
    ADD COLUMN extroversion_complement_score DECIMAL(5, 2) NULL;

ALTER TABLE matching_groups
    ADD CONSTRAINT fk_matching_groups_batch
        FOREIGN KEY (matching_batch_id) REFERENCES matching_batches (matching_batch_id);

CREATE INDEX idx_matching_groups_batch
    ON matching_groups (matching_batch_id);

ALTER TABLE matching_group_members
    ADD COLUMN matching_application_id BIGINT NULL;

ALTER TABLE matching_group_members
    ADD CONSTRAINT fk_matching_group_members_application
        FOREIGN KEY (matching_application_id)
        REFERENCES matching_applications (matching_application_id);

ALTER TABLE matching_group_members
    ADD CONSTRAINT uq_matching_group_members_application
        UNIQUE (matching_application_id);

ALTER TABLE matching_applications
    ADD COLUMN matching_batch_id BIGINT NULL;

ALTER TABLE matching_applications
    ADD COLUMN reassignment_priority BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE matching_applications
    ADD CONSTRAINT fk_matching_applications_batch
        FOREIGN KEY (matching_batch_id) REFERENCES matching_batches (matching_batch_id);

CREATE INDEX idx_matching_applications_batch
    ON matching_applications (matching_batch_id);

CREATE TABLE shedlock (
    name        VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at  TIMESTAMP(3) NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
