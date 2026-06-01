package com.techhub.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 并发场景集成测试：点赞/收藏/关注防重复唯一约束。
 * 验证数据库 uk_user_target、uk_user_post、uk_follower_followee 唯一索引的并发安全。
 */
@Sql(scripts = {"classpath:sql/h2-schema.sql", "classpath:sql/h2-integration-data.sql"})
@DisplayName("并发防重复集成测试")
class ConcurrencyIntegrationTest extends BaseIntegrationTest {

    private String user1Token;
    private String user1Id;
    private String user2Token;
    private String user2Id;
    private String postId;
    private String commentId;

    @BeforeEach
    void setUp() throws Exception {
        // Register user1
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"con_user1\",\"password\":\"password123\",\"email\":\"con_user1@test.com\"}"));
        String resp1 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"con_user1\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        user1Token = objectMapper.readTree(resp1).get("data").get("token").asText();
        user1Id = objectMapper.readTree(resp1).get("data").get("userId").asText();

        // Register user2
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"con_user2\",\"password\":\"password123\",\"email\":\"con_user2@test.com\"}"));
        String resp2 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"con_user2\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        user2Token = objectMapper.readTree(resp2).get("data").get("token").asText();
        user2Id = objectMapper.readTree(resp2).get("data").get("userId").asText();

        // Create a post
        String createResp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearerToken(user1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Concurrency Test\",\"content\":\""
                                + "Testing duplicate ".repeat(5)
                                + "\",\"categoryId\":1000,\"visibility\":0}"))
                .andReturn().getResponse().getContentAsString();
        postId = objectMapper.readTree(createResp).get("data").get("id").asText();

        // Create a comment
        String commentResp = mockMvc.perform(post("/api/v1/posts/{postId}/comments", postId)
                        .header("Authorization", bearerToken(user1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + "Comment for like test ".repeat(3) + "\"}"))
                .andReturn().getResponse().getContentAsString();
        commentId = objectMapper.readTree(commentResp).get("data").get("id").asText();
    }

    // ==================== 帖子点赞防重复 ====================

    @Test
    @DisplayName("帖子点赞: 首次 → 200")
    void likePost_FirstTime_Success() throws Exception {
        mockMvc.perform(post("/api/v1/posts/{id}/likes", postId)
                        .header("Authorization", bearerToken(user1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("点赞成功"));
    }

    @Test
    @DisplayName("帖子点赞: 重复 → 409 (uk_user_target唯一约束)")
    void likePost_Duplicate_Conflict() throws Exception {
        mockMvc.perform(post("/api/v1/posts/{id}/likes", postId)
                .header("Authorization", bearerToken(user1Token)));

        mockMvc.perform(post("/api/v1/posts/{id}/likes", postId)
                        .header("Authorization", bearerToken(user1Token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("已经点赞过了"));
    }

    @Test
    @DisplayName("帖子点赞: 不同用户点赞同一帖 → 都成功")
    void likePost_DifferentUsers_Succeed() throws Exception {
        mockMvc.perform(post("/api/v1/posts/{id}/likes", postId)
                .header("Authorization", bearerToken(user1Token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/posts/{id}/likes", postId)
                .header("Authorization", bearerToken(user2Token)))
                .andExpect(status().isOk());
    }

    // ==================== 评论点赞防重复 ====================

    @Test
    @DisplayName("评论点赞: 重复 → 409")
    void likeComment_Duplicate_Conflict() throws Exception {
        mockMvc.perform(post("/api/v1/comments/{id}/likes", commentId)
                .header("Authorization", bearerToken(user1Token)));

        mockMvc.perform(post("/api/v1/comments/{id}/likes", commentId)
                        .header("Authorization", bearerToken(user1Token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("已经点赞过了"));
    }

    @Test
    @DisplayName("评论点赞 + 帖子点赞: 同一用户不同targetType → 都成功")
    void likePostAndComment_SameUser_Succeed() throws Exception {
        mockMvc.perform(post("/api/v1/posts/{id}/likes", postId)
                        .header("Authorization", bearerToken(user1Token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/comments/{id}/likes", commentId)
                        .header("Authorization", bearerToken(user1Token)))
                .andExpect(status().isOk());
    }

    // ==================== 关注防重复 ====================

    @Test
    @DisplayName("关注用户: 首次 → 200")
    void followUser_FirstTime_Success() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/follow", user2Id)
                        .header("Authorization", bearerToken(user1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("关注成功"));
    }

    @Test
    @DisplayName("关注用户: 重复 → 409 (uk_follower_followee唯一约束)")
    void followUser_Duplicate_Conflict() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/follow", user2Id)
                .header("Authorization", bearerToken(user1Token)));

        mockMvc.perform(post("/api/v1/users/{id}/follow", user2Id)
                        .header("Authorization", bearerToken(user1Token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("已关注该用户"));
    }

    @Test
    @DisplayName("关注用户: 自己关注自己 → 400")
    void followSelf_BadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/follow", user1Id)
                        .header("Authorization", bearerToken(user1Token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("不能关注自己"));
    }

    @Test
    @DisplayName("关注用户: 相互关注 → 都成功 (方向不同键不同)")
    void followEachOther_Succeed() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/follow", user2Id)
                        .header("Authorization", bearerToken(user1Token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/users/{id}/follow", user1Id)
                        .header("Authorization", bearerToken(user2Token)))
                .andExpect(status().isOk());
    }

    // ==================== 收藏防重复 ====================

    @Test
    @DisplayName("收藏帖子: 重复 → 409 (uk_user_post唯一约束)")
    void favoritePost_Duplicate_Conflict() throws Exception {
        mockMvc.perform(post("/api/v1/posts/{id}/favorites", postId)
                .header("Authorization", bearerToken(user1Token)));

        mockMvc.perform(post("/api/v1/posts/{id}/favorites", postId)
                        .header("Authorization", bearerToken(user1Token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("已经收藏过了"));
    }

    // ==================== 幂等取消 ====================

    @Test
    @DisplayName("取消点赞: 未点赞过 → 200 (幂等)")
    void unlikePost_NeverLiked_Idempotent() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/{id}/likes", postId)
                        .header("Authorization", bearerToken(user1Token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("取消关注: 未关注过 → 200 (幂等)")
    void unfollow_NeverFollowed_Idempotent() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}/follow", user2Id)
                        .header("Authorization", bearerToken(user1Token)))
                .andExpect(status().isOk());
    }
}
