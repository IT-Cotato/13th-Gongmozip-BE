-- matching_explanations 초기 시드 데이터 삽입
INSERT INTO matching_explanations (title, summary, sections, disclaimer)
VALUES (
    'AI 분석 매칭은 어떻게 이루어지나요?',
    '사용자의 프로젝트 경험과 협업 성향을 분석하여 적합한 팀 조합과 추천 사유를 제공합니다.',
    '[{"type":"AI_MATCHING","title":"AI 분석 매칭","description":"팀 목표, 협업 방식, 프로젝트 경험과 성향 분석 결과를 종합하여 매칭 결과를 설명합니다.","items":[]},{"type":"HEXACO","title":"HEXACO 성격 분석","description":"여섯 가지 성격 요인을 바탕으로 사용자의 협업 성향을 분석합니다.","items":[{"title":"정직-겸손성","description":"공정성과 진실성을 중요하게 생각하는 경향을 의미합니다."},{"title":"정서성","description":"위험이나 스트레스 상황에 반응하고 다른 사람과 정서적으로 교류하는 경향을 의미합니다."},{"title":"외향성","description":"다른 사람과 적극적으로 교류하고 의견을 표현하는 경향을 의미합니다."},{"title":"우호성","description":"갈등 상황에서 상대를 이해하고 원만하게 조율하려는 경향을 의미합니다."},{"title":"성실성","description":"계획을 세우고 맡은 일을 책임감 있게 수행하려는 경향을 의미합니다."},{"title":"개방성","description":"새로운 아이디어와 경험을 받아들이려는 경향을 의미합니다."}]},{"type":"TEAM_SYNERGY","title":"팀 시너지","description":"팀 목표, 일정 관리 방식, 소통 방식과 성향 조합을 바탕으로 함께 협업할 때 기대되는 강점을 설명합니다.","items":[]},{"type":"EXTRAVERSION_COMPLEMENT","title":"외향성 상보성","description":"외향성 수준이 서로 다른 팀원들이 의견 전달, 발표, 집중 작업 등에서 역할을 나누며 상호 보완할 가능성을 의미합니다.","items":[]}]',
    'AI 분석 결과는 팀 구성을 돕기 위한 참고 자료이며 사용자의 능력이나 성격을 단정하지 않습니다.'
);
