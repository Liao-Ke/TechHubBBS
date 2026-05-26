package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.dto.recommendation.RecommendationVO;
import com.techhub.dto.recommendation.RelatedPostVO;
import com.techhub.entity.*;
import com.techhub.enums.VisibilityEnum;
import com.techhub.mapper.*;
import com.techhub.service.RecommendationService;
import com.techhub.service.PostVisibilityService;
import com.techhub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐服务实现 — 基于内容画像的个性化推荐 + 冷启动热门兜底 + 相似帖子。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final UserProfileMapper userProfileMapper;
    private final PostSimilarityMapper postSimilarityMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final PostVisibilityService postVisibilityService;
    private final ObjectMapper objectMapper;

    private static final int MAX_SIZE = 50;

    @Override
    public List<RecommendationVO> getRecommendations(Long userId, int page, int size) {
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
        if (page < 1) {
            page = 1;
        }

        // 1. 尝试获取用户画像
        Map<String, Double> userKeywords = getUserKeywords(userId);

        // 2. 冷启动：无画像 → 返回热门帖子
        if (userKeywords == null || userKeywords.isEmpty()) {
            return getHotRecommendations(page, size);
        }

        // 3. 基于内容画像推荐
        return getContentBasedRecommendations(userKeywords, userId, page, size);
    }

    @Override
    public List<RelatedPostVO> getRelatedPosts(Long postId, int limit) {
        if (limit <= 0 || limit > 20) {
            limit = 10;
        }

        List<PostSimilarity> similarities = postSimilarityMapper.selectTopSimilar(postId, limit);
        if (similarities.isEmpty()) {
            return Collections.emptyList();
        }

        boolean isAdmin = isAdmin();
        Long currentUserId = SecurityUtils.getCurrentUserId();
        boolean isLoggedIn = currentUserId != null;

        return similarities.stream()
                .map(s -> {
                    Long otherId = s.getPostIdA().equals(postId) ? s.getPostIdB() : s.getPostIdA();
                    Post otherPost = postMapper.selectById(otherId);
                    if (otherPost == null || otherPost.getDeleted() != null && otherPost.getDeleted() == 1) {
                        return null;
                    }
                    if (!postVisibilityService.isVisible(otherPost, currentUserId, isLoggedIn, isAdmin)) {
                        return null;
                    }
                    User author = userMapper.selectById(otherPost.getAuthorId());
                    return RelatedPostVO.builder()
                            .postId(otherPost.getId().toString())
                            .title(otherPost.getTitle())
                            .authorName(author != null ? author.getUsername() : "未知")
                            .similarityScore(round(s.getSimilarityScore(), 4))
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    /**
     * 从 user_profile 表中读取用户关键词权重。
     */
    private Map<String, Double> getUserKeywords(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            UserProfile profile = userProfileMapper.selectById(userId);
            if (profile == null || profile.getKeywordWeights() == null || profile.getKeywordWeights().isBlank()) {
                return null;
            }
            return objectMapper.readValue(profile.getKeywordWeights(),
                    new TypeReference<Map<String, Double>>() {});
        } catch (Exception e) {
            log.warn("RecommendationService: 读取用户 {} 画像失败", userId, e);
            return null;
        }
    }

    /**
     * 冷启动 — 按照 (viewCount * 1 + likeCount * 3 + commentCount * 5) 排序返回热门帖子。
     */
    private List<RecommendationVO> getHotRecommendations(int page, int size) {
        List<Post> allPosts = postMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getVisibility, VisibilityEnum.PUBLIC.getCode()));

        boolean isAdmin = isAdmin();
        Long currentUserId = SecurityUtils.getCurrentUserId();
        boolean isLoggedIn = currentUserId != null;

        int skip = (page - 1) * size;

        return allPosts.stream()
                .filter(p -> postVisibilityService.isVisible(p, currentUserId, isLoggedIn, isAdmin))
                .sorted(Comparator.comparingDouble(this::hotScore).reversed())
                .skip(skip)
                .limit(size)
                .map(p -> toRecommendationVO(p, null, "hot"))
                .collect(Collectors.toList());
    }

    /**
     * 内容推荐算法：
     * 1. 从相似度矩阵中找出与用户偏好的帖子最相似的帖子
     * 2. 用户关键词与帖子关键词直接做余弦相似度匹配
     */
    private List<RecommendationVO> getContentBasedRecommendations(
            Map<String, Double> userKeywords, Long userId, int page, int size) {

        boolean isAdmin = isAdmin();
        // 此处 userId 可能为 null（游客画像为空时走冷启动）
        boolean isLoggedIn = userId != null;

        // 找到所有公共帖子，计算与用户画像的相关度
        List<Post> allPosts = postMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getVisibility, VisibilityEnum.PUBLIC.getCode()));

        // 排除用户自己的帖子
        allPosts.removeIf(p -> userId != null && userId.equals(p.getAuthorId()));

        // 按关键词匹配度 + 热度综合排序
        Map<Long, Double> scoreMap = new HashMap<>();
        for (Post post : allPosts) {
            if (!postVisibilityService.isVisible(post, userId, isLoggedIn, isAdmin)) {
                continue;
            }
            double contentScore = computeContentScore(post, userKeywords);
            double hotBias = hotScore(post) * 0.01; // 热度微调
            scoreMap.put(post.getId(), contentScore + hotBias);
        }

        int skip = (page - 1) * size;

        List<Post> sortedPosts = allPosts.stream()
                .filter(p -> scoreMap.containsKey(p.getId()))
                .sorted((a, b) -> Double.compare(
                        scoreMap.getOrDefault(b.getId(), 0.0),
                        scoreMap.getOrDefault(a.getId(), 0.0)))
                .skip(skip)
                .limit(size)
                .collect(Collectors.toList());

        return sortedPosts.stream()
                .map(p -> toRecommendationVO(p, scoreMap.get(p.getId()), "content_based"))
                .collect(Collectors.toList());
    }

    /**
     * 计算帖子与用户画像的内容匹配度。
     * 简化实现：根据帖子标题和用户关键词做字符串匹配。
     */
    private double computeContentScore(Post post, Map<String, Double> userKeywords) {
        if (userKeywords.isEmpty()) {
            return 0.0;
        }
        String title = post.getTitle() != null ? post.getTitle().toLowerCase() : "";
        double score = 0.0;
        for (Map.Entry<String, Double> entry : userKeywords.entrySet()) {
            if (title.contains(entry.getKey())) {
                score += entry.getValue();
            }
        }
        return score;
    }

    /**
     * 热度评分公式：viewCount * 1 + likeCount * 3 + commentCount * 5。
     */
    private double hotScore(Post post) {
        int views = post.getViewCount() != null ? post.getViewCount() : 0;
        int likes = post.getLikeCount() != null ? post.getLikeCount() : 0;
        int comments = post.getCommentCount() != null ? post.getCommentCount() : 0;
        return views + likes * 3.0 + comments * 5.0;
    }

    /**
     * 将 Post 实体转为 RecommendationVO。
     */
    private RecommendationVO toRecommendationVO(Post post, Double similarityScore, String reason) {
        User author = userMapper.selectById(post.getAuthorId());
        return RecommendationVO.builder()
                .postId(post.getId().toString())
                .title(post.getTitle())
                .authorName(author != null ? author.getUsername() : "未知")
                .authorAvatar(author != null ? author.getAvatarUrl() : null)
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .similarityScore(similarityScore != null ? round(similarityScore, 4) : null)
                .reason(reason)
                .build();
    }

    /**
     * 判断当前用户是否为管理员。
     */
    private boolean isAdmin() {
        String role = SecurityUtils.getCurrentRole();
        return "ADMIN".equals(role);
    }

    /**
     * 四舍五入保留小数位。
     */
    private double round(double value, int places) {
        double factor = Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }
}
