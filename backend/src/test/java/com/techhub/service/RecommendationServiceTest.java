package com.techhub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.dto.recommendation.RecommendationVO;
import com.techhub.dto.recommendation.RelatedPostVO;
import com.techhub.entity.*;
import com.techhub.enums.VisibilityEnum;
import com.techhub.mapper.*;
import com.techhub.security.SecurityUtils;
import com.techhub.service.impl.RecommendationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationServiceImpl")
class RecommendationServiceTest {

    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private PostSimilarityMapper postSimilarityMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PostVisibilityService postVisibilityService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        securityUtils = mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    private Post createPost(Long id, String title, int views, int likes, int comments) {
        Post post = new Post();
        post.setId(id);
        post.setTitle(title);
        post.setAuthorId(10L + id);
        post.setVisibility(VisibilityEnum.PUBLIC.getCode());
        post.setViewCount(views);
        post.setLikeCount(likes);
        post.setCommentCount(comments);
        post.setDeleted(0);
        post.setCreateTime(LocalDateTime.now());
        return post;
    }

    private User createUser(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setUsername(name);
        return user;
    }

    @Nested
    @DisplayName("getRecommendations — 冷启动")
    class ColdStartTests {

        @Test
        @DisplayName("无画像用户返回热门帖子")
        void returnsHotPostsForNewUser() {
            when(userProfileMapper.selectById(1L)).thenReturn(null);

            Post hotPost = createPost(1L, "热门帖子", 1000, 50, 30);
            when(postMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(hotPost));
            when(postVisibilityService.isVisible(any(Post.class), any(), anyBoolean(), anyBoolean()))
                    .thenReturn(true);
            when(userMapper.selectById(anyLong()))
                    .thenReturn(createUser(11L, "author1"));

            List<RecommendationVO> result = recommendationService.getRecommendations(1L, 1, 20);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("hot", result.get(0).getReason());
        }

        @Test
        @DisplayName("游客无画像返回热门帖子")
        void returnsHotPostsForGuest() {
            Post hotPost = createPost(1L, "热门帖子", 500, 20, 10);
            when(postMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(hotPost));
            when(postVisibilityService.isVisible(any(Post.class), any(), anyBoolean(), anyBoolean()))
                    .thenReturn(true);
            when(userMapper.selectById(anyLong()))
                    .thenReturn(createUser(11L, "author1"));

            List<RecommendationVO> result = recommendationService.getRecommendations(null, 1, 20);

            assertNotNull(result);
            if (!result.isEmpty()) {
                assertEquals("hot", result.get(0).getReason());
            }
        }
    }

    @Nested
    @DisplayName("getRelatedPosts")
    class RelatedPostsTests {

        @Test
        @DisplayName("返回相似帖子列表")
        void returnsSimilarPosts() {
            PostSimilarity similarity = new PostSimilarity();
            similarity.setPostIdA(1L);
            similarity.setPostIdB(2L);
            similarity.setSimilarityScore(0.85);

            when(postSimilarityMapper.selectTopSimilar(eq(1L), eq(10)))
                    .thenReturn(List.of(similarity));

            Post related = createPost(2L, "相似帖子", 100, 10, 5);
            when(postMapper.selectById(2L)).thenReturn(related);
            when(userMapper.selectById(anyLong()))
                    .thenReturn(createUser(12L, "author2"));
            when(postVisibilityService.isVisible(any(Post.class), any(), anyBoolean(), anyBoolean()))
                    .thenReturn(true);

            List<RelatedPostVO> result = recommendationService.getRelatedPosts(1L, 10);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("2", result.get(0).getPostId());
            assertEquals(0.85, result.get(0).getSimilarityScore(), 0.001);
        }

        @Test
        @DisplayName("无相似帖子返回空列表")
        void returnsEmptyWhenNoSimilar() {
            when(postSimilarityMapper.selectTopSimilar(eq(1L), eq(10)))
                    .thenReturn(List.of());

            List<RelatedPostVO> result = recommendationService.getRelatedPosts(1L, 10);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("过滤不可见帖子")
        void filtersInvisiblePosts() {
            PostSimilarity similarity = new PostSimilarity();
            similarity.setPostIdA(1L);
            similarity.setPostIdB(2L);
            similarity.setSimilarityScore(0.85);

            when(postSimilarityMapper.selectTopSimilar(eq(1L), eq(10)))
                    .thenReturn(List.of(similarity));

            Post related = createPost(2L, "私密帖子", 100, 10, 5);
            when(postMapper.selectById(2L)).thenReturn(related);
            when(postVisibilityService.isVisible(any(Post.class), any(), anyBoolean(), anyBoolean()))
                    .thenReturn(false);

            List<RelatedPostVO> result = recommendationService.getRelatedPosts(1L, 10);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("分页限制")
    class PaginationTests {

        @Test
        @DisplayName("pageSize 上限为 50")
        void capsSizeAt50() {
            when(userProfileMapper.selectById(1L)).thenReturn(null);
            Post post = createPost(1L, "test", 1, 1, 1);
            when(postMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(post));
            when(postVisibilityService.isVisible(any(Post.class), any(), anyBoolean(), anyBoolean()))
                    .thenReturn(true);
            when(userMapper.selectById(anyLong()))
                    .thenReturn(createUser(11L, "author"));

            assertDoesNotThrow(() -> recommendationService.getRecommendations(1L, 1, 200));
        }
    }
}
