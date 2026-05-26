package com.techhub.service;

import com.techhub.common.PageResult;
import com.techhub.dto.ai.AiQaResponse;
import com.techhub.dto.ai.AiSummaryResponse;

/**
 * AI 服务接口 — 帖子摘要生成与问答。
 */
public interface AiService {

    /**
     * 异步生成帖子 AI 摘要。
     *
     * @param userId 当前用户 ID
     * @param postId 帖子 ID
     */
    void generateSummary(Long userId, Long postId);

    /**
     * 查询 AI 摘要状态与内容。
     *
     * @param userId 当前用户 ID
     * @param postId 帖子 ID
     * @return 摘要响应
     */
    AiSummaryResponse getSummary(Long userId, Long postId);

    /**
     * AI 问答：基于帖子内容回答用户问题。
     *
     * @param userId   当前用户 ID
     * @param postId   帖子 ID
     * @param question 用户问题
     * @return 问答响应
     */
    AiQaResponse askQuestion(Long userId, Long postId, String question);

    /**
     * 分页查询 AI 问答历史记录。
     *
     * @param userId 当前用户 ID
     * @param postId 帖子 ID
     * @param page   页码
     * @param size   每页条数
     * @return 分页问答记录
     */
    PageResult<AiQaResponse> getQaHistory(Long userId, Long postId, int page, int size);
}
