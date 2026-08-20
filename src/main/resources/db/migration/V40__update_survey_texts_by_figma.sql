-- score_weight와 is_reverse_scored는 건드리지 않으므로 기존 제출 결과는 그대로 유효
-- question_id는 환경마다 다를 수 있어 question_key로만 대상을 찾는다.

-- =====================================================
-- 1. 문항 텍스트 (15문항)
-- =====================================================
UPDATE survey_questions SET question_text = '이번 공모전에서 우리 팀은 ___를(을) 목표로 했으면 한다.'
WHERE question_key = 'GOAL_PREFERENCE';

UPDATE survey_questions SET question_text = '내가 가장 선호하는 업무 스타일은 ___이다.'
WHERE question_key = 'WORK_STYLE';

UPDATE survey_questions SET question_text = '나는 팀원들과 ___방식으로 소통하는 것을 선호한다.'
WHERE question_key = 'COMMUNICATION_STYLE';

UPDATE survey_questions SET question_text = '나는 나에게 크게 잘못 대한 사람한테도 거의 원한을 품지 않는다.'
WHERE question_key = 'AGREEABLENESS_1';

UPDATE survey_questions SET question_text = '나는 다른 사람이 반대 의견을 낼 때 꽤 유연하게 생각을 바꾸는 편이다.'
WHERE question_key = 'AGREEABLENESS_2';

UPDATE survey_questions SET question_text = '사람들은 때때로 내가 너무 고집스럽다고 이야기한다.'
WHERE question_key = 'AGREEABLENESS_3';

UPDATE survey_questions SET question_text = '나는 목표를 달성하고자 할 때, 매우 열심히 스스로를 몰아붙인다.'
WHERE question_key = 'CONSCIENTIOUSNESS_1';

UPDATE survey_questions SET question_text = '나는 시간이 걸리더라도 항상 정확하게 일하려고 노력한다.'
WHERE question_key = 'CONSCIENTIOUSNESS_2';

UPDATE survey_questions SET question_text = '나는 감당할 수 있는 최소한의 일만 하는 편이다.'
WHERE question_key = 'CONSCIENTIOUSNESS_3';

UPDATE survey_questions SET question_text = '나는 승진이나 임금 인상을 위해, 도움이 되더라도 아부하지 않을 것이다.'
WHERE question_key = 'HONESTY_HUMILITY_1';

UPDATE survey_questions SET question_text = '나는 평균적인 사람보다 더 많은 존중을 받을 자격이 있다고 생각한다.'
WHERE question_key = 'HONESTY_HUMILITY_2';

UPDATE survey_questions SET question_text = '절대 들키지 않는다는 걸 안다면, 나는 15억을 훔칠 의향이 있다.'
WHERE question_key = 'HONESTY_HUMILITY_3';

UPDATE survey_questions SET question_text = '나는 혼자 일하는 것보다 적극적인 사회적 상호작용이 있는 일을 더 선호한다.'
WHERE question_key = 'EXTROVERSION_1';

UPDATE survey_questions SET question_text = '나는 사람들과 교류할 때 보통 먼저 다가가는 편이다.'
WHERE question_key = 'EXTROVERSION_2';

UPDATE survey_questions SET question_text = '나는 단체 회의에서 내 의견을 거의 표현하지 않는다.'
WHERE question_key = 'EXTROVERSION_3';


-- =====================================================
-- 2. Q1 목표 선택지 — 5점이 맨 위로 (기능명세서 나열 순서 복원)
-- =====================================================
UPDATE survey_options SET option_label = '무조건 수상! 결과물 완성도와 스펙이 최우선', display_order = 1
WHERE option_key = 'GOAL_PREFERENCE_OPT_5';

UPDATE survey_options SET option_label = '수상도 중요하지만, 그 과정에서의 배움도 중요', display_order = 2
WHERE option_key = 'GOAL_PREFERENCE_OPT_3';

UPDATE survey_options SET option_label = '수상보다는 새로운 경험, 네트워킹과 나의 성장이 최우선', display_order = 3
WHERE option_key = 'GOAL_PREFERENCE_OPT_1';


-- =====================================================
-- 3. Q2 업무 스타일 선택지 — 5점이 맨 위로
-- =====================================================
UPDATE survey_options SET option_label = '계획과 역할을 명확히 정하고 마일스톤을 꼼꼼히 관리하는 것', display_order = 1
WHERE option_key = 'WORK_STYLE_OPT_5';

UPDATE survey_options SET option_label = '큰 틀만 잡고 상황에 따라 유동적으로 분담하는 것', display_order = 2
WHERE option_key = 'WORK_STYLE_OPT_3';

UPDATE survey_options SET option_label = '필요할 때마다 모여서 유연하게 대처하며 협업하는 것', display_order = 3
WHERE option_key = 'WORK_STYLE_OPT_1';


-- =====================================================
-- 4. Q3 소통 방식 선택지 — 순서는 기존 유지, 문구만 수정
-- =====================================================
UPDATE survey_options SET option_label = '텍스트/비대면 위주의 빠르고 효율적인 방식'
WHERE option_key = 'COMMUNICATION_STYLE_OPT_1';

UPDATE survey_options SET option_label = '평소엔 온라인, 중요한 의사결정은 대면하는 혼합형 방식'
WHERE option_key = 'COMMUNICATION_STYLE_OPT_3';

UPDATE survey_options SET option_label = '자주 만나서 아이디어를 나누는 대면 위주의 방식'
WHERE option_key = 'COMMUNICATION_STYLE_OPT_5';


-- =====================================================
-- 5. Q4~Q15 5점 척도 선택지 (RATING 12문항) — 순서 역전 + 라벨 수정
--    option_value별로 일괄 적용한다. score_weight는 그대로 두므로
--    정방향/역채점 채점 결과는 변하지 않는다.
-- =====================================================
UPDATE survey_options SET option_label = '매우그렇다', display_order = 1
WHERE option_value = '5'
  AND question_id IN (SELECT question_id FROM survey_questions WHERE question_type = 'RATING');

UPDATE survey_options SET option_label = '그렇다', display_order = 2
WHERE option_value = '4'
  AND question_id IN (SELECT question_id FROM survey_questions WHERE question_type = 'RATING');

UPDATE survey_options SET option_label = '보통이다', display_order = 3
WHERE option_value = '3'
  AND question_id IN (SELECT question_id FROM survey_questions WHERE question_type = 'RATING');

UPDATE survey_options SET option_label = '그렇지 않다', display_order = 4
WHERE option_value = '2'
  AND question_id IN (SELECT question_id FROM survey_questions WHERE question_type = 'RATING');

UPDATE survey_options SET option_label = '매우그렇지 않다', display_order = 5
WHERE option_value = '1'
  AND question_id IN (SELECT question_id FROM survey_questions WHERE question_type = 'RATING');
