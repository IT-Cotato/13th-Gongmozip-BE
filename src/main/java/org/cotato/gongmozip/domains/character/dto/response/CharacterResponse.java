package org.cotato.gongmozip.domains.character.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.character.enums.CharacterPalette;
import org.cotato.gongmozip.domains.character.enums.PaletteStyle;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;

public final class CharacterResponse {

    private CharacterResponse() {}

    // 현재 회원의 캐릭터 상세 조회 응답
    public record CurrentCharacterResponse(
            CharacterType characterType,
            String displayName,
            CharacterPalette paletteCode,
            String catchphrase,
            List<String> hashtags,
            List<String> features,
            LocalDateTime submittedAt,
            LocalDateTime paletteUpdatedAt) {}

    // 팔레트 목록 조회 응답 (기본 팔레트 + 선택 가능한 전체 팔레트)
    public record PaletteListResponse(CharacterPalette defaultPaletteCode, List<PaletteResponse> palettes) {}

    // 개별 팔레트의 렌더링 정보와 현재 선택 여부
    public record PaletteResponse(
            CharacterPalette paletteCode,
            String displayName,
            PaletteStyle style,
            String primaryHex,
            String secondaryHex,
            int displayOrder,
            boolean selected) {}

    // 다른 도메인(팀 채팅 등)이 여러 회원의 아바타를 한 번에 렌더링할 때 쓰는 경량 응답.
    // 캐릭터 이름/캐치프레이즈/태그 등 상세 설명은 필요 없어 CurrentCharacterResponse보다 가볍다.
    public record MemberAvatarResponse(
            Long memberId,
            CharacterType characterType,
            CharacterPalette paletteCode,
            String primaryHex,
            String secondaryHex) {}
}
