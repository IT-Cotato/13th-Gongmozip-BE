-- 챗봇 자유질의(@챗봇) 응답 등 Message.content(TEXT, 무제한)가 그대로 들어올 수 있어
-- VARCHAR(500)로는 길이가 부족했다. 초과 시 INSERT 실패로 트랜잭션이 롤백되면서 이미
-- WebSocket으로 브로드캐스트된 메시지와 어긋나는 문제가 있어 TEXT로 넓힌다.
ALTER TABLE notifications
    MODIFY COLUMN body TEXT NOT NULL;
