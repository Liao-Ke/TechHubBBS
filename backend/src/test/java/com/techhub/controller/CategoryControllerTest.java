package com.techhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.common.BusinessException;
import com.techhub.common.ResultCode;
import com.techhub.dto.category.CategoryCreateRequest;
import com.techhub.dto.category.CategoryUpdateRequest;
import com.techhub.entity.Category;
import com.techhub.security.JwtTokenProvider;
import com.techhub.service.CategoryService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("CategoryController 单元测试")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    // ==================== listEnabled Tests ====================

    @Test
    @DisplayName("listEnabled — 返回已启用的版块列表")
    void listEnabled_ReturnsOnlyEnabled() throws Exception {
        Category category = new Category();
        category.setId(1L);
        category.setName("Java");
        category.setDescription("Java programming");
        category.setSortOrder(1);
        category.setStatus(1);
        category.setCreateTime(LocalDateTime.now());

        when(categoryService.listEnabled()).thenReturn(List.of(category));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("Java"))
                .andExpect(jsonPath("$.data[0].id").value("1"));
    }

    // ==================== create Tests ====================

    @Test
    @DisplayName("create — 管理员创建版块成功")
    @WithMockUser(roles = "ADMIN")
    void create_AsAdmin_Success() throws Exception {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("Python");
        request.setDescription("Python zone");
        request.setSortOrder(2);

        Category saved = new Category();
        saved.setId(10L);
        saved.setName("Python");
        saved.setDescription("Python zone");
        saved.setSortOrder(2);
        saved.setStatus(1);

        when(categoryService.create(any(CategoryCreateRequest.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Python"))
                .andExpect(jsonPath("$.data.id").value("10"));
    }

    @Test
    @DisplayName("create — 普通用户创建版块被拒绝")
    @WithMockUser(roles = "USER")
    void create_AsUser_Forbidden() throws Exception {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("HackZone");
        request.setDescription("Should not be allowed");

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== update Tests ====================

    @Test
    @DisplayName("update — 管理员更新版块成功")
    @WithMockUser(roles = "ADMIN")
    void update_Success() throws Exception {
        CategoryUpdateRequest request = new CategoryUpdateRequest();
        request.setName("Java Advanced");
        request.setSortOrder(3);

        doNothing().when(categoryService).update(eq(10L), any(CategoryUpdateRequest.class));

        mockMvc.perform(patch("/api/v1/categories/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("更新成功"));
    }

    // ==================== delete Tests ====================

    @Test
    @DisplayName("delete — 版块下有帖子时删除被拒绝")
    @WithMockUser(roles = "ADMIN")
    void delete_RejectedWhenHasPost() throws Exception {
        doThrow(new BusinessException(ResultCode.BAD_REQUEST, "版块下有帖子"))
                .when(categoryService).delete(eq(10L));

        mockMvc.perform(delete("/api/v1/categories/10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("版块下有帖子"));
    }
}
