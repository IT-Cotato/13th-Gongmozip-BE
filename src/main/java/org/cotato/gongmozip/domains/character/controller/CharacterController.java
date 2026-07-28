package org.cotato.gongmozip.domains.character.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.character.dto.request.CharacterRequest.UpdatePaletteRequest;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.CurrentCharacterResponse;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.PaletteListResponse;
import org.cotato.gongmozip.domains.character.exception.codes.CharacterErrorCode;
import org.cotato.gongmozip.domains.character.exception.codes.CharacterSuccessCode;
import org.cotato.gongmozip.domains.character.service.CharacterService;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Character", description = "회원 캐릭터 조회 및 꾸미기 API")
@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @Operation(summary = "내 캐릭터 조회", description = "최근 협업 유형 검사 결과와 현재 선택한 팔레트로 캐릭터를 조회합니다.")
    @CustomErrorCodes(
            commonErrorCodes = GlobalErrorCode.class,
            domainErrorCodes = {CharacterErrorCode.class, MemberErrorCode.class})
    @GetMapping("/me")
    public ResponseEntity<BaseResponse<CurrentCharacterResponse>> getMyCharacter(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CurrentCharacterResponse response = characterService.getCurrentCharacter(userDetails.getMemberId());
        return BaseResponseFormatter.success(CharacterSuccessCode.CHARACTER_RETRIEVED, response);
    }

    @Operation(
            summary = "캐릭터 팔레트 목록 조회",
            description = "현재 캐릭터에 적용 가능한 단색·그라데이션 팔레트를 조회합니다. "
                    + "primaryHex는 DEFAULT 스타일일 때 null이며, "
                    + "secondaryHex는 SOLID 또는 DEFAULT 스타일일 때 null입니다.")
    @CustomErrorCodes(
            commonErrorCodes = GlobalErrorCode.class,
            domainErrorCodes = {CharacterErrorCode.class, MemberErrorCode.class})
    @GetMapping("/palettes")
    public ResponseEntity<BaseResponse<PaletteListResponse>> getPalettes(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PaletteListResponse response = characterService.getPalettes(userDetails.getMemberId());
        return BaseResponseFormatter.success(CharacterSuccessCode.PALETTES_RETRIEVED, response);
    }

    @Operation(summary = "내 캐릭터 팔레트 변경", description = "팔레트 코드를 변경합니다. 초기화는 paletteCode에 DEFAULT를 전달합니다.")
    @CustomErrorCodes(
            commonErrorCodes = GlobalErrorCode.class,
            domainErrorCodes = {CharacterErrorCode.class, MemberErrorCode.class})
    @PatchMapping("/me/palette")
    public ResponseEntity<BaseResponse<CurrentCharacterResponse>> updatePalette(
            @RequestBody @Valid UpdatePaletteRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        CurrentCharacterResponse response =
                characterService.updatePalette(userDetails.getMemberId(), request.paletteCode());
        return BaseResponseFormatter.success(CharacterSuccessCode.PALETTE_UPDATED, response);
    }
}
