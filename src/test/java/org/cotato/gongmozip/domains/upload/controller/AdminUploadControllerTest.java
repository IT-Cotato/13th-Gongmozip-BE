package org.cotato.gongmozip.domains.upload.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.enums.MemberRole;
import org.cotato.gongmozip.domains.upload.dto.request.UploadRequest.GetPresignedUrlRequest;
import org.cotato.gongmozip.domains.upload.dto.response.GetPresignedUrlResponse;
import org.cotato.gongmozip.domains.upload.service.S3Service;
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
class AdminUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private S3Service s3Service;

    @DisplayName("관리자 권한을 가진 사용자는 Presigned URL을 발급받을 수 있다.")
    @Test
    void 관리자는_Presigned_URL을_발급받을_수_있다() throws Exception {
        // given
        Member admin = Member.builder()
                .memberId(1L)
                .email("admin@gongmozip.com")
                .role(MemberRole.ADMIN)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(admin);

        GetPresignedUrlRequest request = new GetPresignedUrlRequest("poster.png", "image/png");
        GetPresignedUrlResponse response =
                new GetPresignedUrlResponse("http://presigned-url", "http://image-url", "image/png");

        given(s3Service.getPresignedUrlForUpload("poster.png", "image/png")).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/admin/uploads/presigned-url")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uploadUrl").value("http://presigned-url"))
                .andExpect(jsonPath("$.data.imageUrl").value("http://image-url"))
                .andExpect(jsonPath("$.data.contentType").value("image/png"));

        verify(s3Service).getPresignedUrlForUpload("poster.png", "image/png");
    }

    @DisplayName("일반 권한을 가진 사용자가 Presigned URL 발급 요청 시 403 Forbidden 에러가 발생한다.")
    @Test
    void 일반유저는_Presigned_URL을_발급받을_수_없다() throws Exception {
        // given
        Member user = Member.builder()
                .memberId(2L)
                .email("user@gongmozip.com")
                .role(MemberRole.USER)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        GetPresignedUrlRequest request = new GetPresignedUrlRequest("poster.png", "image/png");

        // when & then
        mockMvc.perform(post("/api/admin/uploads/presigned-url")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @DisplayName("비로그인 사용자가 Presigned URL 발급 요청 시 401 Unauthorized 에러가 발생한다.")
    @Test
    void 비로그인_사용자는_Presigned_URL을_발급받을_수_없다() throws Exception {
        // given
        GetPresignedUrlRequest request = new GetPresignedUrlRequest("poster.png", "image/png");

        // when & then
        mockMvc.perform(post("/api/admin/uploads/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
