package com.techhub.controller;

import com.techhub.common.R;
import com.techhub.dto.recommendation.RecommendationVO;
import com.techhub.dto.recommendation.RelatedPostVO;
import com.techhub.security.SecurityUtils;
import com.techhub.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 推荐接口 — 个性化推荐 + 相似帖子 + 热门兜底。
 */
@Tag(name = "推荐", description = "个性化内容推荐与相似帖子")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "获取个性化推荐", description = "登录用户返回内容推荐，游客或新用户返回热门帖子")
    @GetMapping("/recommendations")
    public R<List<RecommendationVO>> getRecommendations(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<RecommendationVO> result = recommendationService.getRecommendations(userId, page, size);
        return R.ok(result);
    }

    @Operation(summary = "获取相似帖子", description = "返回与指定帖子内容相似度最高的帖子列表")
    @GetMapping("/posts/{id}/related")
    public R<List<RelatedPostVO>> getRelatedPosts(
            @Parameter(description = "帖子ID") @PathVariable Long id,
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "10") int limit) {
        List<RelatedPostVO> result = recommendationService.getRelatedPosts(id, limit);
        return R.ok(result);
    }
}
