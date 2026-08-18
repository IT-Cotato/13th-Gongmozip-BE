-- 중간점검 알림이 카드(PROGRESS_CHECK_CARD)에서 일반 텍스트 메시지로 바뀌면서
-- MessageType.PROGRESS_CHECK_CARD enum 값 자체를 코드에서 삭제했다.
-- messages.message_type은 @Enumerated(EnumType.STRING)이라, 과거에 이미
-- PROGRESS_CHECK_CARD로 저장된 row가 있으면 조회 시 역직렬화 예외가 난다.
-- 그런 row를 TEXT로 백필해 안전하게 만든다(없으면 0 rows, 무해).
UPDATE messages
SET message_type = 'TEXT'
WHERE message_type = 'PROGRESS_CHECK_CARD';
