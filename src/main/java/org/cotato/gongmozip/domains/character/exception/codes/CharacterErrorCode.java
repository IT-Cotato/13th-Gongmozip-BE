package org.cotato.gongmozip.domains.character.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CharacterErrorCode implements BaseErrorCode {

    // 404
    CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHARACTER_404_1", "협업 유형 검사를 완료한 후 캐릭터를 이용할 수 있습니다."),

    // 500
    CHARACTER_DEFINITION_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "CHARACTER_500_1", "캐릭터 유형에 해당하는 설명 정보가 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
