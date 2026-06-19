package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.techhub.common.BusinessException;
import com.techhub.common.ResultCode;
import com.techhub.entity.AiQaHistory;
import com.techhub.entity.AiSummary;
import com.techhub.entity.Follow;
import com.techhub.entity.Post;
import com.techhub.enums.VisibilityEnum;
import com.techhub.mapper.AiQaHistoryMapper;
import com.techhub.mapper.AiSummaryMapper;
import com.techhub.mapper.FollowMapper;
import com.techhub.mapper.PostMapper;
import com.techhub.util.AiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiServiceImpl")
class AiServiceImplTest {

    @Mock
    private PostMapper postMapper;
    @Mock
    private AiSummaryMapper aiSummaryMapper;
    @Mock
    private AiQaHistoryMapper aiQaHistoryMapper;
    @Mock
    private AiClient aiClient;
    @Mock
    private FollowMapper followMapper;
    @Mock
    private AiSummaryAsyncExecutor asyncExecutor;

    @InjectMocks
    private AiServiceImpl aiService;

    private static final Long USER_ID = 1L;
    private static final Long POST_ID = 100L;
    private static final Long AUTHOR_ID = 2L;

    // ==================== generateSummary 可见性测试 ====================

    @Nested
    @DisplayName("generateSummary — 可见性校验")
    class GenerateSummaryVisibility {

        @Test
        @DisplayName("私密帖子(visibility=3) → 抛出 FORBIDDEN")
        void shouldRejectPrivatePost() {
            Post post = new Post();
            post.setId(POST_ID);
            post.setVisibility(VisibilityEnum.PRIVATE.getCode());
            post.setContent("x".repeat(60));
            when(postMapper.selectById(POST_ID)).thenReturn(post);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> aiService.generateSummary(USER_ID, POST_ID));
            assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
            assertEquals("私密帖子不支持AI摘要", ex.getMessage());
        }

        @Test
        @DisplayName("粉丝可见帖子(visibility=2) 且用户未关注作者 → 抛出 FORBIDDEN")
        void shouldRejectFollowersOnlyPostWhenNotFollowing() {
            Post post = new Post();
            post.setId(POST_ID);
            post.setVisibility(VisibilityEnum.FOLLOWERS_ONLY.getCode());
            post.setAuthorId(AUTHOR_ID);
            post.setContent("y".repeat(60));
            when(postMapper.selectById(POST_ID)).thenReturn(post);
            when(followMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> aiService.generateSummary(USER_ID, POST_ID));
            assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
            assertEquals("仅关注者可生成此帖子的AI摘要", ex.getMessage());
        }

        @Test
        @DisplayName("公开帖子(visibility=0) → 不抛异常")
        void shouldAllowPublicPost() {
            Post post = new Post();
            post.setId(POST_ID);
            post.setVisibility(VisibilityEnum.PUBLIC.getCode());
            post.setContent("This is a public post with enough content to meet the minimum length requirement for AI summary generation.");
            when(postMapper.selectById(POST_ID)).thenReturn(post);
            when(aiSummaryMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertDoesNotThrow(() -> aiService.generateSummary(USER_ID, POST_ID));
        }

        @Test
        @DisplayName("粉丝可见帖子 且用户已关注作者 → 不抛异常")
        void shouldAllowFollowersOnlyPostWhenFollowing() {
            Post post = new Post();
            post.setId(POST_ID);
            post.setVisibility(VisibilityEnum.FOLLOWERS_ONLY.getCode());
            post.setAuthorId(AUTHOR_ID);
            post.setContent("This is a followers-only post with sufficient length to allow AI summary generation to proceed.");
            when(postMapper.selectById(POST_ID)).thenReturn(post);
            when(followMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
            when(aiSummaryMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertDoesNotThrow(() -> aiService.generateSummary(USER_ID, POST_ID));
        }
    }

    // ==================== askQuestion status 消息测试 ====================

    @Nested
    @DisplayName("askQuestion — 摘要状态校验")
    class AskQuestionStatus {

        @Test
        @DisplayName("无摘要记录 → 抛出 UNPROCESSABLE, 不泄露 status")
        void shouldRejectWhenNoSummary() {
            when(aiSummaryMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> aiService.askQuestion(USER_ID, POST_ID, "问题"));
            assertEquals(ResultCode.UNPROCESSABLE.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("AI摘要暂未生成"));
            assertTrue(ex.getMessage().contains("请先生成摘要"));
            assertFalse(ex.getMessage().contains("status="));
        }

        @Test
        @DisplayName("摘要 status=0(生成中) → 抛出 UNPROCESSABLE, 不泄露 status")
        void shouldRejectWhenSummaryInProgress() {
            AiSummary summary = new AiSummary();
            summary.setUserId(USER_ID);
            summary.setPostId(POST_ID);
            summary.setStatus(0);
            when(aiSummaryMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(summary);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> aiService.askQuestion(USER_ID, POST_ID, "问题"));
            assertEquals(ResultCode.UNPROCESSABLE.getCode(), ex.getCode());
            assertEquals("AI摘要正在生成中，请稍后再试", ex.getMessage());
        }

        @Test
        @DisplayName("摘要 status=2(失败) → 抛出 UNPROCESSABLE, 不泄露 status")
        void shouldRejectWhenSummaryFailed() {
            AiSummary summary = new AiSummary();
            summary.setUserId(USER_ID);
            summary.setPostId(POST_ID);
            summary.setStatus(2);
            when(aiSummaryMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(summary);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> aiService.askQuestion(USER_ID, POST_ID, "问题"));
            assertEquals(ResultCode.UNPROCESSABLE.getCode(), ex.getCode());
            assertEquals("AI摘要生成失败，请重新生成", ex.getMessage());
        }

        @Test
        @DisplayName("摘要 status=1(成功) → 继续问答流程")
        void shouldProceedWhenSummaryReady() {
            AiSummary summary = new AiSummary();
            summary.setUserId(USER_ID);
            summary.setPostId(POST_ID);
            summary.setStatus(1);
            summary.setContent("摘要内容");
            when(aiSummaryMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(summary);

            Post post = new Post();
            post.setId(POST_ID);
            post.setContent("Post content for QA testing.");
            when(postMapper.selectById(POST_ID)).thenReturn(post);
            when(aiClient.callLlm(any(), any())).thenReturn("回答内容");
            doAnswer(inv -> {
                AiQaHistory h = inv.getArgument(0);
                h.setId(999L);
                return 1;
            }).when(aiQaHistoryMapper).insert(any(AiQaHistory.class));

            assertDoesNotThrow(() -> aiService.askQuestion(USER_ID, POST_ID, "测试问题"));
        }
    }
}
