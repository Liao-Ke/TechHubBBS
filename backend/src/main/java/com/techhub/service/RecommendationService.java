package com.techhub.service;

import com.techhub.dto.recommendation.RecommendationVO;
import com.techhub.dto.recommendation.RelatedPostVO;

import java.util.List;

/**
 * 推荐服务 — 内容推荐 + 热门兜底 + 相似帖子。
 */
public interface RecommendationService {

    /**
     * 获取当前用户的个性化推荐（分页）。
     *
     * @param userId 当前用户 ID（null = 游客）
     * @param page   页码
     * @param size   每页数量
     * @return 推荐帖子列表
     */
    List<RecommendationVO> getRecommendations(Long userId, int page, int size);

    /**
     * 获取指定帖子的 Top-N 相似帖子。
     *
     * @param postId 帖子 ID
     * @param limit  返回数量上限
     * @return 相似帖子列表
     */
    List<RelatedPostVO> getRelatedPosts(Long postId, int limit);
}
