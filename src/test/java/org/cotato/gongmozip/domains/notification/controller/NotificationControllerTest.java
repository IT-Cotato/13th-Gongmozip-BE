package org.cotato.gongmozip.domains.notification.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.notification.dto.response.NotificationResponse.NotificationListResponse;
import org.cotato.gongmozip.domains.notification.enums.NotificationCategory;
import org.cotato.gongmozip.domains.notification.service.NotificationService;
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
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    private final Member member =
            Member.builder().memberId(1L).email("member@example.com").build();
    private final CustomUserDetails userDetails = new CustomUserDetails(member);

    @DisplayName("category를 생략하면 전체 알림을 조회한다.")
    @Test
    void getNotificationsWithoutCategory() throws Exception {
        given(notificationService.getNotifications(1L, null, null))
                .willReturn(new NotificationListResponse(java.util.List.of(), false));

        mockMvc.perform(get("/api/notifications").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_200_1"));
    }

    @DisplayName("올바른 category 값은 해당 카테고리로 변환되어 조회된다.")
    @Test
    void getNotificationsWithValidCategory() throws Exception {
        given(notificationService.getNotifications(1L, NotificationCategory.MATCHING, null))
                .willReturn(new NotificationListResponse(java.util.List.of(), false));

        mockMvc.perform(get("/api/notifications?category=MATCHING").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_200_1"));
    }

    @DisplayName("잘못된 category 값은 400과 전용 에러코드로 응답한다 (500이 아니다).")
    @Test
    void getNotificationsWithInvalidCategoryReturns400() throws Exception {
        mockMvc.perform(get("/api/notifications?category=FOO").with(user(userDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_400_1"));
    }

    @DisplayName("비인증 사용자는 알림 목록을 조회할 수 없다.")
    @Test
    void getNotificationsRejectsUnauthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
    }
}
