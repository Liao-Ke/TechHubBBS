package com.techhub.scheduler;

import com.techhub.service.PostKeywordService;
import com.techhub.service.UserProfileService;
import com.techhub.entity.Post;
import com.techhub.mapper.PostMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 推荐模型调度器 — 每 30 分钟更新用户画像 + 相似度矩阵。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationScheduler {

    private final UserProfileService userProfileService;
    private final PostKeywordService postKeywordService;
    private final PostMapper postMapper;

    /**
     * 每 30 分钟刷新推荐模型（用户画像 + 帖子相似度矩阵）。
     */
    @Scheduled(fixedRate = 1_800_000)
    public void refreshRecommendationModels() {
        log.info("RecommendationScheduler: 开始刷新推荐模型...");

        try {
            // 1. 更新所有帖子的关键词与相似度
            var posts = postMapper.selectList(new LambdaQueryWrapper<Post>()
                    .eq(Post::getDeleted, 0));
            log.info("RecommendationScheduler: 发现 {} 篇帖子，开始更新相似度矩阵", posts.size());
            for (Post post : posts) {
                try {
                    postKeywordService.updatePostKeywords(post.getId(), post.getTitle(), post.getContent());
                } catch (Exception e) {
                    log.error("RecommendationScheduler: 更新帖子 {} 关键词失败", post.getId(), e);
                }
            }

            // 2. 批量更新所有用户画像
            userProfileService.buildAllProfiles();

            log.info("RecommendationScheduler: 推荐模型刷新完成");
        } catch (Exception e) {
            log.error("RecommendationScheduler: 刷新推荐模型失败", e);
        }
    }
}
