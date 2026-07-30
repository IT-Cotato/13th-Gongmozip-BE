-- Phase 0: 팀 채팅/매칭 이후 협업 기능 스키마
-- 설계 근거: docs/decisions/01-team.md, 02-leader-election.md, 03-chat.md,
--           04-contest-voting.md, 05-report.md

-- 1. teams: 매칭된 팀 = 채팅방 (1:1 통합, 별도 chat_room 테이블 없음)
CREATE TABLE teams (
    team_id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    status                VARCHAR(30)  NOT NULL,
    preferred_category    VARCHAR(50)  NOT NULL,
    leader_selection_mode VARCHAR(30)  NOT NULL,
    contest_id            BIGINT       NULL,
    chatbot_enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    progress_percent      INT          NULL,
    progress_check_at     TIMESTAMP    NULL,
    submission_check_at   TIMESTAMP    NULL,
    submitted             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP    NOT NULL,
    updated_at            TIMESTAMP    NOT NULL,
    CONSTRAINT fk_teams_contest
        FOREIGN KEY (contest_id) REFERENCES contest (contest_id)
);

-- 2. team_members: 팀 ↔ 회원 조인, 참여 상태 및 팀장 선출 관련 스냅샷 보유
CREATE TABLE team_members (
    team_member_id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id                  BIGINT      NOT NULL,
    member_id                BIGINT      NOT NULL,
    profile_id               BIGINT      NOT NULL,
    role                     VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    is_pre_leader_candidate  BOOLEAN     NOT NULL DEFAULT FALSE,
    leader_candidacy         VARCHAR(20) NOT NULL DEFAULT 'UNDECIDED',
    status                   VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    greeted_at               TIMESTAMP   NULL,
    last_read_at             TIMESTAMP   NULL,
    joined_at                TIMESTAMP   NOT NULL,
    left_at                  TIMESTAMP   NULL,
    created_at               TIMESTAMP   NOT NULL,
    updated_at               TIMESTAMP   NOT NULL,
    CONSTRAINT fk_team_members_team
        FOREIGN KEY (team_id) REFERENCES teams (team_id),
    CONSTRAINT fk_team_members_member
        FOREIGN KEY (member_id) REFERENCES member (member_id),
    CONSTRAINT fk_team_members_profile
        FOREIGN KEY (profile_id) REFERENCES profiles (profile_id),
    CONSTRAINT uq_team_members_team_member
        UNIQUE (team_id, member_id)
);

-- 3. messages: 채팅 메시지 (MEMBER/CHATBOT/SYSTEM 발신, 카드형 메시지 포함)
CREATE TABLE messages (
    message_id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id                 BIGINT       NOT NULL,
    sender_type             VARCHAR(20)  NOT NULL,
    sender_team_member_id   BIGINT       NULL,
    message_type            VARCHAR(30)  NOT NULL,
    content                 TEXT         NULL,
    metadata                TEXT         NULL,
    created_at              TIMESTAMP    NOT NULL,
    updated_at               TIMESTAMP    NOT NULL,
    CONSTRAINT fk_messages_team
        FOREIGN KEY (team_id) REFERENCES teams (team_id),
    CONSTRAINT fk_messages_sender_team_member
        FOREIGN KEY (sender_team_member_id) REFERENCES team_members (team_member_id)
);

-- 4. reports: 팀원 신고
CREATE TABLE reports (
    report_id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_member_id    BIGINT       NOT NULL,
    reported_member_id    BIGINT       NOT NULL,
    team_id                BIGINT       NULL,
    reason_code            VARCHAR(30)  NOT NULL,
    custom_reason_text     VARCHAR(500) NULL,
    created_at              TIMESTAMP    NOT NULL,
    updated_at              TIMESTAMP    NOT NULL,
    CONSTRAINT fk_reports_reporter_member
        FOREIGN KEY (reporter_member_id) REFERENCES member (member_id),
    CONSTRAINT fk_reports_reported_member
        FOREIGN KEY (reported_member_id) REFERENCES member (member_id),
    CONSTRAINT fk_reports_team
        FOREIGN KEY (team_id) REFERENCES teams (team_id)
);

-- 5. leader_votes: 팀장 선출 투표 (동률 시 round 증가시켜 재투표)
CREATE TABLE leader_votes (
    leader_vote_id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id                     BIGINT    NOT NULL,
    voter_team_member_id        BIGINT    NOT NULL,
    candidate_team_member_id    BIGINT    NOT NULL,
    round                        INT       NOT NULL DEFAULT 1,
    created_at                   TIMESTAMP NOT NULL,
    updated_at                   TIMESTAMP NOT NULL,
    CONSTRAINT fk_leader_votes_team
        FOREIGN KEY (team_id) REFERENCES teams (team_id),
    CONSTRAINT fk_leader_votes_voter
        FOREIGN KEY (voter_team_member_id) REFERENCES team_members (team_member_id),
    CONSTRAINT fk_leader_votes_candidate
        FOREIGN KEY (candidate_team_member_id) REFERENCES team_members (team_member_id),
    CONSTRAINT uq_leader_votes_team_voter_candidate_round
        UNIQUE (team_id, voter_team_member_id, candidate_team_member_id, round)
);

-- 6. contest_candidates: 팀의 공모전 후보 리스트 (기존 contest 테이블 참조)
CREATE TABLE contest_candidates (
    contest_candidate_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id                       BIGINT    NOT NULL,
    contest_id                    BIGINT    NOT NULL,
    added_by_team_member_id       BIGINT    NOT NULL,
    created_at                     TIMESTAMP NOT NULL,
    updated_at                     TIMESTAMP NOT NULL,
    CONSTRAINT fk_contest_candidates_team
        FOREIGN KEY (team_id) REFERENCES teams (team_id),
    CONSTRAINT fk_contest_candidates_contest
        FOREIGN KEY (contest_id) REFERENCES contest (contest_id),
    CONSTRAINT fk_contest_candidates_added_by
        FOREIGN KEY (added_by_team_member_id) REFERENCES team_members (team_member_id),
    CONSTRAINT uq_contest_candidates_team_contest
        UNIQUE (team_id, contest_id)
);

-- 7. contest_votes: 공모전 후보 투표 (다중선택 — voter는 라운드당 여러 후보에 투표 가능,
--    후보별로 1건만 제한)
CREATE TABLE contest_votes (
    contest_vote_id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id                       BIGINT    NOT NULL,
    contest_candidate_id          BIGINT    NOT NULL,
    voter_team_member_id          BIGINT    NOT NULL,
    round                          INT       NOT NULL DEFAULT 1,
    created_at                     TIMESTAMP NOT NULL,
    updated_at                     TIMESTAMP NOT NULL,
    CONSTRAINT fk_contest_votes_team
        FOREIGN KEY (team_id) REFERENCES teams (team_id),
    CONSTRAINT fk_contest_votes_candidate
        FOREIGN KEY (contest_candidate_id) REFERENCES contest_candidates (contest_candidate_id),
    CONSTRAINT fk_contest_votes_voter
        FOREIGN KEY (voter_team_member_id) REFERENCES team_members (team_member_id),
    CONSTRAINT uq_contest_votes_candidate_voter_round
        UNIQUE (contest_candidate_id, voter_team_member_id, round)
);
