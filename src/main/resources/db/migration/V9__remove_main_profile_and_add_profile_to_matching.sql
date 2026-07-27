-- 1. matching_applications 테이블에 profile_id 컬럼 추가 (기존 데이터 호환을 위해 처음엔 NULL 허용)
ALTER TABLE matching_applications ADD COLUMN profile_id BIGINT NULL;

-- 2. 기존 matching_applications 데이터 복구: 회원의 대표 프로필(is_main=true)을 우선으로 연결
UPDATE matching_applications ma
SET ma.profile_id = (
    SELECT p.profile_id
    FROM profiles p
    WHERE p.member_id = ma.member_id
    ORDER BY p.is_main DESC, p.created_at ASC
    LIMIT 1
);

-- 3. profile_id 컬럼을 NOT NULL 제약조건으로 변경
ALTER TABLE matching_applications MODIFY COLUMN profile_id BIGINT NOT NULL;

-- 4. profiles 테이블과의 외래키(FK) 제약조건 추가
ALTER TABLE matching_applications
    ADD CONSTRAINT fk_matching_applications_profile
    FOREIGN KEY (profile_id) REFERENCES profiles (profile_id);

-- 5. fk_profiles_member 외래키가 사용하던 uq_member_is_main_unique 인덱스를 대체할 인덱스 생성
ALTER TABLE profiles ADD INDEX idx_profiles_member_id (member_id);

-- 6. profiles 테이블의 대표 프로필 관련 unique 제약 조건 및 컬럼 삭제
ALTER TABLE profiles DROP CONSTRAINT uq_member_is_main_unique;
ALTER TABLE profiles DROP COLUMN is_main;
ALTER TABLE profiles DROP COLUMN is_main_unique;
