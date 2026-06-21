package com.techhub.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 草稿集成测试：自动保存 → 恢复 → 发布后清理。
 */
@Sql(scripts = {"classpath:sql/h2-schema.sql", "classpath:sql/h2-integration-data.sql"})
@DisplayName("草稿集成测试")
@Transactional
class DraftIntegrationTest extends BaseIntegrationTest {

    private String userToken;
    private String otherUserToken;

    @BeforeEach
    void setUpUsers() throws Exception {
        // Register draft user
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"draft_user\",\"password\":\"password123\",\"email\":\"draft_user@test.com\"}"));
        String resp = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"draft_user\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        userToken = objectMapper.readTree(resp).get("data").get("token").asText();

        // Register other user
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"draft_other\",\"password\":\"password123\",\"email\":\"draft_other@test.com\"}"));
        String otherResp = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"draft_other\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        otherUserToken = objectMapper.readTree(otherResp).get("data").get("token").asText();
    }

    // ==================== 保存草稿 ====================

    @Test
    @DisplayName("保存草稿 → 新帖草稿 → 200 + 返回草稿数据")
    void saveDraft_NewPost_ReturnsDraft() throws Exception {
        assertOk(mockMvc.perform(post("/api/v1/drafts")
                        .header("Authorization", bearerToken(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Draft Title",
                                    "content": "Draft content still being written...",
                                    "categoryId": 1000,
                                    "visibility": 0
                                }
                                """)))
                .andExpect(jsonPath("$.data.title").value("Draft Title"))
                .andExpect(jsonPath("$.data.content").value("Draft content still being written..."))
                .andExpect(jsonPath("$.data.categoryId").value(1000))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.lastSavedAt").isNotEmpty());
    }

    @Test
    @DisplayName("保存草稿 → 编辑已有帖子草稿 → 更新而非新建")
    void saveDraft_ExistingPost_UpdatesDraft() throws Exception {
        // First save
        String firstResp = mockMvc.perform(post("/api/v1/drafts")
                        .header("Authorization", bearerToken(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "postId": 100,
                                    "title": "Editing Post",
                                    "content": "First version",
                                    "categoryId": 1001
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        String draftId = objectMapper.readTree(firstResp).get("data").get("id").asText();

        // Second save with same postId → should update
        assertOk(mockMvc.perform(post("/api/v1/drafts")
                        .header("Authorization", bearerToken(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "postId": 100,
                                    "title": "Editing Post V2",
                                    "content": "Second version with more content",
                                    "categoryId": 1001
                                }
                                """)))
                .andExpect(jsonPath("$.data.id").value(draftId))
                .andExpect(jsonPath("$.data.title").value("Editing Post V2"))
                .andExpect(jsonPath("$.data.content").value("Second version with more content"));
    }

    @Test
    @DisplayName("保存草稿 → 未认证 → 401")
    void saveDraft_Unauthenticated() throws Exception {
        assertUnauthorized(mockMvc.perform(post("/api/v1/drafts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test\",\"content\":\"test\"}")));
    }

    // ==================== 查询草稿列表 ====================

    @Test
    @DisplayName("查询草稿列表 → 返回用户所有草稿")
    void listDrafts_ReturnsUserDrafts() throws Exception {
        // Save multiple drafts with distinct postIds to avoid upsert collision
        mockMvc.perform(post("/api/v1/drafts")
                .header("Authorization", bearerToken(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"postId\":101,\"title\":\"Draft 1\",\"content\":\"Content 1\",\"categoryId\":1000}"));
        mockMvc.perform(post("/api/v1/drafts")
                .header("Authorization", bearerToken(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"postId\":102,\"title\":\"Draft 2\",\"content\":\"Content 2\",\"categoryId\":1000}"));

        assertOk(mockMvc.perform(get("/api/v1/drafts")
                        .header("Authorization", bearerToken(userToken))))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].title").value("Draft 2")) // latest first
                .andExpect(jsonPath("$.data[1].title").value("Draft 1"));
    }

    @Test
    @DisplayName("查询草稿列表 → 用户隔离 → 只能看到自己的")
    void listDrafts_UserIsolation() throws Exception {
        // User saves draft
        mockMvc.perform(post("/api/v1/drafts")
                .header("Authorization", bearerToken(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"My Draft\",\"content\":\"My content\",\"categoryId\":1000}"));

        // Other user sees empty
        assertOk(mockMvc.perform(get("/api/v1/drafts")
                .header("Authorization", bearerToken(otherUserToken))))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("查询草稿列表 → 未认证 → 401")
    void listDrafts_Unauthenticated() throws Exception {
        assertUnauthorized(mockMvc.perform(get("/api/v1/drafts")));
    }

    // ==================== 检查特定帖子草稿 ====================

    @Test
    @DisplayName("检查草稿 → 存在 → 返回草稿")
    void checkDraft_Exists() throws Exception {
        mockMvc.perform(post("/api/v1/drafts")
                .header("Authorization", bearerToken(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"postId\":200,\"title\":\"Edit Draft\",\"content\":\"Editing...\",\"categoryId\":1000}"));

        assertOk(mockMvc.perform(get("/api/v1/drafts/check")
                        .header("Authorization", bearerToken(userToken))
                        .param("postId", "200")))
                .andExpect(jsonPath("$.data.title").value("Edit Draft"));
    }

    @Test
    @DisplayName("检查草稿 → 不存在 → null")
    void checkDraft_NotExists() throws Exception {
        assertOk(mockMvc.perform(get("/api/v1/drafts/check")
                        .header("Authorization", bearerToken(userToken))
                        .param("postId", "99999")));
        // Returns R.ok(null) — empty data
    }

    // ==================== 删除草稿 ====================

    @Test
    @DisplayName("删除草稿 → 本人删除 → 200")
    void deleteDraft_AsOwner() throws Exception {
        String saveResp = mockMvc.perform(post("/api/v1/drafts")
                        .header("Authorization", bearerToken(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"To Delete\",\"content\":\"Will be deleted\",\"categoryId\":1000}"))
                .andReturn().getResponse().getContentAsString();
        String draftId = objectMapper.readTree(saveResp).get("data").get("id").asText();

        assertOk(mockMvc.perform(delete("/api/v1/drafts/{id}", draftId)
                        .header("Authorization", bearerToken(userToken))))
                .andExpect(jsonPath("$.message").value("删除成功"));

        // Verify deleted
        assertOk(mockMvc.perform(get("/api/v1/drafts")
                .header("Authorization", bearerToken(userToken))))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("删除草稿 → 非本人 → 403")
    void deleteDraft_NotOwner() throws Exception {
        String saveResp = mockMvc.perform(post("/api/v1/drafts")
                        .header("Authorization", bearerToken(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Not Yours\",\"content\":\"Leave it\",\"categoryId\":1000}"))
                .andReturn().getResponse().getContentAsString();
        String draftId = objectMapper.readTree(saveResp).get("data").get("id").asText();

        assertForbidden(mockMvc.perform(delete("/api/v1/drafts/{id}", draftId)
                .header("Authorization", bearerToken(otherUserToken))));
    }

    // ==================== 发布后草稿清理 ====================

    @Test
    @DisplayName("发布帖子 → 关联草稿被自动清理 → draftPostId 发布后删除草稿")
    void publishPost_CleansUpDraft() throws Exception {
        // First save a draft
        String saveResp = mockMvc.perform(post("/api/v1/drafts")
                        .header("Authorization", bearerToken(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Draft to Publish",
                                    "content": "This is the final version This is the final version This is the final version ",
                                    "categoryId": 1000,
                                    "visibility": 0
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        String draftId = objectMapper.readTree(saveResp).get("data").get("id").asText();

        // Publish post with draftPostId
        assertOk(mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearerToken(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                    "title": "Published from Draft",
                                    "content": "Final published content Final published content Final published content ",
                                    "categoryId": 1000,
                                    "visibility": 0,
                                    "draftPostId": %s
                                }
                                """, draftId))))
                .andExpect(jsonPath("$.data.title").value("Published from Draft"));

        // Verify draft was deleted
        assertOk(mockMvc.perform(get("/api/v1/drafts")
                .header("Authorization", bearerToken(userToken))))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
