package com.techhub.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.security.JwtTokenProvider;
import com.techhub.service.AdminStatisticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AdminStatisticsController 单元测试")
class AdminStatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminStatisticsService adminStatisticsService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    // ==================== GET /api/v1/admin/statistics ====================

    @Test
    @DisplayName("getStatistics — 管理员查询统计数据")
    @WithMockUser(roles = "ADMIN")
    void getStatistics_AsAdmin_Success() throws Exception {
        Map<String, Object> stats = buildMockStats();
        when(adminStatisticsService.getStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/admin/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userCount").value("150"))
                .andExpect(jsonPath("$.data.postCount").value("320"))
                .andExpect(jsonPath("$.data.commentCount").value("1200"))
                .andExpect(jsonPath("$.data.todayNewUsers").value("5"))
                .andExpect(jsonPath("$.data.todayNewPosts").value("12"))
                .andExpect(jsonPath("$.data.activeUsersToday").value("45"));
    }

    @Test
    @DisplayName("getStatistics — 版主也可查询统计数据")
    @WithMockUser(roles = "MODERATOR")
    void getStatistics_AsModerator_Success() throws Exception {
        Map<String, Object> stats = buildMockStats();
        when(adminStatisticsService.getStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/admin/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userCount").value("150"));
    }

    @Test
    @DisplayName("getStatistics — 普通用户无权访问")
    @WithMockUser(roles = "USER")
    void getStatistics_AsUser_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/statistics"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getStatistics — 验证所有字段存在且为字符串(Long)")
    @WithMockUser(roles = "ADMIN")
    void getStatistics_AllFieldsPresent() throws Exception {
        Map<String, Object> stats = buildMockStats();
        when(adminStatisticsService.getStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/admin/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userCount").isString())
                .andExpect(jsonPath("$.data.postCount").isString())
                .andExpect(jsonPath("$.data.commentCount").isString())
                .andExpect(jsonPath("$.data.todayNewUsers").isString())
                .andExpect(jsonPath("$.data.todayNewPosts").isString())
                .andExpect(jsonPath("$.data.activeUsersToday").isString());
    }

    private Map<String, Object> buildMockStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("userCount", 150L);
        stats.put("postCount", 320L);
        stats.put("commentCount", 1200L);
        stats.put("todayNewUsers", 5L);
        stats.put("todayNewPosts", 12L);
        stats.put("activeUsersToday", 45L);
        return stats;
    }
}
