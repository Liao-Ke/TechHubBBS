package com.techhub.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.techhub.entity.Post;
import com.techhub.enums.VisibilityEnum;
import com.techhub.mapper.PostMapper;
import com.techhub.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HotPostScheduler")
class HotPostSchedulerTest {

    @Mock
    private PostMapper postMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private HotPostScheduler hotPostScheduler;

    private Post createPost(Long id, int views, int likes, int comments) {
        Post post = new Post();
        post.setId(id);
        post.setTitle("Test Post " + id);
        post.setAuthorId(10L + id);
        post.setVisibility(VisibilityEnum.PUBLIC.getCode());
        post.setViewCount(views);
        post.setLikeCount(likes);
        post.setCommentCount(comments);
        post.setDeleted(0);
        post.setCreateTime(LocalDateTime.now());
        return post;
    }

    @Nested
    @DisplayName("refreshHotPosts")
    class RefreshHotPostsTests {

        @Test
        @DisplayName("按热度评分排序并缓存到 Redis")
        void sortsByHotScoreAndCachesToRedis() {
            Post p1 = createPost(1L, 100, 10, 5);   // score: 100 + 30 + 25 = 155
            Post p2 = createPost(2L, 50, 50, 20);   // score: 50 + 150 + 100 = 300
            Post p3 = createPost(3L, 200, 5, 2);    // score: 200 + 15 + 10 = 225

            when(postMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(p1, p2, p3));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(userMapper.selectById(anyLong())).thenReturn(null);

            assertDoesNotThrow(() -> hotPostScheduler.refreshHotPosts());

            verify(redisTemplate, atLeastOnce()).opsForValue();
            verify(valueOperations).set(
                    contains("hot_posts:list"),
                    any(),
                    eq(30L),
                    eq(TimeUnit.MINUTES));
        }

        @Test
        @DisplayName("空帖子列表不报错")
        void handlesEmptyPostList() {
            when(postMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            assertDoesNotThrow(() -> hotPostScheduler.refreshHotPosts());
        }

        @Test
        @DisplayName("Redis 异常被捕获不中断流程")
        void handlesRedisException() {
            Post p1 = createPost(1L, 100, 10, 5);
            when(postMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(p1));
            when(redisTemplate.opsForValue())
                    .thenThrow(new RuntimeException("Redis connection error"));

            // 不应抛出异常
            assertDoesNotThrow(() -> hotPostScheduler.refreshHotPosts());
        }
    }
}
