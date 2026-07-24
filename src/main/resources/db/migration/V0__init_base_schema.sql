-- ============================================================
-- V0: 초기 베이스 스키마 (V1~V8 이전 상태)
--
-- 목적: 클린 DB에서 V1부터 마이그레이션이 정상 실행될 수 있도록
--       JPA ddl-auto가 담당하던 초기 테이블들을 Flyway로 관리
--
-- 주의:
--   - member 테이블은 V2(birth_date/gender), V3(role) ADD COLUMN 이전 상태
--   - member_profiles는 V2의 데이터 이관 UPDATE 쿼리에 필요한 레거시 테이블
--   - certifications는 profile_certifications FK 참조를 위해 생성
--     (실제 데이터 INSERT는 V1이 담당 - IF NOT EXISTS / WHERE NOT EXISTS 사용)
-- ============================================================

-- 1. certifications (자격증 마스터 테이블)
--    데이터 INSERT는 V1에서 처리 (WHERE NOT EXISTS 패턴 사용)
CREATE TABLE IF NOT EXISTS certifications
(
    certification_id   BIGINT       AUTO_INCREMENT PRIMARY KEY,
    certification_code VARCHAR(100) NOT NULL,
    certificate_name   VARCHAR(150) NOT NULL,
    category_code      VARCHAR(50)  NOT NULL,
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP    NOT NULL,
    CONSTRAINT uq_certifications_code UNIQUE (certification_code)
);

-- 2. member (회원 테이블 - V2 ADD COLUMN 이전 상태)
--    birth_date, gender → V2에서 ALTER TABLE로 추가
--    role               → V3에서 ALTER TABLE로 추가
CREATE TABLE IF NOT EXISTS member
(
    member_id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
    email             VARCHAR(255) NOT NULL,
    password          VARCHAR(255) NULL,
    status            VARCHAR(30)  NOT NULL,
    email_verified_at DATETIME     NULL,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,
    CONSTRAINT uq_member_email UNIQUE (email)
);

-- 3. member_profiles (레거시 테이블 - V2에서 member로 데이터 이관 후 보존)
--    V2 마이그레이션의 UPDATE 쿼리가 이 테이블을 참조하므로 반드시 존재해야 함
CREATE TABLE IF NOT EXISTS member_profiles
(
    member_profile_id BIGINT      AUTO_INCREMENT PRIMARY KEY,
    member_id         BIGINT      NOT NULL,
    birth_date        DATE        NULL,
    gender            VARCHAR(20) NULL,
    created_at        TIMESTAMP   NOT NULL,
    updated_at        TIMESTAMP   NOT NULL,
    CONSTRAINT fk_member_profiles_member
        FOREIGN KEY (member_id) REFERENCES member (member_id)
);

-- 4. profiles (사용자 프로필 테이블)
--    is_main_unique: DB 레벨 대표 프로필 단일성 보장을 위한 보조 컬럼
--    (isMain=true → true, isMain=false → NULL; UNIQUE 인덱스가 NULL 중복을 허용하는 특성 활용)
CREATE TABLE IF NOT EXISTS profiles
(
    profile_id           BIGINT        AUTO_INCREMENT PRIMARY KEY,
    member_id            BIGINT        NOT NULL,
    nickname             VARCHAR(50)   NOT NULL,
    school_name          VARCHAR(150)  NOT NULL,
    grade                INT           NOT NULL,
    major                VARCHAR(150)  NOT NULL,
    secondary_major      VARCHAR(150)  NULL,
    gpa                  DOUBLE        NOT NULL,
    gpa_scale            DOUBLE        NOT NULL,
    interest_categories  TEXT          NOT NULL,
    is_main              BOOLEAN       NOT NULL,
    is_main_unique       BOOLEAN       NULL,
    is_public            BOOLEAN       NOT NULL,
    created_at           TIMESTAMP     NOT NULL,
    updated_at           TIMESTAMP     NOT NULL,
    CONSTRAINT fk_profiles_member
        FOREIGN KEY (member_id) REFERENCES member (member_id),
    CONSTRAINT uq_member_is_main_unique
        UNIQUE (member_id, is_main_unique)
);

-- 5. awards (수상 내역 테이블)
CREATE TABLE IF NOT EXISTS awards
(
    award_id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    profile_id        BIGINT       NOT NULL,
    award_name        VARCHAR(200) NOT NULL,
    organization_name VARCHAR(200) NULL,
    award_rank        VARCHAR(100) NULL,
    awarded_at        DATE         NULL,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,
    CONSTRAINT fk_awards_profile
        FOREIGN KEY (profile_id) REFERENCES profiles (profile_id)
);

-- 6. project_experiences (프로젝트 경험 테이블)
CREATE TABLE IF NOT EXISTS project_experiences
(
    project_id   BIGINT       AUTO_INCREMENT PRIMARY KEY,
    profile_id   BIGINT       NOT NULL,
    project_name VARCHAR(200) NOT NULL,
    description  TEXT         NOT NULL,
    role         VARCHAR(200) NOT NULL,
    tech_stacks  TEXT         NOT NULL,
    started_at   DATE         NOT NULL,
    ended_at     DATE         NULL,
    is_ongoing   BOOLEAN      NOT NULL,
    ai_summary   TEXT         NULL,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL,
    CONSTRAINT fk_project_experiences_profile
        FOREIGN KEY (profile_id) REFERENCES profiles (profile_id)
);

-- 7. profile_certifications (프로필-자격증 연결 테이블)
--    certification_id: 자격증 마스터 참조 (커스텀 자격증인 경우 NULL 허용)
CREATE TABLE IF NOT EXISTS profile_certifications
(
    profile_certification_id BIGINT       AUTO_INCREMENT PRIMARY KEY,
    profile_id               BIGINT       NOT NULL,
    certification_id         BIGINT       NULL,
    certificate_name         VARCHAR(150) NOT NULL,
    category_code            VARCHAR(50)  NOT NULL,
    issuer                   VARCHAR(100) NULL,
    acquired_at              DATE         NULL,
    is_custom                BOOLEAN      NOT NULL,
    created_at               TIMESTAMP    NOT NULL,
    updated_at               TIMESTAMP    NOT NULL,
    CONSTRAINT fk_profile_certifications_profile
        FOREIGN KEY (profile_id) REFERENCES profiles (profile_id),
    CONSTRAINT fk_profile_certifications_certification
        FOREIGN KEY (certification_id) REFERENCES certifications (certification_id)
);
