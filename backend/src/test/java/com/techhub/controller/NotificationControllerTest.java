package com.techhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.common.PageResult;
import com.techhub.dto.notification.NotificationVO;
import com.techhub.security.JwtTokenProvider;
import com.techhub.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("NotificationController 单元测试")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    private NotificationVO mockNotification;

    @BeforeEach
    void setUp() {
        mockNotification = new NotificationVO();
        mockNotification.setId("1");
        mockNotification.setType("COMMENT");
        mockNotification.setSourceId("100");
        mockNotification.setContent("有人评论了你的帖子");
        mockNotification.setIsRead(false);
        mockNotification.setCreateTime(LocalDateTime.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );
    }

    // ==================== GET /api/v1/notifications ====================

    @Test
    @DisplayName("listNotifications — 返回当前用户通知分页列表")
    void listNotifications_ReturnsPaginatedList() throws Exception {
        authenticateUser();
        PageResult<NotificationVO> pageResult = PageResult.of(List.of(mockNotification), 1, 20, 1);

        when(notificationService.listNotifications(eq(1), eq(20), eq(1L)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/notifications")
                        .param("page", "1")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value("1"))
                .andExpect(jsonPath("$.data.records[0].type").value("COMMENT"))
                .andExpect(jsonPath("$.data.records[0].isRead").value(false))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.current").value(1));
    }

    @Test
    @DisplayName("listNotifications — 未认证返回 401")
    void listNotifications_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    // ==================== PATCH /api/v1/notifications/{id}/read ====================

    @Test
    @DisplayName("markAsRead — 标记已读成功返回 200")
    void markAsRead_Success() throws Exception {
        authenticateUser();

        doNothing().when(notificationService).markAsRead(eq(1L), eq(1L));

        mockMvc.perform(patch("/api/v1/notifications/1/read")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已读"));
    }

    // ==================== PATCH /api/v1/notifications/read-all ====================

    @Test
    @DisplayName("markAllAsRead — 全部已读成功返回 200")
    void markAllAsRead_Success() throws Exception {
        authenticateUser();

        doNothing().when(notificationService).markAllAsRead(eq(1L));

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("全部已读"));
    }

    // ==================== GET /api/v1/notifications/unread-count ====================

    @Test
    @DisplayName("getUnreadCount — 返回未读通知数")
    void getUnreadCount_ReturnsCount() throws Exception {
        authenticateUser();

        when(notificationService.getUnreadCount(eq(1L))).thenReturn(5);

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.count").value(5));
    }
}
