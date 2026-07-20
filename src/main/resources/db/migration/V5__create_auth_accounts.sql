CREATE TABLE IF NOT EXISTS auth_accounts (
    auth_account_id    BIGINT       AUTO_INCREMENT PRIMARY KEY,
    member_id          BIGINT       NOT NULL,
    provider           VARCHAR(20)  NOT NULL,
    provider_member_id VARCHAR(255) NULL,
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP    NOT NULL,
    CONSTRAINT fk_auth_accounts_member
        FOREIGN KEY (member_id) REFERENCES member (member_id),
    CONSTRAINT uq_auth_accounts_member_provider
        UNIQUE (member_id, provider)
);

CREATE UNIQUE INDEX uq_auth_accounts_provider_member_id
    ON auth_accounts (provider, provider_member_id);

INSERT INTO auth_accounts (member_id, provider, provider_member_id, created_at, updated_at)
SELECT member_id, 'EMAIL', NULL, created_at, updated_at
FROM member;
