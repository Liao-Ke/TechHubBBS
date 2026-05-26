package com.techhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.ResultCode;
import com.techhub.dto.post.PostVO;
import com.techhub.security.JwtTokenProvider;
import com.techhub.service.AdminPostService;
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AdminPostController 单元测试")
class AdminPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminPostService adminPostService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    private PostVO mockPost() {
        PostVO vo = new PostVO();
        vo.setId("1");
        vo.setTitle("Test Post");
        vo.setContent("Test content");
        vo.setCategoryId("10");
        vo.setCategoryName("Java");
        vo.setAuthorId("1");
        vo.setAuthorName("testuser");
        vo.setType(0);
        vo.setStatus(1);
        vo.setVisibility(0);
        vo.setViewCount(100);
        vo.setLikeCount(10);
        vo.setCommentCount(5);
        vo.setCreateTime(LocalDateTime.now());
        vo.setUpdateTime(LocalDateTime.now());
        return vo;
    }

    // ==================== listAllPosts Tests ====================

    @Test
    @DisplayName("listAllPosts — 管理员分页查询所有帖子")
    @WithMockUser(roles = "ADMIN")
    void listAllPosts_AsAdmin_Success() throws Exception {
        PostVO postVO = mockPost();
        PageResult<PostVO> pageResult = PageResult.of(List.of(postVO), 1, 20, 1);

        when(adminPostService.listAllPosts(anyInt(), anyInt(), eq(null)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/admin/posts")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value("1"))
                .andExpect(jsonPath("$.data.records[0].title").value("Test Post"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("listAllPosts — 版主也可查询")
    @WithMockUser(roles = "MODERATOR")
    void listAllPosts_AsModerator_Success() throws Exception {
        PageResult<PostVO> pageResult = PageResult.of(List.of(), 0, 20, 1);

        when(adminPostService.listAllPosts(anyInt(), anyInt(), eq(null)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/admin/posts")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("listAllPosts — 普通用户无权访问")
    @WithMockUser(roles = "USER")
    void listAllPosts_AsUser_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/posts")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isForbidden());
    }

    // ==================== setPostType Tests ====================

    @Test
    @DisplayName("setPostType — 版主设置帖子为精华")
    @WithMockUser(roles = "MODERATOR")
    void setPostType_Feature_Success() throws Exception {
        doNothing().when(adminPostService).setPostType(eq(1L), eq(1));

        mockMvc.perform(patch("/api/v1/admin/posts/1/type")
                        .param("type", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("帖子类型已更新"));
    }

    @Test
    @DisplayName("setPostType — 版主设置帖子为置顶")
    @WithMockUser(roles = "MODERATOR")
    void setPostType_Pin_Success() throws Exception {
        doNothing().when(adminPostService).setPostType(eq(1L), eq(2));

        mockMvc.perform(patch("/api/v1/admin/posts/1/type")
                        .param("type", "2"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("setPostType — 帖子不存在")
    @WithMockUser(roles = "ADMIN")
    void setPostType_NotFound_Returns404() throws Exception {
        doThrow(new BusinessException(ResultCode.NOT_FOUND, "帖子不存在"))
                .when(adminPostService).setPostType(eq(999L), anyInt());

        mockMvc.perform(patch("/api/v1/admin/posts/999/type")
                        .param("type", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("帖子不存在"));
    }

    // ==================== toggleLock Tests ====================

    @Test
    @DisplayName("toggleLock — 管理员锁定帖子")
    @WithMockUser(roles = "ADMIN")
    void toggleLock_Success() throws Exception {
        doNothing().when(adminPostService).togglePostLock(eq(1L));

        mockMvc.perform(patch("/api/v1/admin/posts/1/lock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("帖子锁定状态已切换"));
    }

    // ==================== forceDelete Tests ====================

    @Test
    @DisplayName("forceDelete — 管理员强制删除帖子")
    @WithMockUser(roles = "ADMIN")
    void forceDelete_AsAdmin_Success() throws Exception {
        doNothing().when(adminPostService).forceDeletePost(eq(1L));

        mockMvc.perform(delete("/api/v1/admin/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("帖子已强制删除"));
    }

    @Test
    @DisplayName("forceDelete — 版主无权强制删除")
    @WithMockUser(roles = "MODERATOR")
    void forceDelete_AsModerator_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/posts/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("forceDelete — 帖子不存在")
    @WithMockUser(roles = "ADMIN")
    void forceDelete_NotFound_Returns404() throws Exception {
        doThrow(new BusinessException(ResultCode.NOT_FOUND, "帖子不存在"))
                .when(adminPostService).forceDeletePost(eq(999L));

        mockMvc.perform(delete("/api/v1/admin/posts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("帖子不存在"));
    }
}
