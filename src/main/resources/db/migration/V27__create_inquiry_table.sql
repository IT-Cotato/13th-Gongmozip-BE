-- 문의하기: 비회원이 이메일 + 문의 비밀번호(4자리, BCrypt 해시)로 문의를 등록하고
-- 같은 조합으로 문의 내역/상세를 조회한다. 관리자가 답변하면 status가 ANSWERED로 바뀐다.

CREATE TABLE inquiry (
    inquiry_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    title           VARCHAR(20)  NOT NULL,
    content         TEXT         NOT NULL,
    status          VARCHAR(30)  NOT NULL,
    answer_content  TEXT         NULL,
    answered_at     TIMESTAMP    NULL,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL
);

-- 문의 내역 조회는 이메일로 먼저 조회 후 비밀번호를 애플리케이션에서 대조한다
CREATE INDEX idx_inquiry_email ON inquiry (email);
