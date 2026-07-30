-- Phase 3: 협업거리 포인트 인프라 (docs/decisions/06-collaboration-point.md)
-- 매칭 신청 시점의 matching_applications.collaboration_distance(희망 거리, 정적 값)와는
-- 완전히 다른 개념이므로 별도 컬럼/테이블로 관리한다.

ALTER TABLE member
    ADD COLUMN collaboration_point INT NOT NULL DEFAULT 0;

CREATE TABLE collaboration_point_histories (
    collaboration_point_history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id                       BIGINT      NOT NULL,
    team_id                         BIGINT      NULL,
    delta                           INT         NOT NULL,
    reason_code                     VARCHAR(30) NOT NULL,
    created_at                      TIMESTAMP   NOT NULL,
    updated_at                      TIMESTAMP   NOT NULL,
    CONSTRAINT fk_collaboration_point_histories_member
        FOREIGN KEY (member_id) REFERENCES member (member_id),
    CONSTRAINT fk_collaboration_point_histories_team
        FOREIGN KEY (team_id) REFERENCES teams (team_id)
);
