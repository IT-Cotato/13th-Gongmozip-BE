CREATE TABLE notifications (
    notification_id    BIGINT       AUTO_INCREMENT PRIMARY KEY,
    receiver_member_id BIGINT       NOT NULL,
    category            VARCHAR(20)  NOT NULL,
    body                VARCHAR(500) NOT NULL,
    related_team_id     BIGINT       NULL,
    is_read             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          DATETIME     NOT NULL,
    updated_at          DATETIME     NOT NULL,
    CONSTRAINT fk_notifications_receiver_member FOREIGN KEY (receiver_member_id) REFERENCES member (member_id)
);

-- 알림함 커서 페이지네이션(전체/카테고리 탭 공통) 조회 패턴을 커버한다
CREATE INDEX idx_notifications_receiver_category_id ON notifications (receiver_member_id, category, notification_id DESC);

-- 홈 화면 배지(unread-exists)의 EXISTS 조회, 알림함 진입 시 read-all 일괄 UPDATE 조건을 커버한다
CREATE INDEX idx_notifications_receiver_read ON notifications (receiver_member_id, is_read);
