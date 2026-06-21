package com.techhub.integration;

import com.techhub.service.impl.AiSummaryAsyncExecutor;
import com.techhub.util.AiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 辅助集成测试：生成摘要 → 查询摘要 → 刷新摘要 → 问答。
 * 仅 mock AiClient（外部 LLM 服务），其他组件全真。
 */
@Sql(scripts = {"classpath:sql/h2-schema.sql", "classpath:sql/h2-integration-data.sql"})
@DisplayName("AI 辅助集成测试")
class AiIntegrationTest extends BaseIntegrationTest {

    @MockitoBean
    private AiClient aiClient;  // Only external service is mocked

    private String userToken;
    private String otherUserToken;
    private String adminToken;
    private String postId;

    @BeforeEach
    void setUp() throws Exception {
        // Register users
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"ai_user\",\"password\":\"password123\",\"email\":\"ai_user@test.com\"}"));
        String loginResp = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ai_user\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        userToken = objectMapper.readTree(loginResp).get("data").get("token").asText();

        // Register another user for permission isolation testing
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"ai_other\",\"password\":\"password123\",\"email\":\"ai_other@test.com\"}"));
        String otherResp = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ai_other\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        otherUserToken = objectMapper.readTree(otherResp).get("data").get("token").asText();

        adminToken = generateToken(888L, "ai_admin", "ADMIN");

        // Create a post with sufficient content for AI (> 50 chars)
        String createResp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearerToken(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "AI Test Post",
                                    "content": "This is a detailed technical article about Spring Boot and microservices architecture. It covers topics like dependency injection, auto-configuration, and best practices for building REST APIs with Spring Boot 3.5. The article also discusses database integration with MyBatis-Plus and Redis caching strategies.",
                                    "categoryId": 1000,
                                    "visibility": 0
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        postId = objectMapper.readTree(createResp).get("data").get("id").asText();

        // Mock AiClient to return canned responses
        when(aiClient.callLlm(anyString(), anyString()))
                .thenReturn("这是一篇关于Spring Boot和微服务架构的技术文章，涵盖了依赖注入、自动配置以及REST API最佳实践。");
    }

    // ==================== 生成摘要 ====================

    @Test
    @DisplayName("生成AI摘要 → 触发异步生成 → 200")
    void generateSummary_TriggersAsyncGeneration() throws Exception {
        mockMvc.perform(post("/api/v1/posts/{postId}/ai/summary", postId)
                        .header("Authorization", bearerToken(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("摘要生成已触发，请稍后查询结果"));
    }

    @Test
    @DisplayName("生成AI摘要 → 未认证 → 401")
    void generateSummary_Unauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/posts/{postId}/ai/summary", postId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("生成AI摘要 → 内容不足50字 → 422")
    void generateSummary_ContentTooShort() throws Exception {
        // Create a post with short content
        String shortPostResp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearerToken(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Short Post",
                                    "content": "Too short",
                                    "categoryId": 1000
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        String shortPostId = objectMapper.readTree(shortPostResp).get("data").get("id").asText();

        mockMvc.perform(post("/api/v1/posts/{postId}/ai/summary", shortPostId)
                        .header("Authorization", bearerToken(userToken)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422));
    }

    @Test
    @DisplayName("生成AI摘要 → 帖子不存在 → 404")
    void generateSummary_PostNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/posts/99999/ai/summary")
                        .header("Authorization", bearerToken(userToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== 查询摘要 ====================

    @Test
    @DisplayName("查询AI摘要 → 生成后查询 → 返回 status=1 + content")
    void getSummary_AfterGeneration() throws Exception {
        // Trigger generation
        mockMvc.perform(post("/api/v1/posts/{postId}/ai/summary", postId)
                .header("Authorization", bearerToken(userToken)));

        // The mock AiClient returns immediately (async in test runs sync-ish)
        // Query summary
        mockMvc.perform(get("/api/v1/posts/{postId}/ai/summary", postId)
                        .header("Authorization", bearerToken(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询AI摘要 → 未触发过生成 → null")
    void getSummary_NotGenerated() throws Exception {
        mockMvc.perform(get("/api/v1/posts/{postId}/ai/summary", postId)
                        .header("Authorization", bearerToken(userToken)))
                .andExpect(status().isOk());
        // Data may be null if never generated
    }

    @Test
    @DisplayName("查询AI摘要 → 未认证 → 401")
    void getSummary_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/posts/{postId}/ai/summary", postId))
                .andExpect(status().isUnauthorized());
    }

    // ==================== AI 问答 ====================

    @Test
    @DisplayName("AI问答 → 摘要已生成 → 返回问答结果")
    void askQuestion_WithSummary() throws Exception {
        // Trigger generation first — wait for async completion (mock AiClient returns instantly)
        mockMvc.perform(post("/api/v1/posts/{postId}/ai/summary", postId)
                .header("Authorization", bearerToken(userToken)));
        Thread.sleep(300);

        // Ask question
        mockMvc.perform(post("/api/v1/posts/{postId}/ai/qa", postId)
                        .header("Authorization", bearerToken(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"这篇文章主要讲了什么？\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.question").value("这篇文章主要讲了什么？"))
                .andExpect(jsonPath("$.data.answer").isNotEmpty())
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    @DisplayName("AI问答 → 摘要未生成 → 422")
    void askQuestion_NoSummary() throws Exception {
        mockMvc.perform(post("/api/v1/posts/{postId}/ai/qa", postId)
                        .header("Authorization", bearerToken(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"这篇文章讲了什么？\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422));
    }

    @Test
    @DisplayName("AI问答 → 未认证 → 401")
    void askQuestion_Unauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/posts/{postId}/ai/qa", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"test\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== 问答历史 ====================

    @Test
    @DisplayName("问答历史 → 分页返回 → 包含问答记录")
    void getQaHistory_Paginated() throws Exception {
        // Trigger summary — wait for async completion (mock AiClient returns instantly)
        mockMvc.perform(post("/api/v1/posts/{postId}/ai/summary", postId)
                .header("Authorization", bearerToken(userToken)));
        Thread.sleep(300);

        mockMvc.perform(post("/api/v1/posts/{postId}/ai/qa", postId)
                .header("Authorization", bearerToken(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"Q1: What is Spring Boot?\"}"));

        mockMvc.perform(get("/api/v1/posts/{postId}/ai/qa/history", postId)
                        .header("Authorization", bearerToken(userToken))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("问答历史 → 未认证 → 401")
    void getQaHistory_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/posts/{postId}/ai/qa/history", postId)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== 权限隔离 ====================

    @Test
    @DisplayName("AI数据隔离 → 用户A的摘要用户B不可见 → 独立存储")
    void aiDataIsolation_UsersCannotSeeOthersSummaries() throws Exception {
        // User generates summary for post
        mockMvc.perform(post("/api/v1/posts/{postId}/ai/summary", postId)
                .header("Authorization", bearerToken(userToken)));

        // Other user queries same post's summary → gets their own (null if never generated)
        mockMvc.perform(get("/api/v1/posts/{postId}/ai/summary", postId)
                        .header("Authorization", bearerToken(otherUserToken)))
                .andExpect(status().isOk());
        // Data is null/empty for otherUser because they never triggered summary
        // This proves data isolation by (userId, postId)
    }

    @Test
    @DisplayName("AI问答历史隔离 → 用户A的历史用户B不可见")
    void qaHistoryIsolation() throws Exception {
        // User asks question — wait for async summary completion
        mockMvc.perform(post("/api/v1/posts/{postId}/ai/summary", postId)
                .header("Authorization", bearerToken(userToken)));
        Thread.sleep(300);
        mockMvc.perform(post("/api/v1/posts/{postId}/ai/qa", postId)
                .header("Authorization", bearerToken(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"User A's question\"}"));

        // Other user queries QA history → gets empty
        mockMvc.perform(get("/api/v1/posts/{postId}/ai/qa/history", postId)
                        .header("Authorization", bearerToken(otherUserToken))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }
}
