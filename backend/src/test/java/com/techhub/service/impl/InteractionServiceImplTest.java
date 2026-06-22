package com.techhub.service.impl;

import com.techhub.common.BusinessException;
import com.techhub.common.ResultCode;
import com.techhub.entity.Comment;
import com.techhub.entity.Post;
import com.techhub.entity.UserLike;
import com.techhub.mapper.CommentMapper;
import com.techhub.mapper.FavoriteMapper;
import com.techhub.mapper.PostMapper;
import com.techhub.mapper.UserLikeMapper;
import com.techhub.security.SecurityUtils;
import com.techhub.service.DivineCommentService;
import com.techhub.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InteractionServiceImpl 单元测试")
class InteractionServiceImplTest {

    @Mock
    private UserLikeMapper userLikeMapper;
    @Mock
    private FavoriteMapper favoriteMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private DivineCommentService divineCommentService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private InteractionServiceImpl interactionService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    private void givenCurrentUser(Long userId) {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
    }

    // ==================== likePost ====================

    @Test
    @DisplayName("不同用户点赞同一帖子，每次都产生通知（核心回归）")
    void likePost_DifferentUsers_EachCreatesNotification() {
        Long postId = 1L;
        Long authorId = 2L;
        Post post = new Post();
        post.setId(postId);
        post.setAuthorId(authorId);
        post.setLikeCount(0);

        when(postMapper.selectById(postId)).thenReturn(post);

        // 用户 B（id=3）点赞帖子 A（作者=2）
        givenCurrentUser(3L);
        interactionService.likePost(postId);
        verify(notificationService).create(authorId, "LIKE", postId, "赞了你的帖子");

        // 用户 C（id=4）点赞同一帖子
        givenCurrentUser(4L);
        interactionService.likePost(postId);
        // 确认通知被调用了两次（用户 B 和 用户 C 各一次）
        verify(notificationService, times(2)).create(eq(authorId), eq("LIKE"), eq(postId), eq("赞了你的帖子"));
    }

    @Test
    @DisplayName("点赞自己的帖子不产生通知")
    void likePost_SelfLike_NoNotification() {
        Long postId = 1L;
        Long authorId = 2L;
        Post post = new Post();
        post.setId(postId);
        post.setAuthorId(authorId);
        post.setLikeCount(0);

        when(postMapper.selectById(postId)).thenReturn(post);
        givenCurrentUser(authorId);

        interactionService.likePost(postId);
        verify(notificationService, never()).create(anyLong(), anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("帖子不存在时抛出 NOT_FOUND")
    void likePost_PostNotFound_ThrowsNotFound() {
        when(postMapper.selectById(1L)).thenReturn(null);
        givenCurrentUser(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interactionService.likePost(1L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("重复点赞抛出 CONFLICT")
    void likePost_DuplicateLike_ThrowsConflict() {
        Post post = new Post();
        post.setId(1L);
        post.setAuthorId(2L);
        post.setLikeCount(0);

        when(postMapper.selectById(1L)).thenReturn(post);
        when(userLikeMapper.insert(any(UserLike.class))).thenThrow(new DuplicateKeyException("dup"));
        givenCurrentUser(3L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interactionService.likePost(1L));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("未登录时点赞抛出 UNAUTHORIZED")
    void likePost_Unauthenticated_ThrowsUnauthorized() {
        givenCurrentUser(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interactionService.likePost(1L));
        assertEquals(ResultCode.UNAUTHORIZED.getCode(), ex.getCode());
    }

    // ==================== likeComment ====================

    @Test
    @DisplayName("不同用户点赞同一评论，每次都产生通知（核心回归）")
    void likeComment_DifferentUsers_EachCreatesNotification() {
        Long commentId = 1L;
        Long commentAuthorId = 2L;
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setUserId(commentAuthorId);
        comment.setLikeCount(0);

        when(commentMapper.selectById(commentId)).thenReturn(comment);

        // 用户 B（id=3）点赞评论
        givenCurrentUser(3L);
        interactionService.likeComment(commentId);
        verify(notificationService).create(commentAuthorId, "LIKE", commentId, "赞了你的评论");

        // 用户 C（id=4）点赞同一评论
        givenCurrentUser(4L);
        interactionService.likeComment(commentId);
        verify(notificationService, times(2)).create(eq(commentAuthorId), eq("LIKE"), eq(commentId), eq("赞了你的评论"));
    }

    @Test
    @DisplayName("点赞自己的评论不产生通知")
    void likeComment_SelfLike_NoNotification() {
        Long commentId = 1L;
        Long authorId = 2L;
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setUserId(authorId);
        comment.setLikeCount(0);

        when(commentMapper.selectById(commentId)).thenReturn(comment);
        givenCurrentUser(authorId);

        interactionService.likeComment(commentId);
        verify(notificationService, never()).create(anyLong(), anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("评论不存在时抛出 NOT_FOUND")
    void likeComment_CommentNotFound_ThrowsNotFound() {
        when(commentMapper.selectById(1L)).thenReturn(null);
        givenCurrentUser(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interactionService.likeComment(1L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("重复点赞评论抛出 CONFLICT")
    void likeComment_DuplicateLike_ThrowsConflict() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setUserId(2L);
        comment.setLikeCount(0);

        when(commentMapper.selectById(1L)).thenReturn(comment);
        when(userLikeMapper.insert(any(UserLike.class))).thenThrow(new DuplicateKeyException("dup"));
        givenCurrentUser(3L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interactionService.likeComment(1L));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("未登录时点赞评论抛出 UNAUTHORIZED")
    void likeComment_Unauthenticated_ThrowsUnauthorized() {
        givenCurrentUser(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interactionService.likeComment(1L));
        assertEquals(ResultCode.UNAUTHORIZED.getCode(), ex.getCode());
    }

    // ==================== notificationService 异常容错 ====================

    @Test
    @DisplayName("通知创建异常时不影响点赞主流程")
    void likePost_NotificationException_DoesNotInterruptLike() {
        Post post = new Post();
        post.setId(1L);
        post.setAuthorId(2L);
        post.setLikeCount(0);

        when(postMapper.selectById(1L)).thenReturn(post);
        doThrow(new RuntimeException("通知服务不可用"))
                .when(notificationService).create(anyLong(), anyString(), anyLong(), anyString());
        givenCurrentUser(3L);

        assertDoesNotThrow(() -> interactionService.likePost(1L));
        // 点赞操作应完成
        verify(postMapper).updateById(post);
        assertEquals(1, post.getLikeCount());
    }
}
