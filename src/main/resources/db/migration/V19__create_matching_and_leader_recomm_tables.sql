-- 1. matching_groups: 매칭 결과 그룹 테이블
CREATE TABLE matching_groups (
    matching_group_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    category            VARCHAR(50)    NOT NULL,
    skill_group         INT            NOT NULL,
    matching_score      DECIMAL(5,2)   NOT NULL,
    status              VARCHAR(30)    NOT NULL,
    created_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 2. matching_group_members: 매칭 결과 그룹에 속한 회원 조인 테이블
CREATE TABLE matching_group_members (
    matching_group_member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    matching_group_id        BIGINT         NOT NULL,
    member_id                BIGINT         NOT NULL,
    response_status          VARCHAR(30)    NOT NULL,
    responded_at             TIMESTAMP      NULL,
    created_at               TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_mg_members_matching_group
        FOREIGN KEY (matching_group_id) REFERENCES matching_groups (matching_group_id),
    CONSTRAINT fk_mg_members_member
        FOREIGN KEY (member_id) REFERENCES member (member_id),
    CONSTRAINT uq_mg_members_group_member
        UNIQUE (matching_group_id, member_id)
);

-- 3. matching_reasons: 매칭 결과에 대한 AI 추천 사유 분석 저장 테이블
CREATE TABLE matching_reasons (
    matching_reason_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    matching_group_id           BIGINT       NOT NULL,
    status                      VARCHAR(30)  NOT NULL,
    headline                    VARCHAR(255) NULL,
    summary                     TEXT         NULL,
    strengths                   TEXT         NULL, -- JSON Array
    common_points               TEXT         NULL, -- JSON Array
    complementary_points        TEXT         NULL, -- JSON Array
    cautions                    TEXT         NULL, -- JSON Array
    total_compatibility_score   INT          NOT NULL DEFAULT 0,
    team_goal_score             INT          NOT NULL DEFAULT 0,
    personality_score           INT          NOT NULL DEFAULT 0,
    extraversion_complement_score INT        NOT NULL DEFAULT 0,
    failure_message             TEXT         NULL,
    evaluated_at                TIMESTAMP    NULL,
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_matching_reasons_matching_group
        FOREIGN KEY (matching_group_id) REFERENCES matching_groups (matching_group_id),
    CONSTRAINT uq_matching_reasons_matching_group
        UNIQUE (matching_group_id)
);

-- 4. leader_recommendations: 매칭 완료 팀(teams)의 AI 팀장 추천 분석 저장 테이블
CREATE TABLE leader_recommendations (
    leader_recommendation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id                  BIGINT       NOT NULL,
    status                   VARCHAR(30)  NOT NULL,
    recommended_member_id    BIGINT       NULL,
    recommendation_reason    TEXT         NULL,
    candidates               TEXT         NULL, -- JSON Array
    team_summary             TEXT         NULL,
    caution                  TEXT         NULL,
    failure_message          TEXT         NULL,
    evaluated_at             TIMESTAMP    NULL,
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_leader_recommendations_team
        FOREIGN KEY (team_id) REFERENCES teams (team_id),
    CONSTRAINT fk_leader_recommendations_member
        FOREIGN KEY (recommended_member_id) REFERENCES member (member_id),
    CONSTRAINT uq_leader_recommendations_team
        UNIQUE (team_id)
);

-- 5. matching_explanations: 매칭 시스템 기본 이론/설명 정적 콘텐츠 테이블
CREATE TABLE matching_explanations (
    matching_explanation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title                   VARCHAR(150) NOT NULL,
    summary                 TEXT         NOT NULL,
    sections                TEXT         NOT NULL, -- JSON Array
    disclaimer              TEXT         NOT NULL,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
