package com.techhub.controller;

import com.techhub.security.JwtTokenProvider;
import com.techhub.service.InteractionService;
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

import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("InteractionController 单元测试")
class InteractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InteractionService interactionService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    void setUp() {
        // 默认认证为用户
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== POST /api/v1/posts/{id}/likes ====================

    @Test
    @DisplayName("likePost — 点赞帖子成功返回 200")
    void likePost_Success_Returns200() throws Exception {
        doNothing().when(interactionService).likePost(1L);

        mockMvc.perform(post("/api/v1/posts/1/likes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("点赞成功"));
    }

    // ==================== DELETE /api/v1/posts/{id}/likes ====================

    @Test
    @DisplayName("unlikePost — 取消点赞帖子返回 200")
    void unlikePost_Success_Returns200() throws Exception {
        doNothing().when(interactionService).unlikePost(1L);

        mockMvc.perform(delete("/api/v1/posts/1/likes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已取消"));
    }

    // ==================== POST /api/v1/comments/{id}/likes ====================

    @Test
    @DisplayName("likeComment — 点赞评论成功返回 200")
    void likeComment_Success_Returns200() throws Exception {
        doNothing().when(interactionService).likeComment(1L);

        mockMvc.perform(post("/api/v1/comments/1/likes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("点赞成功"));
    }

    // ==================== DELETE /api/v1/comments/{id}/likes ====================

    @Test
    @DisplayName("unlikeComment — 取消点赞评论返回 200")
    void unlikeComment_Success_Returns200() throws Exception {
        doNothing().when(interactionService).unlikeComment(1L);

        mockMvc.perform(delete("/api/v1/comments/1/likes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已取消"));
    }
}
