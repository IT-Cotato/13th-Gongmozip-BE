-- Figma 5.1.3.6.1~6.5 와이어프레임 확인 결과, 팀원 리뷰는 자유 텍스트가 아니라
-- 3점 척도 응답 2개 + 다중 선택 키워드로 구성된다 (docs/decisions/09-review.md).

ALTER TABLE reviews DROP COLUMN content;
ALTER TABLE reviews ADD COLUMN communication_score VARCHAR(20) NOT NULL DEFAULT 'NEUTRAL';
ALTER TABLE reviews ADD COLUMN participation_score VARCHAR(20) NOT NULL DEFAULT 'NEUTRAL';
ALTER TABLE reviews ADD COLUMN keywords TEXT NOT NULL DEFAULT '[]';
