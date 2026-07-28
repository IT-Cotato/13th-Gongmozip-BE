package org.cotato.gongmozip.domains.character.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.response.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CharacterSuccessCode implements BaseSuccessCode {

    // 200
    CHARACTER_RETRIEVED(HttpStatus.OK, "CHARACTER_200_1", "캐릭터를 성공적으로 조회했습니다."),
    PALETTES_RETRIEVED(HttpStatus.OK, "CHARACTER_200_2", "캐릭터 팔레트를 성공적으로 조회했습니다."),
    PALETTE_UPDATED(HttpStatus.OK, "CHARACTER_200_3", "캐릭터 팔레트를 성공적으로 변경했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
