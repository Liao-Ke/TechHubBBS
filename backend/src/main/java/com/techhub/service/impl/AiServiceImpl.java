package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.ResultCode;
import com.techhub.dto.ai.AiQaResponse;
import com.techhub.dto.ai.AiSummaryResponse;
import com.techhub.entity.AiQaHistory;
import com.techhub.entity.AiSummary;
import com.techhub.entity.Follow;
import com.techhub.entity.Post;
import com.techhub.enums.VisibilityEnum;
import com.techhub.mapper.AiQaHistoryMapper;
import com.techhub.mapper.AiSummaryMapper;
import com.techhub.mapper.FollowMapper;
import com.techhub.mapper.PostMapper;
import com.techhub.service.AiService;
import com.techhub.util.AiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 服务实现 — 帖子摘要生成、问答与历史管理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private static final int MIN_CONTENT_LENGTH = 50;
    private static final int MAX_QUESTION_LENGTH = 500;
    private static final int MAX_QA_PAGE_SIZE = 20;

    private static final String QA_SYSTEM_PROMPT =
            "你是一个技术问答助手。请基于下面提供的帖子原文内容回答用户的问题。"
                    + "只能根据原文内容回答，不要编造信息。如果原文中没有相关信息，请明确告知用户。";

    private final PostMapper postMapper;
    private final AiSummaryMapper aiSummaryMapper;
    private final AiQaHistoryMapper aiQaHistoryMapper;
    private final AiClient aiClient;
    private final FollowMapper followMapper;
    private final AiSummaryAsyncExecutor asyncExecutor;

    @Override
    @Transactional
    public void generateSummary(Long userId, Long postId) {
        // 1. 获取帖子并校验内容长度
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        if (post.getContent() == null || post.getContent().trim().length() < MIN_CONTENT_LENGTH) {
            throw new BusinessException(ResultCode.UNPROCESSABLE,
                    "帖子内容不足" + MIN_CONTENT_LENGTH + "字，无法生成摘要");
        }

        if (post.getVisibility() != null && post.getVisibility() != VisibilityEnum.PUBLIC.getCode()) {
            if (post.getVisibility() == VisibilityEnum.PRIVATE.getCode()) {
                throw new BusinessException(ResultCode.FORBIDDEN, "私密帖子不支持AI摘要");
            }
            if (post.getVisibility() == VisibilityEnum.FOLLOWERS_ONLY.getCode()) {
                boolean isFollowing = followMapper.selectCount(
                        new LambdaQueryWrapper<Follow>()
                                .eq(Follow::getFollowerId, userId)
                                .eq(Follow::getFolloweeId, post.getAuthorId())
                ) > 0;
                if (!isFollowing) {
                    throw new BusinessException(ResultCode.FORBIDDEN, "仅关注者可生成此帖子的AI摘要");
                }
            }
        }

        // 2. 查找或创建摘要记录，状态设为"生成中"
        AiSummary summary = aiSummaryMapper.selectOne(
                new LambdaQueryWrapper<AiSummary>()
                        .eq(AiSummary::getUserId, userId)
                        .eq(AiSummary::getPostId, postId)
        );

        Long summaryId;
        if (summary != null) {
            summary.setStatus(0);
            summary.setContent(null);
            summary.setErrorMessage(null);
            aiSummaryMapper.updateById(summary);
            summaryId = summary.getId();
        } else {
            AiSummary newSummary = new AiSummary();
            newSummary.setUserId(userId);
            newSummary.setPostId(postId);
            newSummary.setStatus(0);
            aiSummaryMapper.insert(newSummary);
            summaryId = newSummary.getId();
        }

        // 3. 异步调用 LLM（通过独立组件避免 @Async 自调用失效）
        asyncExecutor.execute(summaryId, userId, postId, post.getContent());
    }

    @Override
    public AiSummaryResponse getSummary(Long userId, Long postId) {
        AiSummary summary = aiSummaryMapper.selectOne(
                new LambdaQueryWrapper<AiSummary>()
                        .eq(AiSummary::getUserId, userId)
                        .eq(AiSummary::getPostId, postId)
        );
        return toSummaryResponse(summary);
    }

    @Override
    @Transactional
    public AiQaResponse askQuestion(Long userId, Long postId, String question) {
        // 1. 校验问题
        String safeQuestion = AiClient.sanitizeUserInput(question, MAX_QUESTION_LENGTH);
        if (safeQuestion.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "问题不能为空");
        }

        // 2. 检查摘要是否已生成
        AiSummary summary = aiSummaryMapper.selectOne(
                new LambdaQueryWrapper<AiSummary>()
                        .eq(AiSummary::getUserId, userId)
                        .eq(AiSummary::getPostId, postId)
        );
        if (summary == null) {
            throw new BusinessException(ResultCode.UNPROCESSABLE, "AI摘要暂未生成，请先生成摘要");
        }
        if (summary.getStatus() == 0) {
            throw new BusinessException(ResultCode.UNPROCESSABLE, "AI摘要正在生成中，请稍后再试");
        }
        if (summary.getStatus() == 2) {
            throw new BusinessException(ResultCode.UNPROCESSABLE, "AI摘要生成失败，请重新生成");
        }
        if (summary.getStatus() != 1) {
            throw new BusinessException(ResultCode.UNPROCESSABLE, "AI摘要状态异常，请重新生成");
        }

        // 3. 获取帖子内容构建 prompt
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        String userPrompt = "帖子原文：\n" + truncateContent(post.getContent(), 4000)
                + "\n\n用户问题：" + safeQuestion;

        // 4. 调用 LLM
        String answer = aiClient.callLlm(QA_SYSTEM_PROMPT, userPrompt);
        if (answer == null) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "AI 服务暂不可用，请稍后重试");
        }

        // 5. 保存问答记录
        AiQaHistory history = new AiQaHistory();
        history.setUserId(userId);
        history.setPostId(postId);
        history.setQuestion(safeQuestion);
        history.setAnswer(answer);
        aiQaHistoryMapper.insert(history);

        // 6. 返回响应
        AiQaResponse resp = new AiQaResponse();
        resp.setId(history.getId().toString());
        resp.setQuestion(safeQuestion);
        resp.setAnswer(answer);
        resp.setCreateTime(history.getCreateTime());
        return resp;
    }

    @Override
    public PageResult<AiQaResponse> getQaHistory(Long userId, Long postId, int page, int size) {
        int safeSize = Math.min(size, MAX_QA_PAGE_SIZE);
        Page<AiQaHistory> mpPage = new Page<>(page, safeSize);
        LambdaQueryWrapper<AiQaHistory> wrapper = new LambdaQueryWrapper<AiQaHistory>()
                .eq(AiQaHistory::getUserId, userId)
                .eq(AiQaHistory::getPostId, postId)
                .orderByDesc(AiQaHistory::getCreateTime);

        Page<AiQaHistory> result = aiQaHistoryMapper.selectPage(mpPage, wrapper);
        List<AiQaResponse> records = result.getRecords().stream()
                .map(this::toQaResponse)
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getSize(), result.getCurrent());
    }

    // ==================== 私有方法 ====================

    private AiSummaryResponse toSummaryResponse(AiSummary summary) {
        if (summary == null) {
            return null;
        }
        AiSummaryResponse resp = new AiSummaryResponse();
        resp.setId(summary.getId().toString());
        resp.setPostId(summary.getPostId() != null ? summary.getPostId().toString() : null);
        resp.setContent(summary.getContent());
        resp.setStatus(summary.getStatus());
        resp.setErrorMessage(summary.getErrorMessage());
        resp.setCreateTime(summary.getCreateTime());
        resp.setUpdateTime(summary.getUpdateTime());
        return resp;
    }

    private AiQaResponse toQaResponse(AiQaHistory history) {
        AiQaResponse resp = new AiQaResponse();
        resp.setId(history.getId().toString());
        resp.setQuestion(history.getQuestion());
        resp.setAnswer(history.getAnswer());
        resp.setCreateTime(history.getCreateTime());
        return resp;
    }

    /**
     * 截断内容到指定长度，避免 token 超限。
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        return content.length() > maxLength ? content.substring(0, maxLength) : content;
    }
}
