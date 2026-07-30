package org.cotato.gongmozip.domains.profile.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.profile.dto.request.ProfileRequest.ProjectEvaluationRequest;
import org.cotato.gongmozip.domains.profile.dto.response.ProfileResponse.ProjectEvaluationResponse;
import org.cotato.gongmozip.domains.profile.service.ProjectEvaluationService;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProjectEvaluationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private ProjectEvaluationService projectEvaluationService;

    @Test
    void 프로젝트_AI_평가_요청_시_202_ACCEPTED_를_반환한다() throws Exception {
        Member member =
                Member.builder().memberId(1L).email("user@gongmozip.com").build();
        CustomUserDetails userDetails = new CustomUserDetails(member);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        ProjectEvaluationRequest request = new ProjectEvaluationRequest(10L);
        doNothing().when(projectEvaluationService).evaluateProject(10L, member);

        mockMvc.perform(post("/api/ai/project-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value("PROFILE_202_2"));
    }

    @Test
    void 프로젝트_AI_평가_조회_시_200_OK_및_평가결과를_반환한다() throws Exception {
        Member member =
                Member.builder().memberId(1L).email("user@gongmozip.com").build();
        CustomUserDetails userDetails = new CustomUserDetails(member);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        ProjectEvaluationResponse response = new ProjectEvaluationResponse(5L, "COMPLETED", 85, "우수한 성과로 평가되었습니다.");
        given(projectEvaluationService.getEvaluation(5L, member)).willReturn(response);

        mockMvc.perform(get("/api/ai/project-evaluations/{evaluationId}", 5L).with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PROFILE_200_24"))
                .andExpect(jsonPath("$.data.evaluationId").value(5L))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.score").value(85))
                .andExpect(jsonPath("$.data.feedback").value("우수한 성과로 평가되었습니다."));
    }
}
