package com.techhub.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.techhub.entity.Post;
import com.techhub.enums.VisibilityEnum;
import com.techhub.mapper.PostMapper;
import com.techhub.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 热门帖子调度器 — 每 10 分钟刷新热门帖子缓存至 Redis。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotPostScheduler {

    private static final String REDIS_KEY_HOT_POSTS = "hot_posts:list";
    private static final String REDIS_KEY_HOT_POST_PREFIX = "hot_posts:";
    private static final int HOT_POST_CACHE_COUNT = 100;
    private static final long CACHE_TTL_MINUTES = 30;

    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 每 10 分钟刷新热门帖子缓存。
     */
    @Scheduled(fixedRate = 600_000)
    public void refreshHotPosts() {
        log.info("HotPostScheduler: 开始刷新热门帖子缓存...");

        try {
            // 1. 查询公开帖子
            List<Post> publicPosts = postMapper.selectList(
                    new LambdaQueryWrapper<Post>()
                            .eq(Post::getVisibility, VisibilityEnum.PUBLIC.getCode()));

            // 2. 按热度排序
            List<Post> hotPosts = publicPosts.stream()
                    .sorted(Comparator.comparingDouble(this::hotScore).reversed())
                    .limit(HOT_POST_CACHE_COUNT)
                    .collect(Collectors.toList());

            // 3. 缓存到 Redis
            // 存储有序列表（按热度排序的帖子 ID 列表）
            List<Long> hotPostIds = hotPosts.stream()
                    .map(Post::getId)
                    .collect(Collectors.toList());
            redisTemplate.opsForValue().set(REDIS_KEY_HOT_POSTS, hotPostIds, CACHE_TTL_MINUTES, TimeUnit.MINUTES);

            // 单独缓存每个热门帖子的基本信息
            for (Post post : hotPosts) {
                Map<String, Object> postInfo = new HashMap<>();
                postInfo.put("id", post.getId().toString());
                postInfo.put("title", post.getTitle());
                postInfo.put("likeCount", post.getLikeCount());
                postInfo.put("commentCount", post.getCommentCount());
                postInfo.put("viewCount", post.getViewCount());
                postInfo.put("hotScore", hotScore(post));

                var author = userMapper.selectById(post.getAuthorId());
                postInfo.put("authorName", author != null ? author.getUsername() : "未知");

                redisTemplate.opsForValue().set(
                        REDIS_KEY_HOT_POST_PREFIX + post.getId(),
                        postInfo,
                        CACHE_TTL_MINUTES,
                        TimeUnit.MINUTES);
            }

            log.info("HotPostScheduler: 热门帖子缓存刷新完成，共 {} 条", hotPosts.size());
        } catch (Exception e) {
            log.error("HotPostScheduler: 刷新热门帖子缓存失败", e);
        }
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
}
