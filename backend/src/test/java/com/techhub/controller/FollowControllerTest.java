package com.techhub.controller;

import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.ResultCode;
import com.techhub.dto.user.FollowStatusVO;
import com.techhub.dto.user.UserProfileVO;
import com.techhub.security.JwtTokenProvider;
import com.techhub.service.FollowService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("FollowController 单元测试")
class FollowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FollowService followService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== POST /api/v1/users/{id}/follow ====================

    @Test
    @DisplayName("follow — 关注成功返回 200")
    void followSuccess() throws Exception {
        doNothing().when(followService).follow(eq(2L));

        mockMvc.perform(post("/api/v1/users/2/follow")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("关注成功"));
    }

    @Test
    @DisplayName("follow — 关注自己返回 400")
    void followSelf() throws Exception {
        doThrow(new BusinessException(ResultCode.BAD_REQUEST, "不能关注自己"))
                .when(followService).follow(eq(1L));

        mockMvc.perform(post("/api/v1/users/1/follow")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("follow — 重复关注返回 409")
    void followDuplicate() throws Exception {
        doThrow(new BusinessException(ResultCode.CONFLICT, "已关注该用户"))
                .when(followService).follow(eq(2L));

        mockMvc.perform(post("/api/v1/users/2/follow")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    // ==================== DELETE /api/v1/users/{id}/follow ====================

    @Test
    @DisplayName("unfollow — 取消关注成功返回 200")
    void unfollowSuccess() throws Exception {
        doNothing().when(followService).unfollow(eq(2L));

        mockMvc.perform(delete("/api/v1/users/2/follow")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("取消关注成功"));
    }

    @Test
    @DisplayName("unfollow — 未关注时取消也返回 200（幂等）")
    void unfollowIdempotent() throws Exception {
        doNothing().when(followService).unfollow(eq(99L));

        mockMvc.perform(delete("/api/v1/users/99/follow")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== GET /api/v1/users/{id}/follow ====================

    @Test
    @DisplayName("getFollowStatus — 已关注返回 true")
    void getFollowStatusFollowing() throws Exception {
        when(followService.getFollowStatus(eq(2L)))
                .thenReturn(new FollowStatusVO(true));

        mockMvc.perform(get("/api/v1/users/2/follow")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.following").value(true));
    }

    @Test
    @DisplayName("getFollowStatus — 未关注返回 false")
    void getFollowStatusNotFollowing() throws Exception {
        when(followService.getFollowStatus(eq(3L)))
                .thenReturn(new FollowStatusVO(false));

        mockMvc.perform(get("/api/v1/users/3/follow")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.following").value(false));
    }

    // ==================== GET /api/v1/users/{id}/followers ====================

    @Test
    @DisplayName("getFollowers — 返回粉丝列表")
    void getFollowers() throws Exception {
        UserProfileVO vo = new UserProfileVO();
        vo.setId("2");
        vo.setUsername("follower1");
        vo.setAvatarUrl("https://example.com/avatar.png");
        vo.setBio("bio");
        vo.setRole("USER");
        vo.setStatus(1);
        when(followService.getFollowers(1L, 1, 20))
                .thenReturn(new PageResult<>(List.of(vo), 1, 20, 1));

        mockMvc.perform(get("/api/v1/users/1/followers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].username").value("follower1"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.current").value(1));
    }

    @Test
    @DisplayName("getFollowers — 空粉丝列表")
    void getFollowersEmpty() throws Exception {
        when(followService.getFollowers(1L, 1, 20))
                .thenReturn(new PageResult<>(List.of(), 0, 20, 1));

        mockMvc.perform(get("/api/v1/users/1/followers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isEmpty())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    // ==================== GET /api/v1/users/{id}/followings ====================

    @Test
    @DisplayName("getFollowings — 返回关注列表")
    void getFollowings() throws Exception {
        UserProfileVO vo = new UserProfileVO();
        vo.setId("3");
        vo.setUsername("followee1");
        vo.setRole("USER");
        vo.setStatus(1);
        when(followService.getFollowings(1L, 1, 20))
                .thenReturn(new PageResult<>(List.of(vo), 1, 20, 1));

        mockMvc.perform(get("/api/v1/users/1/followings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].username").value("followee1"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.current").value(1));
    }

    @Test
    @DisplayName("getFollowings — 空关注列表")
    void getFollowingsEmpty() throws Exception {
        when(followService.getFollowings(99L, 1, 20))
                .thenReturn(new PageResult<>(List.of(), 0, 20, 1));

        mockMvc.perform(get("/api/v1/users/99/followings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isEmpty())
                .andExpect(jsonPath("$.data.total").value(0));
    }
}
