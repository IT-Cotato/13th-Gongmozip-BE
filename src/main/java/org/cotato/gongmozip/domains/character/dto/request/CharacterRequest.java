package org.cotato.gongmozip.domains.character.dto.request;

import jakarta.validation.constraints.NotNull;
import org.cotato.gongmozip.domains.character.enums.CharacterPalette;

public final class CharacterRequest {

    private CharacterRequest() {}

    // 캐릭터 팔레트 변경 요청
    public record UpdatePaletteRequest(@NotNull(message = "변경할 캐릭터 팔레트는 필수입니다.") CharacterPalette paletteCode) {}
}
