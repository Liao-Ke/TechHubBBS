package com.techhub.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.dto.category.CategoryCreateRequest;
import com.techhub.dto.category.CategoryUpdateRequest;
import com.techhub.entity.Category;
import com.techhub.mapper.CategoryMapper;
import com.techhub.security.JwtTokenProvider;
import com.techhub.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AdminCategoryController 单元测试")
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private CategoryMapper categoryMapper;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    private Category category1;
    private Category category2;

    @BeforeEach
    void setUp() {
        category1 = new Category();
        category1.setId(1L);
        category1.setName("Java");
        category1.setDescription("Java 技术讨论");
        category1.setSortOrder(1);
        category1.setStatus(1);

        category2 = new Category();
        category2.setId(2L);
        category2.setName("Python");
        category2.setDescription("Python 技术讨论");
        category2.setSortOrder(2);
        category2.setStatus(0);
    }

    // ==================== GET /api/v1/admin/categories ====================

    @Test
    @DisplayName("listAll — 管理员获取所有版块（含禁用）")
    @WithMockUser(roles = "ADMIN")
    void listAll_AsAdmin_ReturnsAllCategories() throws Exception {
        when(categoryMapper.selectList(any())).thenReturn(List.of(category1, category2));

        mockMvc.perform(get("/api/v1/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Java"))
                .andExpect(jsonPath("$.data[0].status").value(1))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].name").value("Python"))
                .andExpect(jsonPath("$.data[1].status").value(0));
    }

    @Test
    @DisplayName("listAll — 普通用户无权访问")
    @WithMockUser(roles = "USER")
    void listAll_AsUser_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/categories"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("listAll — 匿名用户无权访问")
    @WithAnonymousUser
    void listAll_Anonymous_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/categories"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/admin/categories ====================

    @Test
    @DisplayName("create — 管理员创建版块成功")
    @WithMockUser(roles = "ADMIN")
    void create_AsAdmin_Success() throws Exception {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("Go");
        request.setDescription("Go 语言技术讨论");
        request.setSortOrder(3);

        Category created = new Category();
        created.setId(3L);
        created.setName("Go");
        created.setDescription("Go 语言技术讨论");
        created.setSortOrder(3);
        created.setStatus(1);

        when(categoryService.create(any(CategoryCreateRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.name").value("Go"))
                .andExpect(jsonPath("$.data.status").value(1));
    }

    @Test
    @DisplayName("create — 名称为空返回 400")
    @WithMockUser(roles = "ADMIN")
    void create_EmptyName_BadRequest() throws Exception {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("");

        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("create — 普通用户无权访问")
    @WithMockUser(roles = "USER")
    void create_AsUser_Forbidden() throws Exception {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("Go");

        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== PATCH /api/v1/admin/categories/{id} ====================

    @Test
    @DisplayName("update — 管理员更新版块成功")
    @WithMockUser(roles = "ADMIN")
    void update_AsAdmin_Success() throws Exception {
        CategoryUpdateRequest request = new CategoryUpdateRequest();
        request.setName("Java 高级");
        request.setDescription("Java 高级技术讨论");

        doNothing().when(categoryService).update(any(Long.class), any(CategoryUpdateRequest.class));

        mockMvc.perform(patch("/api/v1/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("更新成功"));
    }

    @Test
    @DisplayName("update — 普通用户无权访问")
    @WithMockUser(roles = "USER")
    void update_AsUser_Forbidden() throws Exception {
        CategoryUpdateRequest request = new CategoryUpdateRequest();
        request.setName("Java 高级");

        mockMvc.perform(patch("/api/v1/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/v1/admin/categories/{id} ====================

    @Test
    @DisplayName("delete — 管理员删除版块成功")
    @WithMockUser(roles = "ADMIN")
    void delete_AsAdmin_Success() throws Exception {
        doNothing().when(categoryService).delete(1L);

        mockMvc.perform(delete("/api/v1/admin/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("删除成功"));
    }

    @Test
    @DisplayName("delete — 普通用户无权访问")
    @WithMockUser(roles = "USER")
    void delete_AsUser_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/categories/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== PATCH /api/v1/admin/categories/{id}/status ====================

    @Test
    @DisplayName("toggleStatus — 管理员启用版块")
    @WithMockUser(roles = "ADMIN")
    void toggleStatus_Enable_Success() throws Exception {
        doNothing().when(categoryService).update(any(Long.class), any(CategoryUpdateRequest.class));

        mockMvc.perform(patch("/api/v1/admin/categories/1/status")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("版块已启用"));
    }

    @Test
    @DisplayName("toggleStatus — 管理员禁用版块")
    @WithMockUser(roles = "ADMIN")
    void toggleStatus_Disable_Success() throws Exception {
        doNothing().when(categoryService).update(any(Long.class), any(CategoryUpdateRequest.class));

        mockMvc.perform(patch("/api/v1/admin/categories/1/status")
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("版块已禁用"));
    }

    @Test
    @DisplayName("toggleStatus — 普通用户无权访问")
    @WithMockUser(roles = "USER")
    void toggleStatus_AsUser_Forbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/categories/1/status")
                        .param("status", "1"))
                .andExpect(status().isForbidden());
    }
}
