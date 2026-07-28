package org.cotato.gongmozip.domains.character.converter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.CurrentCharacterResponse;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.PaletteListResponse;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.PaletteResponse;
import org.cotato.gongmozip.domains.character.entity.CharacterDefinition;
import org.cotato.gongmozip.domains.character.entity.CharacterDefinitionFeature;
import org.cotato.gongmozip.domains.character.entity.CharacterDefinitionTag;
import org.cotato.gongmozip.domains.character.entity.MemberCharacter;
import org.cotato.gongmozip.domains.character.enums.CharacterPalette;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.survey.entity.SurveySubmission;

public class CharacterConverter {

    private CharacterConverter() {}

    // 최초 설문 제출 회원의 기본 캐릭터 설정을 생성한다
    public static MemberCharacter toDefaultMemberCharacter(Member member) {
        return MemberCharacter.builder()
                .member(member)
                .palette(CharacterPalette.DEFAULT)
                .build();
    }

    // 설문 결과, 캐릭터 정의, 회원 설정을 캐릭터 상세 응답으로 변환한다
    public static CurrentCharacterResponse toCurrentCharacterResponse(
            SurveySubmission submission,
            MemberCharacter memberCharacter,
            CharacterDefinition definition,
            List<CharacterDefinitionTag> tags,
            List<CharacterDefinitionFeature> features) {
        return new CurrentCharacterResponse(
                submission.getCharacterType(),
                definition.getDisplayName(),
                memberCharacter.getPalette(),
                definition.getCatchphrase(),
                tags.stream().map(CharacterDefinitionTag::getTag).toList(),
                features.stream().map(CharacterDefinitionFeature::getContent).toList(),
                submission.getSubmittedAt(),
                memberCharacter.getUpdatedAt());
    }

    // 선택 가능한 전체 팔레트를 표시 순서와 현재 선택 여부를 포함한 응답으로 변환한다
    public static PaletteListResponse toPaletteListResponse(CharacterPalette selectedPalette) {
        List<PaletteResponse> palettes = Arrays.stream(CharacterPalette.values())
                .sorted(Comparator.comparingInt(CharacterPalette::getDisplayOrder))
                .map(palette -> toPaletteResponse(palette, selectedPalette))
                .toList();
        return new PaletteListResponse(CharacterPalette.DEFAULT, palettes);
    }

    private static PaletteResponse toPaletteResponse(CharacterPalette palette, CharacterPalette selectedPalette) {
        return new PaletteResponse(
                palette,
                palette.getDisplayName(),
                palette.getStyle(),
                palette.getPrimaryHex(),
                palette.getSecondaryHex(),
                palette.getDisplayOrder(),
                palette == selectedPalette);
    }
}
