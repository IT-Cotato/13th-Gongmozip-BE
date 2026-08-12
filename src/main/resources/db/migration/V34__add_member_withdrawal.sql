ALTER TABLE member ADD COLUMN withdrawn_at DATETIME DEFAULT NULL;
ALTER TABLE member ADD COLUMN anonymized BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS member_withdrawal_reasons (
    withdrawal_reason_id BIGINT       AUTO_INCREMENT PRIMARY KEY,
    reason               VARCHAR(30)  NOT NULL,
    reason_detail        VARCHAR(500) NULL,
    created_at           TIMESTAMP    NOT NULL,
    updated_at           TIMESTAMP    NOT NULL
);

CREATE INDEX idx_member_status_withdrawn_at ON member (status, withdrawn_at);
