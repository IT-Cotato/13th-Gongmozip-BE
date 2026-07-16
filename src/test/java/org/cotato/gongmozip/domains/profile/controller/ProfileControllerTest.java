package org.cotato.gongmozip.domains.profile.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.cotato.gongmozip.domains.profile.dto.response.ProfileResponse.PublicProfileResponse;
import org.cotato.gongmozip.domains.profile.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @Test
    void 익명_사용자가_공개_프로필을_조회할_수_있다() throws Exception {
        given(profileService.getPublicProfile(1L))
                .willReturn(new PublicProfileResponse(
                        1L, "러너", null, "서울 소재 대학교", "학교", 3, "컴퓨터공학", null, List.of(), List.of(), List.of()));

        mockMvc.perform(get("/api/public/profiles/{profileId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileId").value(1L))
                .andExpect(jsonPath("$.data.nickname").value("러너"));
    }
}
