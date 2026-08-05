package org.cotato.gongmozip.domains.collaboration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.collaboration.dto.response.CollaborationResponse.CollaborationDistanceResponse;
import org.cotato.gongmozip.domains.collaboration.dto.response.CollaborationResponse.CollaborationHistoryListResponse;
import org.cotato.gongmozip.domains.collaboration.dto.response.CollaborationResponse.CollaborationHistoryResponse;
import org.cotato.gongmozip.domains.collaboration.service.CollaborationPointService;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.enums.MemberRole;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CollaborationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CollaborationPointService collaborationPointService;

    private Member member() {
        return Member.builder()
                .memberId(1L)
                .email("test@gongmozip.com")
                .role(MemberRole.USER)
                .build();
    }

    @Test
    @DisplayName("로그인한 회원은 자신의 협업거리 게이지를 조회할 수 있다")
    void getDistanceReturnsPointAndGauge() throws Exception {
        Member member = member();
        CollaborationDistanceResponse response = new CollaborationDistanceResponse(150, 500, 30.0);
        given(collaborationPointService.getCollaborationDistance(1L)).willReturn(response);

        mockMvc.perform(get("/api/members/me/collaboration-distance").with(user(new CustomUserDetails(member))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COLLABORATION_200_1"))
                .andExpect(jsonPath("$.data.collaborationPoint").value(150))
                .andExpect(jsonPath("$.data.maxCollaborationPoint").value(500))
                .andExpect(jsonPath("$.data.gaugePercent").value(30.0));
    }

    @Test
    @DisplayName("로그인한 회원은 자신의 협업거리 히스토리를 조회할 수 있다")
    void getHistoriesReturnsList() throws Exception {
        Member member = member();
        CollaborationHistoryResponse history = new CollaborationHistoryResponse(1L, 10, "프로젝트 완주", LocalDateTime.now());
        CollaborationHistoryListResponse response =
                new CollaborationHistoryListResponse(List.of(history), 0, 10, 1L, 1, false);
        given(collaborationPointService.getCollaborationHistories(
                        eq(1L), any(org.springframework.data.domain.Pageable.class)))
                .willReturn(response);

        mockMvc.perform(get("/api/members/me/collaboration-distance/histories")
                        .with(user(new CustomUserDetails(member))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COLLABORATION_200_2"))
                .andExpect(jsonPath("$.data.histories[0].delta").value(10))
                .andExpect(jsonPath("$.data.histories[0].reason").value("프로젝트 완주"));
    }
}
