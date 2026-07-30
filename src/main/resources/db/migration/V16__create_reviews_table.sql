-- Phase 9: 팀원 리뷰 (docs/decisions/09-review.md)
-- 팀이 SUBMITTED 상태일 때 활성 팀원끼리 서로(자기 자신 제외) 1회씩 리뷰를 남긴다.

CREATE TABLE reviews (
    review_id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id                    BIGINT      NOT NULL,
    reviewer_team_member_id    BIGINT      NOT NULL,
    reviewee_team_member_id    BIGINT      NOT NULL,
    content                    TEXT        NOT NULL,
    created_at                 TIMESTAMP   NOT NULL,
    updated_at                 TIMESTAMP   NOT NULL,
    CONSTRAINT fk_reviews_team
        FOREIGN KEY (team_id) REFERENCES teams (team_id),
    CONSTRAINT fk_reviews_reviewer
        FOREIGN KEY (reviewer_team_member_id) REFERENCES team_members (team_member_id),
    CONSTRAINT fk_reviews_reviewee
        FOREIGN KEY (reviewee_team_member_id) REFERENCES team_members (team_member_id),
    CONSTRAINT uq_reviews_team_reviewer_reviewee
        UNIQUE (team_id, reviewer_team_member_id, reviewee_team_member_id)
);
