package com.techhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.ResultCode;
import com.techhub.dto.user.UserProfileVO;
import com.techhub.security.JwtTokenProvider;
import com.techhub.service.AdminUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AdminUserController 单元测试")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    // ==================== listUsers Tests ====================

    @Test
    @DisplayName("listUsers — 管理员分页查询用户列表")
    @WithMockUser(roles = "ADMIN")
    void listUsers_AsAdmin_Success() throws Exception {
        UserProfileVO user = new UserProfileVO();
        user.setId("1");
        user.setUsername("testuser");
        user.setRole("user");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());

        PageResult<UserProfileVO> pageResult = PageResult.of(List.of(user), 1, 20, 1);

        when(adminUserService.listUsers(anyInt(), anyInt(), eq(null)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value("1"))
                .andExpect(jsonPath("$.data.records[0].username").value("testuser"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("listUsers — 关键词搜索")
    @WithMockUser(roles = "ADMIN")
    void listUsers_WithKeyword_Success() throws Exception {
        UserProfileVO user = new UserProfileVO();
        user.setId("2");
        user.setUsername("admin");
        user.setRole("admin");
        user.setStatus(1);

        PageResult<UserProfileVO> pageResult = PageResult.of(List.of(user), 1, 20, 1);

        when(adminUserService.listUsers(eq(1), eq(20), eq("admin")))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("page", "1")
                        .param("size", "20")
                        .param("keyword", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].username").value("admin"));
    }

    @Test
    @DisplayName("listUsers — 普通用户无权访问")
    @WithMockUser(roles = "USER")
    void listUsers_AsUser_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isForbidden());
    }

    // ==================== banUser Tests ====================

    @Test
    @DisplayName("banUser — 管理员封禁用户")
    @WithMockUser(roles = "ADMIN")
    void banUser_AsAdmin_Success() throws Exception {
        doNothing().when(adminUserService).banUser(eq(10L), eq(true));

        mockMvc.perform(patch("/api/v1/admin/users/10/ban")
                        .param("ban", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("用户已封禁"));
    }

    @Test
    @DisplayName("banUser — 管理员解封用户")
    @WithMockUser(roles = "ADMIN")
    void banUser_Unban_Success() throws Exception {
        doNothing().when(adminUserService).banUser(eq(10L), eq(false));

        mockMvc.perform(patch("/api/v1/admin/users/10/ban")
                        .param("ban", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("用户已解封"));
    }

    @Test
    @DisplayName("banUser — 封禁自己不合法")
    @WithMockUser(roles = "ADMIN")
    void banUser_Self_Rejected() throws Exception {
        doThrow(new BusinessException(ResultCode.BAD_REQUEST, "不能封禁/解封自己"))
                .when(adminUserService).banUser(eq(1L), anyBoolean());

        mockMvc.perform(patch("/api/v1/admin/users/1/ban")
                        .param("ban", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("不能封禁/解封自己"));
    }

    @Test
    @DisplayName("banUser — 用户不存在")
    @WithMockUser(roles = "ADMIN")
    void banUser_NotFound_Returns404() throws Exception {
        doThrow(new BusinessException(ResultCode.NOT_FOUND, "用户不存在"))
                .when(adminUserService).banUser(eq(999L), anyBoolean());

        mockMvc.perform(patch("/api/v1/admin/users/999/ban")
                        .param("ban", "true"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }

    // ==================== changeRole Tests ====================

    @Test
    @DisplayName("changeRole — 管理员修改用户角色")
    @WithMockUser(roles = "ADMIN")
    void changeRole_AsAdmin_Success() throws Exception {
        doNothing().when(adminUserService).changeRole(eq(10L), eq("MODERATOR"));

        mockMvc.perform(patch("/api/v1/admin/users/10/role")
                        .param("role", "MODERATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("角色已更新"));
    }

    @Test
    @DisplayName("changeRole — 不能修改自己角色")
    @WithMockUser(roles = "ADMIN")
    void changeRole_Self_Rejected() throws Exception {
        doThrow(new BusinessException(ResultCode.BAD_REQUEST, "不能修改自己的角色"))
                .when(adminUserService).changeRole(eq(1L), anyString());

        mockMvc.perform(patch("/api/v1/admin/users/1/role")
                        .param("role", "MODERATOR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("不能修改自己的角色"));
    }
}
