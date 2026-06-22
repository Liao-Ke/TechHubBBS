package com.techhub.service.impl;

import com.techhub.entity.Comment;
import com.techhub.entity.Post;
import com.techhub.entity.UserLike;
import com.techhub.mapper.CommentMapper;
import com.techhub.mapper.FavoriteMapper;
import com.techhub.mapper.NotificationMapper;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InteractionServiceImpl 通知字段验证")
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
    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private InteractionServiceImpl interactionService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    private static final Long USER_ID = 2L;
    private static final Long POST_ID = 100L;
    private static final Long COMMENT_ID = 200L;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);

        Post mockPost = new Post();
        mockPost.setId(POST_ID);
        mockPost.setAuthorId(1L);
        mockPost.setLikeCount(0);
        when(postMapper.selectById(POST_ID)).thenReturn(mockPost);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    @DisplayName("likePost 通知：sourceType=POST, parentId=null")
    void likePostShouldCreateNotificationWithPostSourceType() {
        doReturn(1).when(userLikeMapper).insert(any(UserLike.class));
        when(notificationMapper.selectCount(any())).thenReturn(0L);

        interactionService.likePost(POST_ID);

        verify(notificationService).create(eq(1L), eq("LIKE"), eq(POST_ID),
                eq("POST"), isNull(), eq("赞了你的帖子"));
    }

    @Test
    @DisplayName("likeComment 通知：sourceType=COMMENT, parentId=comment.getPostId()")
    void likeCommentShouldCreateNotificationWithCommentSourceType() {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);

        Comment mockComment = new Comment();
        mockComment.setId(COMMENT_ID);
        mockComment.setUserId(3L);
        mockComment.setPostId(POST_ID);
        mockComment.setLikeCount(0);
        when(commentMapper.selectById(COMMENT_ID)).thenReturn(mockComment);
        doReturn(1).when(userLikeMapper).insert(any(UserLike.class));
        when(notificationMapper.selectCount(any())).thenReturn(0L);
        doNothing().when(divineCommentService).checkAndUpdateDivineStatus(any());
        doReturn(1).when(commentMapper).updateById(any(Comment.class));

        interactionService.likeComment(COMMENT_ID);

        verify(notificationService).create(eq(3L), eq("LIKE"), eq(COMMENT_ID),
                eq("COMMENT"), eq(POST_ID), eq("赞了你的评论"));
    }

    @Test
    @DisplayName("likeComment 通知：点赞自己的评论不发通知")
    void likeOwnCommentShouldNotCreateNotification() {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);

        Comment mockComment = new Comment();
        mockComment.setId(COMMENT_ID);
        mockComment.setUserId(USER_ID);
        mockComment.setPostId(POST_ID);
        mockComment.setLikeCount(0);
        when(commentMapper.selectById(COMMENT_ID)).thenReturn(mockComment);
        doReturn(1).when(userLikeMapper).insert(any(UserLike.class));
        doNothing().when(divineCommentService).checkAndUpdateDivineStatus(any());
        doReturn(1).when(commentMapper).updateById(any(Comment.class));

        interactionService.likeComment(COMMENT_ID);

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any());
    }
}