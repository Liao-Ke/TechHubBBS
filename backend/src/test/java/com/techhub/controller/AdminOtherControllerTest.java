package com.techhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.ResultCode;
import com.techhub.dto.category.CategoryNoticeVO;
import com.techhub.security.JwtTokenProvider;
import com.techhub.service.AdminOtherService;
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
@DisplayName("AdminOtherController 单元测试")
class AdminOtherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminOtherService adminOtherService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    // ==================== forceSetDivine Tests ====================

    @Test
    @DisplayName("forceSetDivine — 管理员强制设置神评")
    @WithMockUser(roles = "ADMIN")
    void forceSetDivine_Success() throws Exception {
        doNothing().when(adminOtherService).forceSetDivine(eq(10L), eq(true));

        mockMvc.perform(patch("/api/v1/admin/comments/10/divine")
                        .param("divine", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已强制设置为神评"));
    }

    @Test
    @DisplayName("forceSetDivine — 管理员取消神评")
    @WithMockUser(roles = "ADMIN")
    void forceSetDivine_Unset_Success() throws Exception {
        doNothing().when(adminOtherService).forceSetDivine(eq(10L), eq(false));

        mockMvc.perform(patch("/api/v1/admin/comments/10/divine")
                        .param("divine", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("已取消神评"));
    }

    @Test
    @DisplayName("forceSetDivine — 评论不存在")
    @WithMockUser(roles = "ADMIN")
    void forceSetDivine_NotFound_Returns404() throws Exception {
        doThrow(new BusinessException(ResultCode.NOT_FOUND, "评论不存在"))
                .when(adminOtherService).forceSetDivine(eq(999L), anyBoolean());

        mockMvc.perform(patch("/api/v1/admin/comments/999/divine")
                        .param("divine", "true"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("评论不存在"));
    }

    @Test
    @DisplayName("forceSetDivine — 普通用户无权操作")
    @WithMockUser(roles = "USER")
    void forceSetDivine_AsUser_Forbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/comments/10/divine")
                        .param("divine", "true"))
                .andExpect(status().isForbidden());
    }

    // ==================== listNotices Tests ====================

    @Test
    @DisplayName("listNotices — 管理员分页查询所有公告")
    @WithMockUser(roles = "ADMIN")
    void listNotices_AsAdmin_Success() throws Exception {
        CategoryNoticeVO notice = new CategoryNoticeVO();
        notice.setId("1");
        notice.setCategoryId("10");
        notice.setTitle("Announcement");
        notice.setContent("Content here");
        notice.setType(0);
        notice.setAuthorId("1");
        notice.setAuthorName("admin");
        notice.setIsPinned(1);
        notice.setStatus(1);
        notice.setCreateTime(LocalDateTime.now());

        PageResult<CategoryNoticeVO> pageResult = PageResult.of(List.of(notice), 1, 20, 1);

        when(adminOtherService.listNotices(anyInt(), anyInt(), eq(null), eq(null)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/admin/notices")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value("1"))
                .andExpect(jsonPath("$.data.records[0].title").value("Announcement"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("listNotices — 版主也可查询")
    @WithMockUser(roles = "MODERATOR")
    void listNotices_AsModerator_Success() throws Exception {
        PageResult<CategoryNoticeVO> pageResult = PageResult.of(List.of(), 0, 20, 1);

        when(adminOtherService.listNotices(anyInt(), anyInt(), eq(null), eq(null)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/admin/notices")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("listNotices — 按版块和类型过滤")
    @WithMockUser(roles = "ADMIN")
    void listNotices_WithFilters_Success() throws Exception {
        CategoryNoticeVO notice = new CategoryNoticeVO();
        notice.setId("2");
        notice.setTitle("Event Notice");
        notice.setType(1);

        PageResult<CategoryNoticeVO> pageResult = PageResult.of(List.of(notice), 1, 20, 1);

        when(adminOtherService.listNotices(eq(1), eq(20), eq(10L), eq(1)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/admin/notices")
                        .param("page", "1")
                        .param("size", "20")
                        .param("categoryId", "10")
                        .param("type", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].type").value(1));
    }

    @Test
    @DisplayName("listNotices — 普通用户无权访问")
    @WithMockUser(roles = "USER")
    void listNotices_AsUser_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/notices")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isForbidden());
    }
}
