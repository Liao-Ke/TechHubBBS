package com.techhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.common.BusinessException;
import com.techhub.common.ResultCode;
import com.techhub.dto.DraftSaveRequest;
import com.techhub.entity.PostDraft;
import com.techhub.security.JwtTokenProvider;
import com.techhub.service.DraftService;
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

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("DraftController 单元测试")
class DraftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DraftService draftService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    private PostDraft mockDraft;

    @BeforeEach
    void setUp() {
        // Mock authenticated user with userId=1, role=USER
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        mockDraft = new PostDraft();
        mockDraft.setId(100L);
        mockDraft.setUserId(1L);
        mockDraft.setPostId(null);
        mockDraft.setTitle("Test Draft Title");
        mockDraft.setContent("Test draft content");
        mockDraft.setCategoryId(10L);
        mockDraft.setVisibility(1);
        mockDraft.setLastSavedAt(LocalDateTime.now());
        mockDraft.setCreateTime(LocalDateTime.now());
        mockDraft.setUpdateTime(LocalDateTime.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== POST /api/v1/drafts ====================

    @Test
    @DisplayName("saveDraft — 新建帖子草稿成功")
    void saveDraft_NewPost_Creates() throws Exception {
        DraftSaveRequest request = new DraftSaveRequest();
        request.setPostId(null);
        request.setTitle("New Draft");
        request.setContent("Draft content here");
        request.setCategoryId(10L);
        request.setVisibility(1);

        PostDraft saved = new PostDraft();
        saved.setId(200L);
        saved.setUserId(1L);
        saved.setPostId(null);
        saved.setTitle("New Draft");
        saved.setContent("Draft content here");
        saved.setCategoryId(10L);
        saved.setVisibility(1);
        saved.setLastSavedAt(LocalDateTime.now());
        saved.setCreateTime(LocalDateTime.now());
        saved.setUpdateTime(LocalDateTime.now());

        when(draftService.saveDraft(any(DraftSaveRequest.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("200"))
                .andExpect(jsonPath("$.data.title").value("New Draft"))
                .andExpect(jsonPath("$.data.categoryId").value(10))
                .andExpect(jsonPath("$.data.visibility").value(1));
    }

    @Test
    @DisplayName("saveDraft — 编辑已有帖子的草稿更新成功")
    void saveDraft_ExistingPost_Updates() throws Exception {
        DraftSaveRequest request = new DraftSaveRequest();
        request.setPostId(50L);
        request.setTitle("Updated Draft Title");
        request.setContent("Updated draft content");
        request.setCategoryId(20L);
        request.setVisibility(2);

        PostDraft updated = new PostDraft();
        updated.setId(100L);
        updated.setUserId(1L);
        updated.setPostId(50L);
        updated.setTitle("Updated Draft Title");
        updated.setContent("Updated draft content");
        updated.setCategoryId(20L);
        updated.setVisibility(2);
        updated.setLastSavedAt(LocalDateTime.now());
        updated.setCreateTime(LocalDateTime.now());
        updated.setUpdateTime(LocalDateTime.now());

        when(draftService.saveDraft(any(DraftSaveRequest.class))).thenReturn(updated);

        mockMvc.perform(post("/api/v1/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("100"))
                .andExpect(jsonPath("$.data.title").value("Updated Draft Title"))
                .andExpect(jsonPath("$.data.postId").value("50"));
    }

    // ==================== GET /api/v1/drafts ====================

    @Test
    @DisplayName("listDrafts — 返回当前用户的草稿列表")
    void listDrafts_ReturnsUserDrafts() throws Exception {
        PostDraft draft1 = new PostDraft();
        draft1.setId(101L);
        draft1.setUserId(1L);
        draft1.setTitle("Draft One");
        draft1.setContent("Content one");
        draft1.setLastSavedAt(LocalDateTime.now());

        PostDraft draft2 = new PostDraft();
        draft2.setId(102L);
        draft2.setUserId(1L);
        draft2.setTitle("Draft Two");
        draft2.setContent("Content two");
        draft2.setLastSavedAt(LocalDateTime.now().minusHours(1));

        when(draftService.listDrafts()).thenReturn(List.of(draft1, draft2));

        mockMvc.perform(get("/api/v1/drafts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value("101"))
                .andExpect(jsonPath("$.data[0].title").value("Draft One"))
                .andExpect(jsonPath("$.data[1].id").value("102"))
                .andExpect(jsonPath("$.data[1].title").value("Draft Two"));
    }

    // ==================== GET /api/v1/drafts/check ====================

    @Test
    @DisplayName("checkDraft — 存在草稿时返回草稿")
    void checkDraft_Exists_ReturnsDraft() throws Exception {
        PostDraft draft = new PostDraft();
        draft.setId(300L);
        draft.setUserId(1L);
        draft.setPostId(99L);
        draft.setTitle("Pending Draft");
        draft.setContent("Pending content");
        draft.setCategoryId(10L);
        draft.setVisibility(1);
        draft.setLastSavedAt(LocalDateTime.now());

        when(draftService.checkDraft(99L)).thenReturn(draft);

        mockMvc.perform(get("/api/v1/drafts/check")
                        .param("postId", "99")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("300"))
                .andExpect(jsonPath("$.data.title").value("Pending Draft"))
                .andExpect(jsonPath("$.data.postId").value("99"));
    }

    // ==================== GET /api/v1/drafts/{id} ====================

    @Test
    @DisplayName("getDraft — 草稿存在时返回草稿")
    void getDraft_Exists_ReturnsDraft() throws Exception {
        when(draftService.getById(100L)).thenReturn(mockDraft);

        mockMvc.perform(get("/api/v1/drafts/100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("100"))
                .andExpect(jsonPath("$.data.title").value("Test Draft Title"))
                .andExpect(jsonPath("$.data.content").value("Test draft content"));
    }

    @Test
    @DisplayName("getDraft — 草稿不存在时返回 404")
    void getDraft_NotFound_Returns404() throws Exception {
        when(draftService.getById(999L))
                .thenThrow(new BusinessException(ResultCode.NOT_FOUND, "草稿不存在"));

        mockMvc.perform(get("/api/v1/drafts/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("草稿不存在"));
    }

    // ==================== DELETE /api/v1/drafts/{id} ====================

    @Test
    @DisplayName("deleteDraft — 草稿所有者删除成功")
    void deleteDraft_AsOwner_Success() throws Exception {
        doNothing().when(draftService).deleteDraft(100L);

        mockMvc.perform(delete("/api/v1/drafts/100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("删除成功"));
    }

    @Test
    @DisplayName("deleteDraft — 无权删除他人草稿返回 403")
    void deleteDraft_NotOwner_Forbidden() throws Exception {
        doThrow(new BusinessException(ResultCode.FORBIDDEN, "无权操作此草稿"))
                .when(draftService).deleteDraft(999L);

        mockMvc.perform(delete("/api/v1/drafts/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
