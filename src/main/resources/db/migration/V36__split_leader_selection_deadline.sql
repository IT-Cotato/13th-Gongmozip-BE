-- V36: 팀장 "후보 등록"(팀장 여부 투표) 마감과 "투표" 마감을 분리한다. 기존에는
--      leader_selection_deadline_at 하나가 두 하위 단계를 모두 커버해서, 후보 등록이 늦게
--      끝나면 투표 시간이 그만큼 줄어드는 문제가 있었다 (docs/decisions/02-leader-election.md
--      참고). leader_selection_deadline_at을 leader_candidacy_deadline_at으로 이름을 바꿔
--      "후보 등록 마감"만 의미하게 하고, "투표 마감"을 위한 leader_vote_deadline_at을 새로
--      추가한다. 투표 마감은 투표 라운드(재투표 포함)가 새로 열릴 때마다 다시 세팅된다.

ALTER TABLE teams RENAME COLUMN leader_selection_deadline_at TO leader_candidacy_deadline_at;
ALTER TABLE teams ADD COLUMN leader_vote_deadline_at TIMESTAMP NULL;
