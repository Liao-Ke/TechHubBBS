package com.techhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.ResultCode;
import com.techhub.dto.comment.CommentCreateRequest;
import com.techhub.dto.comment.CommentVO;
import com.techhub.security.JwtTokenProvider;
import com.techhub.service.CommentService;
import com.techhub.service.DivineCommentService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("CommentController 单元测试")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private DivineCommentService divineCommentService;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    void setUp() {
        // Mock authenticated user with userId=1, role=USER
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== GET /api/v1/posts/{postId}/comments ====================

    @Test
    @DisplayName("listComments — 返回帖子的评论列表并填充作者信息")
    void listComments_ReturnsPaginatedComments() throws Exception {
        CommentVO vo1 = new CommentVO();
        vo1.setId("201");
        vo1.setContent("第一条评论");
        vo1.setPostId("10");
        vo1.setUserId("1");
        vo1.setUsername("testuser");
        vo1.setAvatarUrl("/avatar.png");
        vo1.setLikeCount(3);
        vo1.setRecommendCount(0);
        vo1.setIsDivine(false);
        vo1.setLiked(false);
        vo1.setCreateTime(LocalDateTime.now());

        CommentVO vo2 = new CommentVO();
        vo2.setId("202");
        vo2.setContent("第二条评论");
        vo2.setPostId("10");
        vo2.setUserId("2");
        vo2.setUsername("otheruser");
        vo2.setAvatarUrl(null);
        vo2.setLikeCount(0);
        vo2.setRecommendCount(0);
        vo2.setIsDivine(false);
        vo2.setLiked(false);
        vo2.setCreateTime(LocalDateTime.now());

        PageResult<CommentVO> pageResult = new PageResult<>(List.of(vo1, vo2), 2, 20, 1);

        when(commentService.listByPost(eq(10L), eq(1), eq(20), eq(1L))).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/posts/10/comments")
                        .param("page", "1")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records", hasSize(2)))
                .andExpect(jsonPath("$.data.records[0].id").value("201"))
                .andExpect(jsonPath("$.data.records[0].content").value("第一条评论"))
                .andExpect(jsonPath("$.data.records[0].username").value("testuser"))
                .andExpect(jsonPath("$.data.records[0].likeCount").value(3))
                .andExpect(jsonPath("$.data.records[1].id").value("202"))
                .andExpect(jsonPath("$.data.records[1].username").value("otheruser"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.current").value(1));
    }

    // ==================== POST /api/v1/posts/{postId}/comments ====================

    @Test
    @DisplayName("createComment — 发表评论成功返回评论 VO")
    void createComment_Success_ReturnsCommentVO() throws Exception {
        CommentCreateRequest request = new CommentCreateRequest();
        request.setContent("这是一条测试评论");
        request.setParentId(null);
        request.setReplyToUserId(null);

        CommentVO created = new CommentVO();
        created.setId("300");
        created.setContent("这是一条测试评论");
        created.setPostId("10");
        created.setUserId("1");
        created.setUsername("testuser");
        created.setAvatarUrl("/avatar.png");
        created.setParentId(null);
        created.setReplyToUserId(null);
        created.setLikeCount(0);
        created.setRecommendCount(0);
        created.setIsDivine(false);
        created.setLiked(false);
        created.setCreateTime(LocalDateTime.now());

        when(commentService.create(eq(10L), any(CommentCreateRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/posts/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("300"))
                .andExpect(jsonPath("$.data.content").value("这是一条测试评论"))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.likeCount").value(0));
    }

    // ==================== DELETE /api/v1/comments/{id} ====================

    @Test
    @DisplayName("deleteComment — 作者本人删除评论成功")
    void deleteComment_AsAuthor_Success() throws Exception {
        doNothing().when(commentService).delete(100L);

        mockMvc.perform(delete("/api/v1/comments/100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("删除成功"));
    }

    // ==================== POST /api/v1/comments/{id}/recommend ====================

    @Test
    @DisplayName("recommendComment — 推荐神评成功")
    void recommendComment_Success() throws Exception {
        doNothing().when(divineCommentService).recommend(100L);

        mockMvc.perform(post("/api/v1/comments/100/recommend")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("推荐成功"));
    }

    // ==================== DELETE /api/v1/comments/{id}/recommend ====================

    @Test
    @DisplayName("cancelRecommendComment — 取消推荐神评成功")
    void cancelRecommendComment_Success() throws Exception {
        doNothing().when(divineCommentService).cancelRecommend(100L);

        mockMvc.perform(delete("/api/v1/comments/100/recommend")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("取消推荐成功"));
    }

    // ==================== GET /api/v1/posts/{postId}/comments/divine ====================

    @Test
    @DisplayName("listDivineComments — 返回神评列表")
    void listDivineComments_ReturnsDivineComments() throws Exception {
        CommentVO vo1 = new CommentVO();
        vo1.setId("301");
        vo1.setContent("神评内容1");
        vo1.setPostId("10");
        vo1.setUserId("1");
        vo1.setUsername("testuser");
        vo1.setIsDivine(true);
        vo1.setCreateTime(LocalDateTime.now());

        CommentVO vo2 = new CommentVO();
        vo2.setId("302");
        vo2.setContent("神评内容2");
        vo2.setPostId("10");
        vo2.setUserId("2");
        vo2.setUsername("otheruser");
        vo2.setIsDivine(true);
        vo2.setCreateTime(LocalDateTime.now());

        when(divineCommentService.listDivineComments(10L)).thenReturn(List.of(vo1, vo2));

        mockMvc.perform(get("/api/v1/posts/10/comments/divine")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value("301"))
                .andExpect(jsonPath("$.data[0].content").value("神评内容1"))
                .andExpect(jsonPath("$.data[0].isDivine").value(true))
                .andExpect(jsonPath("$.data[1].id").value("302"))
                .andExpect(jsonPath("$.data[1].content").value("神评内容2"))
                .andExpect(jsonPath("$.data[1].isDivine").value(true));
    }

    @Test
    @DisplayName("deleteComment — 无权删除他人评论返回 403")
    void deleteComment_NotAuthor_Forbidden() throws Exception {
        doThrow(new BusinessException(ResultCode.FORBIDDEN, "无权删除此评论"))
                .when(commentService).delete(999L);

        mockMvc.perform(delete("/api/v1/comments/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
