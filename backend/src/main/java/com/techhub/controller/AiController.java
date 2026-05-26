package com.techhub.controller;

import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.R;
import com.techhub.common.ResultCode;
import com.techhub.dto.ai.AiQaRequest;
import com.techhub.dto.ai.AiQaResponse;
import com.techhub.dto.ai.AiSummaryResponse;
import com.techhub.security.SecurityUtils;
import com.techhub.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AI 控制器 — 帖子摘要生成、问答与历史查询。
 * 所有接口需要登录，数据按 (userId, postId) 隔离。
 */
@RestController
@RequestMapping("/api/v1/posts/{postId}/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * 触发 AI 摘要生成（异步）。
     * status: 1=成功, 0=生成中, 2=失败
     */
    @PostMapping("/summary")
    public R<Void> generateSummary(@PathVariable Long postId) {
        Long userId = requireAuth();
        aiService.generateSummary(userId, postId);
        return R.ok("摘要生成已触发，请稍后查询结果");
    }

    /**
     * 查询 AI 摘要状态与内容。
     */
    @GetMapping("/summary")
    public R<AiSummaryResponse> getSummary(@PathVariable Long postId) {
        Long userId = requireAuth();
        AiSummaryResponse resp = aiService.getSummary(userId, postId);
        return R.ok(resp);
    }

    /**
     * AI 问答：基于当前用户已生成的摘要进行提问。
     */
    @PostMapping("/qa")
    public R<AiQaResponse> askQuestion(@PathVariable Long postId,
                                       @Valid @RequestBody AiQaRequest request) {
        Long userId = requireAuth();
        AiQaResponse resp = aiService.askQuestion(userId, postId, request.getQuestion());
        return R.ok(resp);
    }

    /**
     * 分页查询 AI 问答历史。
     */
    @GetMapping("/qa/history")
    public R<PageResult<AiQaResponse>> getQaHistory(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = requireAuth();
        return R.ok(aiService.getQaHistory(userId, postId, page, size));
    }

    private Long requireAuth() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
