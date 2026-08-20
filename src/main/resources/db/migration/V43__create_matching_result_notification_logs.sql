-- 매칭 결과 공개 알림(MatchingResultNotificationJobs)을 하루에 한 번만 보내기 위한 멱등성 기록.
-- 스케줄러가 5분마다 폴링하며 MatchingTimePolicy.isResultPublished를 직접 확인하는 방식으로 바뀌면서,
-- 신청일당 한 번만 알림이 나가도록 이 테이블의 존재 여부로 판단한다.
CREATE TABLE matching_result_notification_logs (
    matching_result_notification_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_date                    DATE     NOT NULL,
    created_at                          DATETIME NOT NULL,
    updated_at                          DATETIME NOT NULL,
    CONSTRAINT uq_matching_result_notification_logs_date UNIQUE (application_date)
);
