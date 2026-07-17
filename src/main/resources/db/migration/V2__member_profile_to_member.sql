-- 1. member 테이블에 컬럼 추가
ALTER TABLE member
    ADD COLUMN birth_date DATE NULL,
    ADD COLUMN gender VARCHAR(20) NULL;

-- 2. 기존 member_profiles 데이터 이관 (MySQL & H2 범용 서브쿼리 문법)
UPDATE member m
SET m.birth_date = (SELECT mp.birth_date FROM member_profiles mp WHERE mp.member_id = m.member_id),
    m.gender = (SELECT mp.gender FROM member_profiles mp WHERE mp.member_id = m.member_id)
WHERE EXISTS (
    SELECT 1 FROM member_profiles mp WHERE mp.member_id = m.member_id
);

-- 3. 기존 member_profiles 테이블 제거
DROP TABLE member_profiles;
