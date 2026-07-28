package org.cotato.gongmozip.domains.character.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.CurrentCharacterResponse;
import org.cotato.gongmozip.domains.character.enums.CharacterPalette;
import org.cotato.gongmozip.domains.character.service.CharacterService;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.enums.MemberRole;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CharacterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CharacterService characterService;

    @Test
    @DisplayName("로그인한 회원은 자신의 캐릭터와 DB 설명을 조회할 수 있다")
    void getMyCharacterReturnsCharacter() throws Exception {
        Member member = member();
        given(characterService.getCurrentCharacter(1L)).willReturn(response(CharacterPalette.DEFAULT));

        mockMvc.perform(get("/api/characters/me").with(user(new CustomUserDetails(member))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CHARACTER_200_1"))
                .andExpect(jsonPath("$.data.characterType").value("TRACK_RUNNER"))
                .andExpect(jsonPath("$.data.displayName").value("트랙러너"))
                .andExpect(jsonPath("$.data.paletteCode").value("DEFAULT"))
                .andExpect(jsonPath("$.data.imageUrl").doesNotExist())
                .andExpect(jsonPath("$.data.hashtags[0]").value("꼼꼼함"));
    }

    @Test
    @DisplayName("팔레트 변경 요청은 선택한 팔레트가 적용된 캐릭터를 반환한다")
    void updatePaletteReturnsUpdatedCharacter() throws Exception {
        Member member = member();
        given(characterService.updatePalette(1L, CharacterPalette.GRADIENT_OCEAN))
                .willReturn(response(CharacterPalette.GRADIENT_OCEAN));

        mockMvc.perform(patch("/api/characters/me/palette")
                        .with(user(new CustomUserDetails(member)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paletteCode\":\"GRADIENT_OCEAN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CHARACTER_200_3"))
                .andExpect(jsonPath("$.data.paletteCode").value("GRADIENT_OCEAN"));
    }

    private Member member() {
        return Member.builder()
                .memberId(1L)
                .email("character@gongmozip.com")
                .role(MemberRole.USER)
                .build();
    }

    private CurrentCharacterResponse response(CharacterPalette palette) {
        return new CurrentCharacterResponse(
                CharacterType.TRACK_RUNNER,
                "트랙러너",
                palette,
                "조용히 달려도 결국 완주하는 건 나야!",
                List.of("꼼꼼함", "책임감"),
                List.of("말보다 결과물로 보여주는 꾸준한 러너"),
                LocalDateTime.of(2026, 5, 19, 15, 0),
                LocalDateTime.of(2026, 7, 27, 10, 0));
    }
}
