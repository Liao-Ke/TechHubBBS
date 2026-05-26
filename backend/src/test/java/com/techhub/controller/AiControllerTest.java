package com.techhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.ResultCode;
import com.techhub.dto.ai.AiQaRequest;
import com.techhub.dto.ai.AiQaResponse;
import com.techhub.dto.ai.AiSummaryResponse;
import com.techhub.security.JwtTokenProvider;
import com.techhub.service.AiService;
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AiController 单元测试")
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AiService aiService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    void setUp() {
        authenticateUser();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );
    }

    // ==================== POST /api/v1/posts/{postId}/ai/summary ====================

    @Test
    @DisplayName("generateSummary — 成功触发摘要生成")
    void generateSummary_Success() throws Exception {
        doNothing().when(aiService).generateSummary(eq(1L), eq(10L));

        mockMvc.perform(post("/api/v1/posts/10/ai/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("摘要生成已触发，请稍后查询结果"));
    }

    @Test
    @DisplayName("generateSummary — 未认证返回 401")
    void generateSummary_Unauthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(post("/api/v1/posts/10/ai/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("generateSummary — 帖子不存在返回 404")
    void generateSummary_NotFound() throws Exception {
        doThrow(new BusinessException(ResultCode.NOT_FOUND, "帖子不存在"))
                .when(aiService).generateSummary(eq(1L), eq(999L));

        mockMvc.perform(post("/api/v1/posts/999/ai/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("帖子不存在"));
    }

    @Test
    @DisplayName("generateSummary — 内容不足50字返回 422")
    void generateSummary_ContentTooShort() throws Exception {
        doThrow(new BusinessException(ResultCode.UNPROCESSABLE, "帖子内容不足50字，无法生成摘要"))
                .when(aiService).generateSummary(eq(1L), eq(10L));

        mockMvc.perform(post("/api/v1/posts/10/ai/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("帖子内容不足50字，无法生成摘要"));
    }

    // ==================== GET /api/v1/posts/{postId}/ai/summary ====================

    @Test
    @DisplayName("getSummary — 返回摘要成功状态")
    void getSummary_Success() throws Exception {
        AiSummaryResponse summary = new AiSummaryResponse();
        summary.setId("1");
        summary.setStatus(1);
        summary.setContent("这是一篇关于Spring Boot的技术文章摘要");
        summary.setCreateTime(LocalDateTime.now());

        when(aiService.getSummary(eq(1L), eq(10L))).thenReturn(summary);

        mockMvc.perform(get("/api/v1/posts/10/ai/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.content").value("这是一篇关于Spring Boot的技术文章摘要"));
    }

    @Test
    @DisplayName("getSummary — 摘要生成中(status=0)")
    void getSummary_Generating() throws Exception {
        AiSummaryResponse summary = new AiSummaryResponse();
        summary.setId("1");
        summary.setStatus(0);
        summary.setContent(null);

        when(aiService.getSummary(eq(1L), eq(10L))).thenReturn(summary);

        mockMvc.perform(get("/api/v1/posts/10/ai/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(0));
    }

    @Test
    @DisplayName("getSummary — 摘要生成失败(status=2)")
    void getSummary_Failed() throws Exception {
        AiSummaryResponse summary = new AiSummaryResponse();
        summary.setId("1");
        summary.setStatus(2);
        summary.setErrorMessage("LLM 返回为空");

        when(aiService.getSummary(eq(1L), eq(10L))).thenReturn(summary);

        mockMvc.perform(get("/api/v1/posts/10/ai/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(2))
                .andExpect(jsonPath("$.data.errorMessage").value("LLM 返回为空"));
    }

    @Test
    @DisplayName("getSummary — 未认证返回 401")
    void getSummary_Unauthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/v1/posts/10/ai/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ==================== POST /api/v1/posts/{postId}/ai/qa ====================

    @Test
    @DisplayName("askQuestion — 问答成功返回")
    void askQuestion_Success() throws Exception {
        AiQaRequest request = new AiQaRequest();
        request.setQuestion("这篇文章讲了什么？");

        AiQaResponse response = new AiQaResponse();
        response.setId("100");
        response.setQuestion("这篇文章讲了什么？");
        response.setAnswer("这篇文章主要介绍了Spring Boot的核心概念。");
        response.setCreateTime(LocalDateTime.now());

        when(aiService.askQuestion(eq(1L), eq(10L), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/v1/posts/10/ai/qa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("100"))
                .andExpect(jsonPath("$.data.answer").value("这篇文章主要介绍了Spring Boot的核心概念。"));
    }

    @Test
    @DisplayName("askQuestion — 问题为空返回 400(validation)")
    void askQuestion_EmptyQuestion() throws Exception {
        AiQaRequest request = new AiQaRequest();
        request.setQuestion("");

        mockMvc.perform(post("/api/v1/posts/10/ai/qa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("askQuestion — 未认证返回 401")
    void askQuestion_Unauthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        AiQaRequest request = new AiQaRequest();
        request.setQuestion("test");

        mockMvc.perform(post("/api/v1/posts/10/ai/qa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("askQuestion — 摘要未生成返回 422")
    void askQuestion_NoSummary() throws Exception {
        AiQaRequest request = new AiQaRequest();
        request.setQuestion("这篇文章讲了什么？");

        doThrow(new BusinessException(ResultCode.UNPROCESSABLE, "请先生成AI摘要"))
                .when(aiService).askQuestion(eq(1L), eq(10L), anyString());

        mockMvc.perform(post("/api/v1/posts/10/ai/qa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("请先生成AI摘要"));
    }

    // ==================== GET /api/v1/posts/{postId}/ai/qa/history ====================

    @Test
    @DisplayName("getQaHistory — 分页返回问答历史")
    void getQaHistory_Success() throws Exception {
        AiQaResponse qa = new AiQaResponse();
        qa.setId("1");
        qa.setQuestion("这篇文章讲了什么？");
        qa.setAnswer("介绍了Spring Boot。");
        qa.setCreateTime(LocalDateTime.now());

        PageResult<AiQaResponse> pageResult = PageResult.of(List.of(qa), 1, 20, 1);

        when(aiService.getQaHistory(eq(1L), eq(10L), anyInt(), anyInt()))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/posts/10/ai/qa/history")
                        .param("page", "1")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].question").value("这篇文章讲了什么？"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("getQaHistory — 未认证返回 401")
    void getQaHistory_Unauthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/v1/posts/10/ai/qa/history")
                        .param("page", "1")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
