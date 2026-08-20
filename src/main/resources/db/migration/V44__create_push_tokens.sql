CREATE TABLE push_tokens (
    push_token_id BIGINT       AUTO_INCREMENT PRIMARY KEY,
    member_id     BIGINT       NOT NULL,
    token         VARCHAR(500) NOT NULL,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL,
    CONSTRAINT fk_push_tokens_member FOREIGN KEY (member_id) REFERENCES member (member_id),
    CONSTRAINT uq_push_tokens_token UNIQUE (token)
);

-- 회원별 토큰 조회(발송 시 회원 목록 -> 토큰 목록)를 커버한다. uq_push_tokens_token이 이미 token 단독
-- 조회는 커버하므로 별도 인덱스를 두지 않는다.
CREATE INDEX idx_push_tokens_member ON push_tokens (member_id);
