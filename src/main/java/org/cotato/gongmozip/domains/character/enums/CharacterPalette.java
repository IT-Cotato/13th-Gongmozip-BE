package org.cotato.gongmozip.domains.character.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CharacterPalette {
    // DEFAULT는 별도 색상 합성 없이 캐릭터 원본 색상을 사용한다.
    DEFAULT("기본", PaletteStyle.DEFAULT, null, null, 0),
    SOLID_PINK("핑크", PaletteStyle.SOLID, "#FFE9E7", null, 1),
    SOLID_MINT("민트", PaletteStyle.SOLID, "#E9FAEF", null, 2),
    SOLID_SKY("스카이", PaletteStyle.SOLID, "#E8F5FF", null, 3),
    SOLID_LEMON("레몬", PaletteStyle.SOLID, "#FAFDE8", null, 4),
    SOLID_SAND("샌드", PaletteStyle.SOLID, "#F3F2EA", null, 5),
    SOLID_LAVENDER("라벤더", PaletteStyle.SOLID, "#F1E9EF", null, 6),
    GRADIENT_SUNSET("선셋", PaletteStyle.GRADIENT, "#FF684F", "#FFA45B", 7),
    GRADIENT_OCEAN("오션", PaletteStyle.GRADIENT, "#73C4FF", "#4D9AE8", 8),
    GRADIENT_FOREST("포레스트", PaletteStyle.GRADIENT, "#31E47A", "#39DFA2", 9);

    // 팔레트 선택 화면에 표시할 한글 이름
    private final String displayName;

    // 원본·단색·그라데이션을 구분하는 렌더링 방식
    private final PaletteStyle style;

    // 단색 또는 그라데이션 시작 색상
    private final String primaryHex;

    // 그라데이션 종료 색상 — 원본과 단색 팔레트는 null
    private final String secondaryHex;

    // 팔레트 선택 화면의 표시 순서
    private final int displayOrder;
}
