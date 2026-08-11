package org.cotato.gongmozip.domains.team.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.enums.LeaderSelectionMode;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.domains.team.service.TeamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "test.api-key=test-secret-key")
class TeamTestControllerTest {

    private static final String VALID_BODY =
            """
            {
              "members": [
                { "memberId": 1, "profileId": 1, "leaderPreference": "WANTS", "extroversionType": "E", "extroversionScore": 4.2 },
                { "memberId": 2, "profileId": 2, "leaderPreference": "NEUTRAL", "extroversionType": "I", "extroversionScore": 2.1 },
                { "memberId": 3, "profileId": 3, "leaderPreference": "NEUTRAL", "extroversionType": "E", "extroversionScore": 3.8 }
              ],
              "preferredCategory": "PHOTO_VIDEO"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TeamService teamService;

    @Test
    @DisplayName("X-Test-Api-Key 헤더가 없으면 403이고 createTeam은 호출되지 않는다")
    void 헤더가_없으면_403이고_createTeam은_호출되지_않는다() throws Exception {
        mockMvc.perform(post("/api/test/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());

        verify(teamService, never()).createTeam(any());
    }

    @Test
    @DisplayName("X-Test-Api-Key가 틀리면 403이고 createTeam은 호출되지 않는다")
    void 헤더가_틀리면_403이고_createTeam은_호출되지_않는다() throws Exception {
        mockMvc.perform(post("/api/test/teams")
                        .header("X-Test-Api-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());

        verify(teamService, never()).createTeam(any());
    }

    @Test
    @DisplayName("X-Test-Api-Key가 일치하면 createTeam을 호출하고 팀 정보를 반환한다")
    void 헤더가_일치하면_createTeam을_호출하고_팀_정보를_반환한다() throws Exception {
        Team team = Team.builder()
                .teamId(5L)
                .status(TeamStatus.GREETING)
                .leaderSelectionMode(LeaderSelectionMode.AUTO_ASSIGNED)
                .build();
        given(teamService.createTeam(any())).willReturn(team);

        mockMvc.perform(post("/api/test/teams")
                        .header("X-Test-Api-Key", "test-secret-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").value(5))
                .andExpect(jsonPath("$.status").value("GREETING"))
                .andExpect(jsonPath("$.leaderSelectionMode").value("AUTO_ASSIGNED"));
    }

    @Test
    @DisplayName("팀원 목록이 비어있으면 400을 반환하고 createTeam은 호출되지 않는다")
    void 팀원_목록이_비어있으면_400을_반환한다() throws Exception {
        mockMvc.perform(
                        post("/api/test/teams")
                                .header("X-Test-Api-Key", "test-secret-key")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "members": [], "preferredCategory": "PHOTO_VIDEO" }
                                """))
                .andExpect(status().isBadRequest());

        verify(teamService, never()).createTeam(any());
    }
}
