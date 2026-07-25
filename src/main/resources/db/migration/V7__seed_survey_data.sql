-- =====================================================
-- survey_questions (16개)
-- =====================================================
INSERT INTO survey_questions (question_key, question_text, question_type, is_required, is_reverse_scored,
                               display_order, help_text, placeholder_text, min_value, max_value, created_at,
                               updated_at)
VALUES
    -- 리더 선호 (1개)
    ('LEADER_PREFERENCE', '팀에서 리더 역할을 원하시나요?',
     'SINGLE_CHOICE', true, false, 1, null, null, null, null, NOW(), NOW()),

    -- 팀 성향 (3개)
    ('GOAL_PREFERENCE', '이번 공모전에서 우리 팀의 최우선 목표는 무엇이었으면 하나요?',
     'SINGLE_CHOICE', true, false, 2, null, null, null, null, NOW(), NOW()),
    ('WORK_STYLE', '어떤 방식으로 일정을 관리하고 작업하는 것을 선호하나요?',
     'SINGLE_CHOICE', true, false, 3, null, null, null, null, NOW(), NOW()),
    ('COMMUNICATION_STYLE', '팀원들과 어떤 형태의 소통을 선호하나요?',
     'SINGLE_CHOICE', true, false, 4, null, null, null, null, NOW(), NOW()),

    -- 우호성 (3개)
    ('AGREEABLENESS_1', '나는 나를 크게 잘못 대한 사람에게도 거의 원한을 품지 않는다.',
     'RATING', true, false, 5, null, null, 1, 5, NOW(), NOW()),
    ('AGREEABLENESS_2', '나는 다른 사람이 반대 의견을 낼 때 보통 꽤 유연하게 생각을 바꾸는 편이다.',
     'RATING', true, false, 6, null, null, 1, 5, NOW(), NOW()),
    ('AGREEABLENESS_3', '사람들은 때때로 내가 너무 고집스럽다고 이야기한다.',
     'RATING', true, true, 7, null, null, 1, 5, NOW(), NOW()),

    -- 성실성 (3개)
    ('CONSCIENTIOUSNESS_1', '나는 목표를 달성하려 할 때 종종 매우 열심히 자신을 몰아붙인다.',
     'RATING', true, false, 8, null, null, 1, 5, NOW(), NOW()),
    ('CONSCIENTIOUSNESS_2', '나는 시간이 걸리더라도 항상 정확하게 일하려고 노력한다.',
     'RATING', true, false, 9, null, null, 1, 5, NOW(), NOW()),
    ('CONSCIENTIOUSNESS_3', '나는 그냥 버틸 수 있을 최소한의 일만 한다.',
     'RATING', true, true, 10, null, null, 1, 5, NOW(), NOW()),

    -- 정직겸손성 (3개)
    ('HONESTY_HUMILITY_1', '나는 승진이나 임금 인상을 위해, 효과가 있을 것을 알더라도 아부하지 않을 것이다.',
     'RATING', true, false, 11, null, null, 1, 5, NOW(), NOW()),
    ('HONESTY_HUMILITY_2', '나는 평균적인 사람보다 더 많은 존중을 받을 자격이 있다고 생각한다.',
     'RATING', true, true, 12, null, null, 1, 5, NOW(), NOW()),
    ('HONESTY_HUMILITY_3', '절대 들키지 않는다는 걸 안다면, 나는 15억을 훔칠 의향이 있다.',
     'RATING', true, true, 13, null, null, 1, 5, NOW(), NOW()),

    -- 외향성 (3개)
    ('EXTROVERSION_1', '나는 혼자 일하는 것보다 적극적인 사회적 상호작용이 있는 일을 더 선호한다.',
     'RATING', true, false, 14, null, null, 1, 5, NOW(), NOW()),
    ('EXTROVERSION_2', '나는 사회적 상황에서 보통 먼저 다가가는 편이다.',
     'RATING', true, false, 15, null, null, 1, 5, NOW(), NOW()),
    ('EXTROVERSION_3', '나는 단체 회의에서 내 의견을 거의 표현하지 않는다.',
     'RATING', true, true, 16, null, null, 1, 5, NOW(), NOW());


-- =====================================================
-- survey_options (72개)
-- question_id는 question_key로 참조
-- =====================================================

-- -----------------------------------------------------
-- LEADER_PREFERENCE (3개)
-- score_weight: 유효 리더 수 L 계산에 직접 사용
-- -----------------------------------------------------
INSERT INTO survey_options (question_id, option_key, option_label, option_value, display_order, score_weight,
                             is_other_option, created_at, updated_at)
SELECT question_id, 'LEADER_PREFERENCE_YES', '리더 원합니다', 'YES', 1, 1.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'LEADER_PREFERENCE'
UNION ALL
SELECT question_id, 'LEADER_PREFERENCE_NEUTRAL', '상관없어요', 'NEUTRAL', 2, 0.50, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'LEADER_PREFERENCE'
UNION ALL
SELECT question_id, 'LEADER_PREFERENCE_NO', '리더를 원하지 않아요', 'NO', 3, 0.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'LEADER_PREFERENCE';

-- -----------------------------------------------------
-- GOAL_PREFERENCE (3개)
-- score_weight: 팀 내 분산 계산에 사용 (1 / 3 / 5)
-- -----------------------------------------------------
INSERT INTO survey_options (question_id, option_key, option_label, option_value, display_order, score_weight,
                             is_other_option, created_at, updated_at)
SELECT question_id, 'GOAL_PREFERENCE_OPT_1', '수상보다는 새로운 경험, 네트워킹, 나의 성장이 최우선', '1', 1, 1.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'GOAL_PREFERENCE'
UNION ALL
SELECT question_id, 'GOAL_PREFERENCE_OPT_3', '수상도 중요하지만, 그 과정에서의 배움도 중요', '3', 2, 3.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'GOAL_PREFERENCE'
UNION ALL
SELECT question_id, 'GOAL_PREFERENCE_OPT_5', '무조건 수상! 결과물 완성도와 스펙이 최우선', '5', 3, 5.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'GOAL_PREFERENCE';

-- -----------------------------------------------------
-- WORK_STYLE (3개)
-- -----------------------------------------------------
INSERT INTO survey_options (question_id, option_key, option_label, option_value, display_order, score_weight,
                             is_other_option, created_at, updated_at)
SELECT question_id, 'WORK_STYLE_OPT_1', '필요할 때마다 모여서 유연하게 대처하며 협업', '1', 1, 1.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'WORK_STYLE'
UNION ALL
SELECT question_id, 'WORK_STYLE_OPT_3', '큰 틀만 잡고 상황에 따라 유동적으로 분담', '3', 2, 3.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'WORK_STYLE'
UNION ALL
SELECT question_id, 'WORK_STYLE_OPT_5', '철저한 사전 기획, 명확한 역할 분담, 꼼꼼한 마일스톤 관리', '5', 3, 5.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'WORK_STYLE';

-- -----------------------------------------------------
-- COMMUNICATION_STYLE (3개)
-- -----------------------------------------------------
INSERT INTO survey_options (question_id, option_key, option_label, option_value, display_order, score_weight,
                             is_other_option, created_at, updated_at)
SELECT question_id, 'COMMUNICATION_STYLE_OPT_1', '텍스트/온라인(비대면) 위주의 빠르고 효율적인 소통', '1', 1, 1.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'COMMUNICATION_STYLE'
UNION ALL
SELECT question_id, 'COMMUNICATION_STYLE_OPT_3', '평소엔 온라인, 중요한 의사결정은 대면', '3', 2, 3.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'COMMUNICATION_STYLE'
UNION ALL
SELECT question_id, 'COMMUNICATION_STYLE_OPT_5', '자주 만나서 아이디어를 나누는 대면 위주의 밀도 있는 소통', '5', 3, 5.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'COMMUNICATION_STYLE';

-- -----------------------------------------------------
-- AGREEABLENESS_1 — 정방향 (score_weight: 1→1, 5→5)
-- -----------------------------------------------------
INSERT INTO survey_options (question_id, option_key, option_label, option_value, display_order, score_weight,
                             is_other_option, created_at, updated_at)
SELECT question_id, 'AGREEABLENESS_1_OPT_1', '전혀 그렇지 않다', '1', 1, 1.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'AGREEABLENESS_1'
UNION ALL
SELECT question_id, 'AGREEABLENESS_1_OPT_2', '그렇지 않다', '2', 2, 2.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'AGREEABLENESS_1'
UNION ALL
SELECT question_id, 'AGREEABLENESS_1_OPT_3', '보통이다', '3', 3, 3.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'AGREEABLENESS_1'
UNION ALL
SELECT question_id, 'AGREEABLENESS_1_OPT_4', '그렇다', '4', 4, 4.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'AGREEABLENESS_1'
UNION ALL
SELECT question_id, 'AGREEABLENESS_1_OPT_5', '매우 그렇다', '5', 5, 5.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'AGREEABLENESS_1';

-- -----------------------------------------------------
-- AGREEABLENESS_2 — 정방향
-- -----------------------------------------------------
INSERT INTO survey_options (question_id, option_key, option_label, option_value, display_order, score_weight,
                             is_other_option, created_at, updated_at)
SELECT question_id, 'AGREEABLENESS_2_OPT_1', '전혀 그렇지 않다', '1', 1, 1.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'AGREEABLENESS_2'
UNION ALL
SELECT question_id, 'AGREEABLENESS_2_OPT_2', '그렇지 않다', '2', 2, 2.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'AGREEABLENESS_2'
UNION ALL
SELECT question_id, 'AGREEABLENESS_2_OPT_3', '보통이다', '3', 3, 3.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'AGREEABLENESS_2'
UNION ALL
SELECT question_id, 'AGREEABLENESS_2_OPT_4', '그렇다', '4', 4, 4.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'AGREEABLENESS_2'
UNION ALL
SELECT question_id, 'AGREEABLENESS_2_OPT_5', '매우 그렇다', '5', 5, 5.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'AGREEABLENESS_2';

-- -----------------------------------------------------
-- AGREEABLENESS_3 — 역채점 (score_weight: 1→5, 5→1)
-- -----------------------------------------------------
INSERT INTO survey_options (question_id, option_key, option_label, option_value, display_order, score_weight,
                             is_other_option, created_at, updated_at)
SELECT question_id, 'AGREEABLENESS_3_OPT_1', '전혀 그렇지 않다', '1', 1, 5.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'AGREEABLENESS_3'
UNION ALL
SELECT question_id, 'AGREEABLENESS_3_OPT_2', '그렇지 않다', '2', 2, 4.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'AGREEABLENESS_3'
UNION ALL
SELECT question_id, 'AGREEABLENESS_3_OPT_3', '보통이다', '3', 3, 3.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'AGREEABLENESS_3'
UNION ALL
SELECT question_id, 'AGREEABLENESS_3_OPT_4', '그렇다', '4', 4, 2.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'AGREEABLENESS_3'
UNION ALL
SELECT question_id, 'AGREEABLENESS_3_OPT_5', '매우 그렇다', '5', 5, 1.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'AGREEABLENESS_3';

-- -----------------------------------------------------
-- CONSCIENTIOUSNESS_1 — 정방향
-- -----------------------------------------------------
INSERT INTO survey_options (question_id, option_key, option_label, option_value, display_order, score_weight,
                             is_other_option, created_at, updated_at)
SELECT question_id, 'CONSCIENTIOUSNESS_1_OPT_1', '전혀 그렇지 않다', '1', 1, 1.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'CONSCIENTIOUSNESS_1'
UNION ALL
SELECT question_id, 'CONSCIENTIOUSNESS_1_OPT_2', '그렇지 않다', '2', 2, 2.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'CONSCIENTIOUSNESS_1'
UNION ALL
SELECT question_id, 'CONSCIENTIOUSNESS_1_OPT_3', '보통이다', '3', 3, 3.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'CONSCIENTIOUSNESS_1'
UNION ALL
SELECT question_id, 'CONSCIENTIOUSNESS_1_OPT_4', '그렇다', '4', 4, 4.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'CONSCIENTIOUSNESS_1'
UNION ALL
SELECT question_id, 'CONSCIENTIOUSNESS_1_OPT_5', '매우 그렇다', '5', 5, 5.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'CONSCIENTIOUSNESS_1';

-- -----------------------------------------------------
-- CONSCIENTIOUSNESS_2 — 정방향
-- -----------------------------------------------------
INSERT INTO survey_options (question_id, option_key, option_label, option_value, display_order, score_weight,
                             is_other_option, created_at, updated_at)
SELECT question_id, 'CONSCIENTIOUSNESS_2_OPT_1', '전혀 그렇지 않다', '1', 1, 1.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'CONSCIENTIOUSNESS_2'
UNION ALL
SELECT question_id, 'CONSCIENTIOUSNESS_2_OPT_2', '그렇지 않다', '2', 2, 2.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'CONSCIENTIOUSNESS_2'
UNION ALL
SELECT question_id, 'CONSCIENTIOUSNESS_2_OPT_3', '보통이다', '3', 3, 3.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'CONSCIENTIOUSNESS_2'
UNION ALL
SELECT question_id, 'CONSCIENTIOUSNESS_2_OPT_4', '그렇다', '4', 4, 4.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'CONSCIENTIOUSNESS_2'
UNION ALL
SELECT question_id, 'CONSCIENTIOUSNESS_2_OPT_5', '매우 그렇다', '5', 5, 5.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'CONSCIENTIOUSNESS_2';

-- -----------------------------------------------------
-- CONSCIENTIOUSNESS_3 — 역채점
-- -----------------------------------------------------
INSERT INTO survey_options (question_id, option_key, option_label, option_value, display_order, score_weight,
                             is_other_option, created_at, updated_at)
SELECT question_id, 'CONSCIENTIOUSNESS_3_OPT_1', '전혀 그렇지 않다', '1', 1, 5.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'CONSCIENTIOUSNESS_3'
UNION ALL
SELECT question_id, 'CONSCIENTIOUSNESS_3_OPT_2', '그렇지 않다', '2', 2, 4.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'CONSCIENTIOUSNESS_3'
UNION ALL
SELECT question_id, 'CONSCIENTIOUSNESS_3_OPT_3', '보통이다', '3', 3, 3.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'CONSCIENTIOUSNESS_3'
UNION ALL
SELECT question_id, 'CONSCIENTIOUSNESS_3_OPT_4', '그렇다', '4', 4, 2.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'CONSCIENTIOUSNESS_3'
UNION ALL
SELECT question_id, 'CONSCIENTIOUSNESS_3_OPT_5', '매우 그렇다', '5', 5, 1.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'CONSCIENTIOUSNESS_3';

-- -----------------------------------------------------
-- HONESTY_HUMILITY_1 — 정방향
-- -----------------------------------------------------
INSERT INTO survey_options (question_id, option_key, option_label, option_value, display_order, score_weight,
                             is_other_option, created_at, updated_at)
SELECT question_id, 'HONESTY_HUMILITY_1_OPT_1', '전혀 그렇지 않다', '1', 1, 1.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'HONESTY_HUMILITY_1'
UNION ALL
SELECT question_id, 'HONESTY_HUMILITY_1_OPT_2', '그렇지 않다', '2', 2, 2.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'HONESTY_HUMILITY_1'
UNION ALL
SELECT question_id, 'HONESTY_HUMILITY_1_OPT_3', '보통이다', '3', 3, 3.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'HONESTY_HUMILITY_1'
UNION ALL
SELECT question_id, 'HONESTY_HUMILITY_1_OPT_4', '그렇다', '4', 4, 4.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'HONESTY_HUMILITY_1'
UNION ALL
SELECT question_id, 'HONESTY_HUMILITY_1_OPT_5', '매우 그렇다', '5', 5, 5.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'HONESTY_HUMILITY_1';

-- -----------------------------------------------------
-- HONESTY_HUMILITY_2 — 역채점
-- -----------------------------------------------------
INSERT INTO survey_options (question_id, option_key, option_label, option_value, display_order, score_weight,
                             is_other_option, created_at, updated_at)
SELECT question_id, 'HONESTY_HUMILITY_2_OPT_1', '전혀 그렇지 않다', '1', 1, 5.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'HONESTY_HUMILITY_2'
UNION ALL
SELECT question_id, 'HONESTY_HUMILITY_2_OPT_2', '그렇지 않다', '2', 2, 4.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'HONESTY_HUMILITY_2'
UNION ALL
SELECT question_id, 'HONESTY_HUMILITY_2_OPT_3', '보통이다', '3', 3, 3.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'HONESTY_HUMILITY_2'
UNION ALL
SELECT question_id, 'HONESTY_HUMILITY_2_OPT_4', '그렇다', '4', 4, 2.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'HONESTY_HUMILITY_2'
UNION ALL
SELECT question_id, 'HONESTY_HUMILITY_2_OPT_5', '매우 그렇다', '5', 5, 1.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'HONESTY_HUMILITY_2';

-- -----------------------------------------------------
-- HONESTY_HUMILITY_3 — 역채점
-- -----------------------------------------------------
INSERT INTO survey_options (question_id, option_key, option_label, option_value, display_order, score_weight,
                             is_other_option, created_at, updated_at)
SELECT question_id, 'HONESTY_HUMILITY_3_OPT_1', '전혀 그렇지 않다', '1', 1, 5.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'HONESTY_HUMILITY_3'
UNION ALL
SELECT question_id, 'HONESTY_HUMILITY_3_OPT_2', '그렇지 않다', '2', 2, 4.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'HONESTY_HUMILITY_3'
UNION ALL
SELECT question_id, 'HONESTY_HUMILITY_3_OPT_3', '보통이다', '3', 3, 3.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'HONESTY_HUMILITY_3'
UNION ALL
SELECT question_id, 'HONESTY_HUMILITY_3_OPT_4', '그렇다', '4', 4, 2.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'HONESTY_HUMILITY_3'
UNION ALL
SELECT question_id, 'HONESTY_HUMILITY_3_OPT_5', '매우 그렇다', '5', 5, 1.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'HONESTY_HUMILITY_3';

-- -----------------------------------------------------
-- EXTROVERSION_1 — 정방향
-- -----------------------------------------------------
INSERT INTO survey_options (question_id, option_key, option_label, option_value, display_order, score_weight,
                             is_other_option, created_at, updated_at)
SELECT question_id, 'EXTROVERSION_1_OPT_1', '전혀 그렇지 않다', '1', 1, 1.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'EXTROVERSION_1'
UNION ALL
SELECT question_id, 'EXTROVERSION_1_OPT_2', '그렇지 않다', '2', 2, 2.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'EXTROVERSION_1'
UNION ALL
SELECT question_id, 'EXTROVERSION_1_OPT_3', '보통이다', '3', 3, 3.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'EXTROVERSION_1'
UNION ALL
SELECT question_id, 'EXTROVERSION_1_OPT_4', '그렇다', '4', 4, 4.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'EXTROVERSION_1'
UNION ALL
SELECT question_id, 'EXTROVERSION_1_OPT_5', '매우 그렇다', '5', 5, 5.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'EXTROVERSION_1';

-- -----------------------------------------------------
-- EXTROVERSION_2 — 정방향
-- -----------------------------------------------------
INSERT INTO survey_options (question_id, option_key, option_label, option_value, display_order, score_weight,
                             is_other_option, created_at, updated_at)
SELECT question_id, 'EXTROVERSION_2_OPT_1', '전혀 그렇지 않다', '1', 1, 1.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'EXTROVERSION_2'
UNION ALL
SELECT question_id, 'EXTROVERSION_2_OPT_2', '그렇지 않다', '2', 2, 2.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'EXTROVERSION_2'
UNION ALL
SELECT question_id, 'EXTROVERSION_2_OPT_3', '보통이다', '3', 3, 3.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'EXTROVERSION_2'
UNION ALL
SELECT question_id, 'EXTROVERSION_2_OPT_4', '그렇다', '4', 4, 4.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'EXTROVERSION_2'
UNION ALL
SELECT question_id, 'EXTROVERSION_2_OPT_5', '매우 그렇다', '5', 5, 5.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'EXTROVERSION_2';

-- -----------------------------------------------------
-- EXTROVERSION_3 — 역채점
-- -----------------------------------------------------
INSERT INTO survey_options (question_id, option_key, option_label, option_value, display_order, score_weight,
                             is_other_option, created_at, updated_at)
SELECT question_id, 'EXTROVERSION_3_OPT_1', '전혀 그렇지 않다', '1', 1, 5.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'EXTROVERSION_3'
UNION ALL
SELECT question_id, 'EXTROVERSION_3_OPT_2', '그렇지 않다', '2', 2, 4.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'EXTROVERSION_3'
UNION ALL
SELECT question_id, 'EXTROVERSION_3_OPT_3', '보통이다', '3', 3, 3.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'EXTROVERSION_3'
UNION ALL
SELECT question_id, 'EXTROVERSION_3_OPT_4', '그렇다', '4', 4, 2.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'EXTROVERSION_3'
UNION ALL
SELECT question_id, 'EXTROVERSION_3_OPT_5', '매우 그렇다', '5', 5, 1.00, false, NOW(), NOW()
FROM survey_questions WHERE question_key = 'EXTROVERSION_3';
