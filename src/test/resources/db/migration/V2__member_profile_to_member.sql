-- 1. H2 환경을 위해 기존 스키마 모사 (Hibernate 작동 전 임시 생성)
CREATE TABLE IF NOT EXISTS member (
    member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    email_verified_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS member_profiles (
    user_profile_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    birth_date DATE,
    gender VARCHAR(20),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 2. 마이그레이션 로직 검증용 테스트 데이터 삽입
INSERT INTO member (email, password, status, created_at, updated_at)
VALUES ('migrate_test@gongmozip.com', 'pass', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO member_profiles (member_id, birth_date, gender, created_at, updated_at)
SELECT member_id, '2000-01-01', 'MALE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM member
WHERE email = 'migrate_test@gongmozip.com';

-- 3. member 테이블에 컬럼 추가 (운영 마이그레이션과 동일)
ALTER TABLE member
    ADD COLUMN birth_date DATE NULL,
    ADD COLUMN gender VARCHAR(20) NULL;

-- 4. 데이터 이관 실행 (운영 마이그레이션과 동일)
UPDATE member m
SET m.birth_date = (SELECT mp.birth_date FROM member_profiles mp WHERE mp.member_id = m.member_id),
    m.gender = (SELECT mp.gender FROM member_profiles mp WHERE mp.member_id = m.member_id)
WHERE EXISTS (
    SELECT 1 FROM member_profiles mp WHERE mp.member_id = m.member_id
);

-- 5. 기존 테이블 삭제 (운영 마이그레이션과 동일)
DROP TABLE member_profiles;
