-- ============================================================
-- V9: 캐릭터 카탈로그 및 회원별 팔레트 설정 추가
--
-- 목적
--   1. 설문 결과로 결정된 캐릭터 유형의 공통 설명을 별도 카탈로그로 관리한다.
--   2. 회원별로 달라지는 팔레트 설정만 member_characters에 저장한다.
--   3. 마이그레이션 시점에 이미 설문을 완료한 회원의 기본 설정을 보정한다.
--
-- 관계
--   character_definitions 1 : N character_definition_tags
--   character_definitions 1 : N character_definition_features
--   member                1 : 1 member_characters
--
-- 캐릭터 유형의 원본 값은 survey_submissions.character_type이다.
-- 따라서 member_characters는 캐릭터 유형을 중복 저장하지 않는다.
-- ============================================================

-- 1. 캐릭터 유형별 이름과 한 줄 소개를 관리하는 고정 카탈로그
-- character_type은 CharacterType enum의 문자열이며, 유형별 정의가 하나만 존재하도록 UNIQUE로 제한한다.
CREATE TABLE character_definitions
(
    character_definition_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    character_type          VARCHAR(50)  NOT NULL,
    display_name            VARCHAR(50)  NOT NULL,
    catchphrase             VARCHAR(255) NOT NULL,
    created_at              TIMESTAMP    NOT NULL,
    updated_at              TIMESTAMP    NOT NULL,
    CONSTRAINT uq_character_definitions_type UNIQUE (character_type)
);

-- 2. 캐릭터 유형별 결과 화면 해시태그
-- display_order는 캐릭터 정의 안에서만 유일하며, API 응답의 해시태그 표시 순서로 사용한다.
CREATE TABLE character_definition_tags
(
    character_definition_tag_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    character_definition_id     BIGINT       NOT NULL,
    tag                         VARCHAR(50)  NOT NULL,
    display_order               INT          NOT NULL,
    created_at                  TIMESTAMP    NOT NULL,
    updated_at                  TIMESTAMP    NOT NULL,
    CONSTRAINT fk_character_tags_definition
        FOREIGN KEY (character_definition_id) REFERENCES character_definitions (character_definition_id),
    CONSTRAINT uq_character_tags_order UNIQUE (character_definition_id, display_order)
);

-- 3. 캐릭터 유형별 결과 화면 상세 특징
-- display_order는 캐릭터 정의 안에서만 유일하며, API 응답의 특징 표시 순서로 사용한다.
CREATE TABLE character_definition_features
(
    character_definition_feature_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    character_definition_id         BIGINT      NOT NULL,
    content                         VARCHAR(500) NOT NULL,
    display_order                   INT         NOT NULL,
    created_at                      TIMESTAMP   NOT NULL,
    updated_at                      TIMESTAMP   NOT NULL,
    CONSTRAINT fk_character_features_definition
        FOREIGN KEY (character_definition_id) REFERENCES character_definitions (character_definition_id),
    CONSTRAINT uq_character_features_order UNIQUE (character_definition_id, display_order)
);

-- 4. 회원별 캐릭터 커스터마이징 설정
-- 캐릭터 유형은 survey_submissions에서 조회하고 여기에는 회원이 선택한 팔레트만 저장한다.
-- member_id의 UNIQUE 제약으로 회원마다 설정 행을 하나만 가질 수 있다.
CREATE TABLE member_characters
(
    member_character_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id           BIGINT      NOT NULL,
    palette_code        VARCHAR(40) NOT NULL,
    created_at          TIMESTAMP   NOT NULL,
    updated_at          TIMESTAMP   NOT NULL,
    CONSTRAINT fk_member_characters_member
        FOREIGN KEY (member_id) REFERENCES member (member_id),
    CONSTRAINT uq_member_characters_member UNIQUE (member_id)
);

-- 5. 협업 유형 검사에서 사용하는 네 가지 캐릭터 기본 설명
-- created_at과 updated_at은 BaseEntity의 NOT NULL 매핑을 만족하도록 함께 입력한다.
INSERT INTO character_definitions
    (character_type, display_name, catchphrase, created_at, updated_at)
VALUES
    ('LEAD_RUNNER', '리드러너', '방향을 정하고 함께 완주하는 리더', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TRACK_RUNNER', '트랙러너', '조용히 달려도 결국 완주하는 건 나야!', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('BOOST_RUNNER', '부스트러너', '팀에 활력을 더하고 함께 속도를 높여요!', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('FREE_RUNNER', '프리러너', '정해진 길보다 나만의 방식으로 답을 찾아요!', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 6. 캐릭터별 결과 화면 해시태그
-- 자동 생성된 PK를 직접 가정하지 않고 character_type으로 정의 ID를 조회하여 연결한다.
-- UNION ALL을 사용해 모든 캐릭터의 해시태그를 한 번의 INSERT로 저장한다.
INSERT INTO character_definition_tags
    (character_definition_id, tag, display_order, created_at, updated_at)
-- LEAD_RUNNER
SELECT character_definition_id, '주도성', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'LEAD_RUNNER'
UNION ALL
SELECT character_definition_id, '계획성', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'LEAD_RUNNER'
UNION ALL
SELECT character_definition_id, '조율력', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'LEAD_RUNNER'
UNION ALL
SELECT character_definition_id, '추진력', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'LEAD_RUNNER'
UNION ALL
-- TRACK_RUNNER
SELECT character_definition_id, '꼼꼼함', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'TRACK_RUNNER'
UNION ALL
SELECT character_definition_id, '책임감', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'TRACK_RUNNER'
UNION ALL
SELECT character_definition_id, '집중력', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'TRACK_RUNNER'
UNION ALL
SELECT character_definition_id, '안정성', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'TRACK_RUNNER'
UNION ALL
-- BOOST_RUNNER
SELECT character_definition_id, '활력', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'BOOST_RUNNER'
UNION ALL
SELECT character_definition_id, '친화력', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'BOOST_RUNNER'
UNION ALL
SELECT character_definition_id, '실행력', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'BOOST_RUNNER'
UNION ALL
SELECT character_definition_id, '긍정성', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'BOOST_RUNNER'
UNION ALL
-- FREE_RUNNER
SELECT character_definition_id, '유연함', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'FREE_RUNNER'
UNION ALL
SELECT character_definition_id, '탐색력', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'FREE_RUNNER'
UNION ALL
SELECT character_definition_id, '독립성', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'FREE_RUNNER'
UNION ALL
SELECT character_definition_id, '창의성', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'FREE_RUNNER';

-- 7. 캐릭터별 결과 화면 상세 특징
-- 해시태그와 마찬가지로 character_type으로 정의 ID를 조회하여 연결한다.
INSERT INTO character_definition_features
    (character_definition_id, content, display_order, created_at, updated_at)
-- LEAD_RUNNER
SELECT character_definition_id, '팀의 목표와 방향을 빠르게 정리하는 러너', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'LEAD_RUNNER'
UNION ALL
SELECT character_definition_id, '구성원의 의견을 조율하며 실행을 이끄는 존재', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'LEAD_RUNNER'
UNION ALL
SELECT character_definition_id, '계획을 세우고 끝까지 완주하는 데 강함', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'LEAD_RUNNER'
UNION ALL
-- TRACK_RUNNER
SELECT character_definition_id, '말보다 결과물로 보여주는 꾸준한 러너', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'TRACK_RUNNER'
UNION ALL
SELECT character_definition_id, '눈에 띄진 않지만 팀의 완성도를 책임짐', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'TRACK_RUNNER'
UNION ALL
SELECT character_definition_id, '맡은 일은 끝까지 해내는 신뢰형 플레이어', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'TRACK_RUNNER'
UNION ALL
-- BOOST_RUNNER
SELECT character_definition_id, '밝은 에너지로 팀의 분위기를 끌어올리는 러너', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'BOOST_RUNNER'
UNION ALL
SELECT character_definition_id, '빠르게 소통하고 동료의 참여를 자연스럽게 이끔', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'BOOST_RUNNER'
UNION ALL
SELECT character_definition_id, '아이디어를 행동으로 옮기며 팀에 추진력을 더함', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'BOOST_RUNNER'
UNION ALL
-- FREE_RUNNER
SELECT character_definition_id, '상황에 맞게 방향을 바꾸며 답을 찾아가는 러너', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'FREE_RUNNER'
UNION ALL
SELECT character_definition_id, '혼자 깊게 탐색하고 새로운 관점을 발견하는 데 강함', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'FREE_RUNNER'
UNION ALL
SELECT character_definition_id, '정해진 방식보다 유연하고 창의적인 접근을 선호함', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_definitions WHERE character_type = 'FREE_RUNNER';

-- 8. 기존 설문 완료 회원의 캐릭터 설정 보정
-- SUBMITTED 상태인 회원만 대상으로 하며, NOT EXISTS로 이미 설정이 있는 회원은 제외한다.
-- DEFAULT 팔레트를 사용해 기존 데이터에도 신규 기능을 즉시 적용한다.
INSERT INTO member_characters (member_id, palette_code, created_at, updated_at)
SELECT ss.member_id, 'DEFAULT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM survey_submissions ss
WHERE ss.status = 'SUBMITTED'
  AND NOT EXISTS (
      SELECT 1
      FROM member_characters mc
      WHERE mc.member_id = ss.member_id
  );
