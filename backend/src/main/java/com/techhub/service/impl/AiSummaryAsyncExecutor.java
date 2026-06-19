package com.techhub.service.impl;

import com.techhub.entity.AiSummary;
import com.techhub.mapper.AiSummaryMapper;
import com.techhub.util.AiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * AI 摘要异步生成执行器 — 独立组件以支持 {@link Async @Async} 代理正确拦截。
 * <p>
 * 将 {@code @Async} 方法与调用方分离，避免同一类内自调用导致 AOP 代理失效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiSummaryAsyncExecutor {

    private static final String SUMMARY_SYSTEM_PROMPT =
            "你是一个专业的技术内容摘要助手。请用简洁的中文总结以下技术帖子内容，"
                    + "提取核心观点和关键信息。摘要应控制在200字以内。";

    private final AiClient aiClient;
    private final AiSummaryMapper aiSummaryMapper;

    /**
     * 异步执行 AI 摘要生成并持久化结果。
     *
     * @param summaryId   摘要记录 ID
     * @param userId      用户 ID（仅记录）
     * @param postId      帖子 ID（仅记录）
     * @param postContent 帖子正文（将截断至 4000 字符）
     */
    @Async
    public void execute(Long summaryId, Long userId, Long postId, String postContent) {
        log.info("开始异步生成摘要: summaryId={}, userId={}, postId={}", summaryId, userId, postId);
        try {
            String result = aiClient.callLlm(SUMMARY_SYSTEM_PROMPT, truncateContent(postContent, 4000));
            AiSummary summary = new AiSummary();
            summary.setId(summaryId);
            if (result != null && !result.isBlank()) {
                summary.setStatus(1);
                summary.setContent(result);
                log.info("摘要生成成功: summaryId={}", summaryId);
            } else {
                summary.setStatus(2);
                summary.setErrorMessage("LLM 返回为空");
                log.warn("摘要生成失败(空响应): summaryId={}", summaryId);
            }
            aiSummaryMapper.updateById(summary);
        } catch (Exception e) {
            log.error("摘要生成异常: summaryId={}", summaryId, e);
            AiSummary summary = new AiSummary();
            summary.setId(summaryId);
            summary.setStatus(2);
            summary.setErrorMessage("生成失败: " + e.getMessage());
            aiSummaryMapper.updateById(summary);
        }
    }

    /**
     * 截断内容到指定长度，避免 token 超限。
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        return content.length() > maxLength ? content.substring(0, maxLength) : content;
    }
}
