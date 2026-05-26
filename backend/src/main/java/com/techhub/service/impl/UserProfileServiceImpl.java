package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.entity.*;
import com.techhub.mapper.*;
import com.techhub.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户画像服务实现 — 基于交互行为聚合关键词权重。
 * <p>
 * 权重策略:
 * <ul>
 *   <li>点赞帖子关键词 × 3</li>
 *   <li>收藏帖子关键词 × 4</li>
 *   <li>评论帖子关键词 × 5</li>
 *   <li>浏览（通过发帖体现）关键词 × 1</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileMapper userProfileMapper;
    private final UserLikeMapper userLikeMapper;
    private final FavoriteMapper favoriteMapper;
    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final PostKeywordMapper postKeywordMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Async("recommendationExecutor")
    @Transactional
    public void buildProfile(Long userId) {
        log.info("UserProfileService: 开始构建用户 {} 的兴趣画像", userId);

        Map<String, Double> keywordWeights = new HashMap<>();

        // 1. 点赞的帖子关键词（权重 × 3）
        List<UserLike> likedPosts = userLikeMapper.selectList(
                new LambdaQueryWrapper<UserLike>()
                        .eq(UserLike::getUserId, userId)
                        .eq(UserLike::getTargetType, "POST"));
        for (UserLike like : likedPosts) {
            accumulateKeywords(keywordWeights, like.getTargetId(), 3.0);
        }

        // 2. 收藏的帖子关键词（权重 × 4）
        List<Favorite> favorites = favoriteMapper.selectList(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId));
        for (Favorite fav : favorites) {
            accumulateKeywords(keywordWeights, fav.getPostId(), 4.0);
        }

        // 3. 评论过的帖子关键词（权重 × 5）
        List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getUserId, userId));
        for (Comment comment : comments) {
            accumulateKeywords(keywordWeights, comment.getPostId(), 5.0);
        }

        // 4. 自己发布的帖子关键词（权重 × 1）
        List<Post> ownPosts = postMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getAuthorId, userId));
        for (Post post : ownPosts) {
            accumulateKeywords(keywordWeights, post.getId(), 1.0);
        }

        // 归一化权重到 [0, 1] 区间
        normalizeWeights(keywordWeights);

        // 保存到 user_profile 表
        try {
            String json = objectMapper.writeValueAsString(keywordWeights);
            UserProfile profile = userProfileMapper.selectById(userId);
            if (profile == null) {
                profile = new UserProfile();
                profile.setUserId(userId);
                profile.setKeywordWeights(json);
                profile.setLastUpdateTime(LocalDateTime.now());
                userProfileMapper.insert(profile);
            } else {
                profile.setKeywordWeights(json);
                profile.setLastUpdateTime(LocalDateTime.now());
                userProfileMapper.updateById(profile);
            }
            log.info("UserProfileService: 用户 {} 画像已保存，含 {} 个关键词", userId, keywordWeights.size());
        } catch (JsonProcessingException e) {
            log.error("UserProfileService: 用户 {} 画像 JSON 序列化失败", userId, e);
        }
    }

    @Override
    @Async("recommendationExecutor")
    @Transactional
    public void buildAllProfiles() {
        log.info("UserProfileService: 开始批量构建所有用户画像");
        List<User> users = userMapper.selectList(null);
        int count = 0;
        for (User user : users) {
            try {
                buildProfile(user.getId());
                count++;
            } catch (Exception e) {
                log.error("UserProfileService: 构建用户 {} 画像失败", user.getId(), e);
            }
        }
        log.info("UserProfileService: 批量构建完成，成功 {} / {}", count, users.size());
    }

    /**
     * 累计指定帖子的关键词权重（乘以倍率）。
     */
    private void accumulateKeywords(Map<String, Double> keywordWeights, Long postId, double multiplier) {
        List<PostKeyword> keywords = postKeywordMapper.selectByPostId(postId);
        for (PostKeyword pk : keywords) {
            double weight = pk.getTfidfWeight() != null ? pk.getTfidfWeight() * multiplier : multiplier;
            keywordWeights.merge(pk.getKeyword(), weight, Double::sum);
        }
    }

    /**
     * 将权重 Map 归一化到 [0, 1]。
     */
    private void normalizeWeights(Map<String, Double> keywordWeights) {
        if (keywordWeights.isEmpty()) {
            return;
        }
        double max = keywordWeights.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (max == 0) max = 1.0;
        for (Map.Entry<String, Double> entry : keywordWeights.entrySet()) {
            entry.setValue(entry.getValue() / max);
        }
    }
}
