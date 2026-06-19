package com.techhub.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.config.AiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * AI LLM 客户端
 * 以 OpenAI 兼容格式调用大模型 API，支持超时重试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiClient {

    private final RestTemplate aiRestTemplate;
    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 调用 LLM，带 2 次重试。
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return LLM 返回文本，失败返回 null
     */
    public String callLlm(String systemPrompt, String userPrompt) {
        // 入口前置校验：apiKey / apiUrl 为空时直接返回 null，避免浪费 HTTP 请求
        if (aiConfig.getApiKey() == null || aiConfig.getApiKey().isBlank()) {
            log.error("AI_API_KEY 未配置，无法调用 LLM");
            return null;
        }
        if (aiConfig.getApiUrl() == null || aiConfig.getApiUrl().isBlank()) {
            log.error("AI_API_URL 未配置，无法调用 LLM");
            return null;
        }

        int maxAttempts = aiConfig.getMaxRetries() + 1;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return doCall(systemPrompt, userPrompt);
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    log.error("LLM 调用失败(已重试{}次): {}", aiConfig.getMaxRetries(), e.getMessage());
                    return null;
                }
                log.warn("LLM 调用失败, 第{}次重试: {}", attempt, e.getMessage());
            }
        }
        return null;
    }

    private String doCall(String systemPrompt, String userPrompt) {
        String url = aiConfig.getApiUrl();
        if (!url.endsWith("/")) {
            url += "/";
        }
        url += "chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiConfig.getApiKey());

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );

        Map<String, Object> body = Map.of(
                "model", aiConfig.getModel(),
                "messages", messages,
                "temperature", 0.7,
                "max_tokens", 1000
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = aiRestTemplate.postForEntity(url, entity, String.class);
            if (response.getBody() == null) {
                log.error("LLM 返回空响应体");
                return null;
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                log.error("LLM 响应中无 choices: {}", response.getBody());
                return null;
            }
            JsonNode message = choices.get(0).get("message");
            if (message == null) {
                log.error("LLM 响应中无 message");
                return null;
            }
            JsonNode content = message.get("content");
            if (content == null) {
                log.error("LLM 响应中无 content");
                return null;
            }
            return content.asText().trim();
        } catch (RestClientException e) {
            log.error("LLM HTTP 请求异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("LLM 响应解析异常: {}", e.getMessage());
            throw new RuntimeException("LLM 响应解析失败", e);
        }
    }

    /**
     * 防止 prompt 注入：截断过长用户输入并转义特殊字符。
     *
     * @param input 原始用户输入
     * @param maxLength 最大允许长度
     * @return 处理后的安全文本
     */
    public static String sanitizeUserInput(String input, int maxLength) {
        if (input == null) {
            return "";
        }
        String sanitized = input.trim();
        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength);
        }
        // 移除可能用于 prompt 注入的标记符号
        sanitized = sanitized.replace("```", "")
                .replace("---", "")
                .replace("===", "");
        return sanitized;
    }
}
