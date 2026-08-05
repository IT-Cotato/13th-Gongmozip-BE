-- V26: member 테이블에 마이페이지 상세 정보 관련 컬럼 추가
-- 이름, SNS 연동 타입, SNS 이메일, 이메일 마케팅 수신동의, SMS 마케팅 수신동의

ALTER TABLE member
    ADD COLUMN name VARCHAR(50) NULL;

ALTER TABLE member
    ADD COLUMN sns_type VARCHAR(50) NULL;

ALTER TABLE member
    ADD COLUMN sns_email VARCHAR(255) NULL;

ALTER TABLE member
    ADD COLUMN marketing_consent_email BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE member
    ADD COLUMN marketing_consent_sms BOOLEAN NOT NULL DEFAULT FALSE;
