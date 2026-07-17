-- 1. member 테이블에 role 컬럼 추가
ALTER TABLE member
    ADD COLUMN role VARCHAR(30) NOT NULL DEFAULT 'USER';

-- 2. contest 테이블 생성
CREATE TABLE IF NOT EXISTS contest (
    contest_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(500) NULL,
    description TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    host_name VARCHAR(150) NOT NULL,
    apply_start_at TIMESTAMP NULL,
    apply_end_at TIMESTAMP NOT NULL,
    announcement_at TIMESTAMP NULL,
    eligibility_text TEXT NULL,
    prize_text TEXT NULL,
    location_text VARCHAR(255) NULL,
    thumbnail_url VARCHAR(1000) NULL,
    detail_image_urls TEXT NULL,
    source_url VARCHAR(1000) NULL,
    is_team_participation BOOLEAN NOT NULL,
    min_team_size INT NULL,
    max_team_size INT NULL,
    view_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- 3. contest_scrap 테이블 생성
CREATE TABLE IF NOT EXISTS contest_scrap (
    contest_scrap_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    contest_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_contest_scrap_member FOREIGN KEY (member_id) REFERENCES member(member_id),
    CONSTRAINT fk_contest_scrap_contest FOREIGN KEY (contest_id) REFERENCES contest(contest_id),
    CONSTRAINT uq_contest_scrap_member_contest UNIQUE (member_id, contest_id)
);
