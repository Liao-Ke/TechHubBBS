package com.techhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.common.BusinessException;
import com.techhub.common.ResultCode;
import com.techhub.dto.category.CategoryNoticeCreateRequest;
import com.techhub.dto.category.CategoryNoticeUpdateRequest;
import com.techhub.dto.category.CategoryNoticeVO;
import com.techhub.security.JwtTokenProvider;
import com.techhub.service.CategoryNoticeService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("NoticeController 单元测试")
class NoticeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryNoticeService noticeService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    private CategoryNoticeVO mockNotice;

    @BeforeEach
    void setUp() {
        // Mock authenticated user with userId=1, role=ADMIN for create tests
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        mockNotice = new CategoryNoticeVO();
        mockNotice.setId("20");
        mockNotice.setCategoryId("10");
        mockNotice.setTitle("Test Notice");
        mockNotice.setContent("Test content");
        mockNotice.setType(0);
        mockNotice.setAuthorId("1");
        mockNotice.setAuthorName("admin");
        mockNotice.setIsPinned(1);
        mockNotice.setStatus(1);
        mockNotice.setCreateTime(LocalDateTime.now());
        mockNotice.setUpdateTime(LocalDateTime.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== GET /categories/{categoryId}/notices ====================

    @Test
    @DisplayName("listByCategory — 返回版块公告列表，置顶优先")
    void listByCategory_ReturnsPinnedFirst() throws Exception {
        CategoryNoticeVO pinned = new CategoryNoticeVO();
        pinned.setId("1");
        pinned.setCategoryId("10");
        pinned.setTitle("置顶公告");
        pinned.setContent("重要");
        pinned.setType(0);
        pinned.setAuthorId("1");
        pinned.setAuthorName("admin");
        pinned.setIsPinned(1);
        pinned.setStatus(1);
        pinned.setCreateTime(LocalDateTime.now().minusDays(1));
        pinned.setUpdateTime(LocalDateTime.now());

        CategoryNoticeVO normal = new CategoryNoticeVO();
        normal.setId("2");
        normal.setCategoryId("10");
        normal.setTitle("普通公告");
        normal.setContent("一般");
        normal.setType(0);
        normal.setAuthorId("1");
        normal.setAuthorName("admin");
        normal.setIsPinned(0);
        normal.setStatus(1);
        normal.setCreateTime(LocalDateTime.now());
        normal.setUpdateTime(LocalDateTime.now());

        when(noticeService.listByCategory(10L, null))
                .thenReturn(List.of(pinned, normal));

        mockMvc.perform(get("/api/v1/categories/10/notices")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value("1"))
                .andExpect(jsonPath("$.data[0].title").value("置顶公告"))
                .andExpect(jsonPath("$.data[0].isPinned").value(1))
                .andExpect(jsonPath("$.data[1].id").value("2"))
                .andExpect(jsonPath("$.data[1].isPinned").value(0));
    }

    // ==================== POST /categories/{categoryId}/notices ====================

    @Test
    @DisplayName("create — 管理员创建公告成功")
    void create_AsAdmin_Success() throws Exception {
        CategoryNoticeCreateRequest request = new CategoryNoticeCreateRequest();
        request.setTitle("新公告");
        request.setContent("这是内容");
        request.setType(0);
        request.setIsPinned(0);

        when(noticeService.create(eq(10L), any(CategoryNoticeCreateRequest.class), eq(1L)))
                .thenReturn(mockNotice);

        mockMvc.perform(post("/api/v1/categories/10/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("20"))
                .andExpect(jsonPath("$.data.title").value("Test Notice"));
    }

    // ==================== GET /notices/{id} ====================

    @Test
    @DisplayName("getById — 公开访问，无需认证")
    void getById_Public() throws Exception {
        when(noticeService.getById(20L)).thenReturn(mockNotice);

        mockMvc.perform(get("/api/v1/notices/20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("20"))
                .andExpect(jsonPath("$.data.title").value("Test Notice"))
                .andExpect(jsonPath("$.data.authorName").value("admin"));
    }

    @Test
    @DisplayName("getById — 公告不存在返回 404")
    void getById_NotFound_Returns404() throws Exception {
        when(noticeService.getById(999L))
                .thenThrow(new BusinessException(ResultCode.NOT_FOUND, "公告不存在"));

        mockMvc.perform(get("/api/v1/notices/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ==================== PATCH /notices/{id} ====================

    @Test
    @DisplayName("update — 作者编辑公告成功")
    void update_AsAuthor_Success() throws Exception {
        CategoryNoticeUpdateRequest request = new CategoryNoticeUpdateRequest();
        request.setTitle("更新后的标题");
        request.setIsPinned(1);

        doNothing().when(noticeService).update(eq(20L), any(CategoryNoticeUpdateRequest.class));

        mockMvc.perform(patch("/api/v1/notices/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("更新成功"));
    }

    // ==================== DELETE /notices/{id} ====================

    @Test
    @DisplayName("delete — 作者删除公告成功")
    void delete_AsAuthor_Success() throws Exception {
        doNothing().when(noticeService).delete(20L);

        mockMvc.perform(delete("/api/v1/notices/20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("删除成功"));
    }
}
