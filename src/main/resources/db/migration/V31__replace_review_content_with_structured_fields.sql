-- Figma 5.1.3.6.1~6.5 와이어프레임 확인 결과, 팀원 리뷰는 자유 텍스트가 아니라
-- 3점 척도 응답 2개 + 다중 선택 키워드로 구성된다 (docs/decisions/09-review.md).

-- 기존 자유 텍스트는 새 구조화 필드로 자동 환산할 수 없어(척도/키워드로 역산 불가) 드롭 대신
-- legacy_content로 이름만 바꿔 보존한다. 애플리케이션 코드(Review 엔티티)는 이 컬럼을 더 이상
-- 참조하지 않으며, 필요 시 수동 조회/백업 용도로만 남겨둔다.
ALTER TABLE reviews RENAME COLUMN content TO legacy_content;
ALTER TABLE reviews ALTER COLUMN legacy_content DROP NOT NULL;
ALTER TABLE reviews ADD COLUMN communication_score VARCHAR(20) NOT NULL DEFAULT 'NEUTRAL';
ALTER TABLE reviews ADD COLUMN participation_score VARCHAR(20) NOT NULL DEFAULT 'NEUTRAL';
ALTER TABLE reviews ADD COLUMN keywords TEXT NOT NULL DEFAULT '[]';
