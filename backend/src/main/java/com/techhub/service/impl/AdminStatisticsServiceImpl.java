package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.techhub.entity.Comment;
import com.techhub.entity.Post;
import com.techhub.entity.User;
import com.techhub.mapper.CommentMapper;
import com.techhub.mapper.PostMapper;
import com.techhub.mapper.UserMapper;
import com.techhub.service.AdminStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 管理后台统计服务实现 — Redis 缓存 5 分钟。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStatisticsServiceImpl implements AdminStatisticsService {

    private static final String STATS_CACHE_KEY = "admin:statistics";
    private static final long CACHE_TTL_MINUTES = 5;

    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getStatistics() {
        // 尝试从 Redis 读取缓存
        try {
            Object cached = redisTemplate.opsForValue().get(STATS_CACHE_KEY);
            if (cached instanceof Map) {
                log.debug("从 Redis 缓存读取统计数据");
                return (Map<String, Object>) cached;
            }
        } catch (Exception e) {
            log.warn("Redis 读取缓存失败，直接查询数据库: {}", e.getMessage());
        }

        // 缓存未命中，查询数据库
        Map<String, Object> stats = computeStatistics();

        // 写入 Redis 缓存
        try {
            redisTemplate.opsForValue().set(STATS_CACHE_KEY, stats, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("统计数据已写入 Redis 缓存, TTL={}分钟", CACHE_TTL_MINUTES);
        } catch (Exception e) {
            log.warn("Redis 写入缓存失败: {}", e.getMessage());
        }

        return stats;
    }

    private Map<String, Object> computeStatistics() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        long userCount = userMapper.selectCount(null);
        long postCount = postMapper.selectCount(null);
        long commentCount = commentMapper.selectCount(null);

        long todayNewUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getCreateTime, todayStart));
        long todayNewPosts = postMapper.selectCount(
                new LambdaQueryWrapper<Post>().ge(Post::getCreateTime, todayStart));

        // 今日活跃用户：发帖或评论的 distinct userId
        List<Long> activePostUsers = postMapper.selectObjs(
                new LambdaQueryWrapper<Post>()
                        .select(Post::getAuthorId)
                        .ge(Post::getCreateTime, todayStart)
        ).stream().map(obj -> (Long) obj).collect(Collectors.toList());

        List<Long> activeCommentUsers = commentMapper.selectObjs(
                new LambdaQueryWrapper<Comment>()
                        .select(Comment::getUserId)
                        .ge(Comment::getCreateTime, todayStart)
        ).stream().map(obj -> (Long) obj).collect(Collectors.toList());

        Set<Long> activeUserIds = new java.util.HashSet<>(activePostUsers);
        activeUserIds.addAll(activeCommentUsers);

        // 保持字段顺序
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("userCount", userCount);
        stats.put("postCount", postCount);
        stats.put("commentCount", commentCount);
        stats.put("todayNewUsers", todayNewUsers);
        stats.put("todayNewPosts", todayNewPosts);
        stats.put("activeUsersToday", (long) activeUserIds.size());

        return stats;
    }
}
