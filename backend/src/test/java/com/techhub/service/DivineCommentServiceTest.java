package com.techhub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.techhub.common.BusinessException;
import com.techhub.common.ResultCode;
import com.techhub.dto.comment.CommentVO;
import com.techhub.entity.Comment;
import com.techhub.entity.CommentRecommend;
import com.techhub.entity.Post;
import com.techhub.entity.User;
import com.techhub.mapper.CommentMapper;
import com.techhub.mapper.CommentRecommendMapper;
import com.techhub.mapper.PostMapper;
import com.techhub.mapper.UserMapper;
import com.techhub.security.SecurityUtils;
import com.techhub.service.impl.DivineCommentServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DivineCommentServiceImpl")
class DivineCommentServiceTest {

    @Mock private CommentMapper commentMapper;
    @Mock private CommentRecommendMapper commentRecommendMapper;
    @Mock private PostMapper postMapper;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private DivineCommentServiceImpl divineCommentService;

    private MockedStatic<SecurityUtils> securityUtils;
    private static final Long USER_ID = 1L;
    private static final Long COMMENT_ID = 100L;
    private static final Long POST_ID = 200L;

    @BeforeEach void setUp() { securityUtils = mockStatic(SecurityUtils.class); }
    @AfterEach void tearDown() { securityUtils.close(); }

    private User createUser(Long id, LocalDateTime createTime) {
        User u = new User();
        u.setId(id); u.setUsername("test"); u.setCreateTime(createTime); u.setRole("USER");
        return u;
    }
    private Comment createComment(Long id, Long postId, int like, int rec, int divine) {
        Comment c = new Comment();
        c.setId(id); c.setPostId(postId); c.setUserId(USER_ID); c.setContent("test");
        c.setLikeCount(like); c.setRecommendCount(rec); c.setIsDivine(divine);
        c.setDivineTime(divine == 1 ? LocalDateTime.now() : null);
        c.setCreateTime(LocalDateTime.now());
        return c;
    }
    private Post createPost(Long id, int cc) {
        Post p = new Post();
        p.setId(id); p.setTitle("T"); p.setCommentCount(cc); p.setDivineCommentCount(0);
        return p;
    }
    private void mockAuth(Long uid) { securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(uid); }

    @Nested @DisplayName("recommend")
    class RecommendTests {
        @Test @DisplayName("未登录 -> UNAUTHORIZED")
        void notAuth() {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(null);
            BusinessException ex = assertThrows(BusinessException.class, () -> divineCommentService.recommend(COMMENT_ID));
            assertEquals(ResultCode.UNAUTHORIZED.getCode(), ex.getCode());
        }
        @Test @DisplayName("注册不足7天 -> FORBIDDEN")
        void newUser() {
            mockAuth(USER_ID);
            when(userMapper.selectById(USER_ID)).thenReturn(createUser(USER_ID, LocalDateTime.now().minusDays(5)));
            BusinessException ex = assertThrows(BusinessException.class, () -> divineCommentService.recommend(COMMENT_ID));
            assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
        }
        @Test @DisplayName("帖子评论数不足10 -> FORBIDDEN")
        void fewComments() {
            mockAuth(USER_ID);
            when(userMapper.selectById(USER_ID)).thenReturn(createUser(USER_ID, LocalDateTime.now().minusDays(30)));
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(createComment(COMMENT_ID, POST_ID, 5, 3, 0));
            when(postMapper.selectById(POST_ID)).thenReturn(createPost(POST_ID, 5));
            BusinessException ex = assertThrows(BusinessException.class, () -> divineCommentService.recommend(COMMENT_ID));
            assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
        }
        @Test @DisplayName("重复推荐 -> CONFLICT")
        void duplicate() {
            mockAuth(USER_ID);
            when(userMapper.selectById(USER_ID)).thenReturn(createUser(USER_ID, LocalDateTime.now().minusDays(30)));
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(createComment(COMMENT_ID, POST_ID, 5, 3, 0));
            when(postMapper.selectById(POST_ID)).thenReturn(createPost(POST_ID, 15));
            when(commentRecommendMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
            BusinessException ex = assertThrows(BusinessException.class, () -> divineCommentService.recommend(COMMENT_ID));
            assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
        }
        @Test @DisplayName("正常推荐 -> recommendCount +1")
        void normal() {
            mockAuth(USER_ID);
            when(userMapper.selectById(USER_ID)).thenReturn(createUser(USER_ID, LocalDateTime.now().minusDays(30)));
            Comment comment = createComment(COMMENT_ID, POST_ID, 5, 3, 0);
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(comment);
            when(postMapper.selectById(POST_ID)).thenReturn(createPost(POST_ID, 15));
            when(commentRecommendMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            doReturn(1).when(commentRecommendMapper).insert(any(CommentRecommend.class));
            doReturn(1).when(commentMapper).updateById(any(Comment.class));

            divineCommentService.recommend(COMMENT_ID);
            ArgumentCaptor<Comment> cap = ArgumentCaptor.forClass(Comment.class);
            verify(commentMapper, atLeastOnce()).updateById(cap.capture());
            assertEquals(4, cap.getAllValues().get(0).getRecommendCount());
        }
    }

    @Nested @DisplayName("cancelRecommend")
    class CancelTests {
        @Test @DisplayName("正常撤销")
        void normalCancel() {
            mockAuth(USER_ID);
            Comment c = createComment(COMMENT_ID, POST_ID, 5, 5, 1);
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(c);
            when(commentRecommendMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
            doReturn(1).when(commentMapper).updateById(any(Comment.class));

            divineCommentService.cancelRecommend(COMMENT_ID);
            ArgumentCaptor<Comment> cap = ArgumentCaptor.forClass(Comment.class);
            verify(commentMapper, atLeastOnce()).updateById(cap.capture());
            assertEquals(4, cap.getAllValues().get(0).getRecommendCount());
        }
        @Test @DisplayName("未推荐过 -> NOT_FOUND")
        void notRecommended() {
            mockAuth(USER_ID);
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(createComment(COMMENT_ID, POST_ID, 5, 3, 0));
            when(commentRecommendMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);
            BusinessException ex = assertThrows(BusinessException.class, () -> divineCommentService.cancelRecommend(COMMENT_ID));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested @DisplayName("checkAndUpdateDivineStatus")
    class CheckDivineTests {
        @Test @DisplayName("阈值达标 -> isDivine=1")
        void thresholdMet() {
            Comment c = createComment(COMMENT_ID, POST_ID, 10, 5, 0);
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(c);
            doReturn(1).when(commentMapper).updateById(any(Comment.class));
            when(postMapper.selectById(POST_ID)).thenReturn(createPost(POST_ID, 15));

            divineCommentService.checkAndUpdateDivineStatus(c);
            ArgumentCaptor<Comment> cap = ArgumentCaptor.forClass(Comment.class);
            verify(commentMapper).updateById(cap.capture());
            assertEquals(1, cap.getValue().getIsDivine());
            assertNotNull(cap.getValue().getDivineTime());
        }
        @Test @DisplayName("阈值丢失 -> isDivine=0")
        void thresholdLost() {
            Comment c = createComment(COMMENT_ID, POST_ID, 8, 4, 1);
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(c);
            doReturn(1).when(commentMapper).updateById(any(Comment.class));
            Post p = createPost(POST_ID, 15);
            p.setDivineCommentCount(1);
            when(postMapper.selectById(POST_ID)).thenReturn(p);

            divineCommentService.checkAndUpdateDivineStatus(c);
            ArgumentCaptor<Comment> cap = ArgumentCaptor.forClass(Comment.class);
            verify(commentMapper).updateById(cap.capture());
            assertEquals(0, cap.getValue().getIsDivine());
            assertNull(cap.getValue().getDivineTime());
        }
        @Test @DisplayName("已是神评且达标 -> 不变")
        void alreadyDivine() {
            Comment c = createComment(COMMENT_ID, POST_ID, 15, 8, 1);
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(c);
            divineCommentService.checkAndUpdateDivineStatus(c);
            verify(commentMapper, never()).updateById(any(Comment.class));
        }
        @Test @DisplayName("非神评且未达标 -> 不变")
        void notDivine() {
            Comment c = createComment(COMMENT_ID, POST_ID, 3, 2, 0);
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(c);
            divineCommentService.checkAndUpdateDivineStatus(c);
            verify(commentMapper, never()).updateById(any(Comment.class));
        }
    }

    @Nested @DisplayName("forceSetDivine")
    class ForceDivineTests {
        @Test @DisplayName("非管理员 -> FORBIDDEN")
        void notAdmin() {
            mockAuth(USER_ID);
            securityUtils.when(SecurityUtils::getCurrentRole).thenReturn("USER");
            BusinessException ex = assertThrows(BusinessException.class, () -> divineCommentService.forceSetDivine(COMMENT_ID, true));
            assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
        }
        @Test @DisplayName("管理员强制设神评（绕过阈值）")
        void adminForceSet() {
            mockAuth(USER_ID);
            securityUtils.when(SecurityUtils::getCurrentRole).thenReturn("ADMIN");
            Comment c = createComment(COMMENT_ID, POST_ID, 0, 0, 0);
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(c);
            doReturn(1).when(commentMapper).updateById(any(Comment.class));
            when(postMapper.selectById(POST_ID)).thenReturn(createPost(POST_ID, 15));

            divineCommentService.forceSetDivine(COMMENT_ID, true);
            ArgumentCaptor<Comment> cap = ArgumentCaptor.forClass(Comment.class);
            verify(commentMapper).updateById(cap.capture());
            assertEquals(1, cap.getValue().getIsDivine());
            assertNotNull(cap.getValue().getDivineTime());
        }
        @Test @DisplayName("管理员强制撤销神评")
        void adminForceUnset() {
            mockAuth(USER_ID);
            securityUtils.when(SecurityUtils::getCurrentRole).thenReturn("ADMIN");
            Comment c = createComment(COMMENT_ID, POST_ID, 20, 10, 1);
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(c);
            doReturn(1).when(commentMapper).updateById(any(Comment.class));
            Post p = createPost(POST_ID, 15);
            p.setDivineCommentCount(1);
            when(postMapper.selectById(POST_ID)).thenReturn(p);

            divineCommentService.forceSetDivine(COMMENT_ID, false);
            ArgumentCaptor<Comment> cap = ArgumentCaptor.forClass(Comment.class);
            verify(commentMapper).updateById(cap.capture());
            assertEquals(0, cap.getValue().getIsDivine());
            assertNull(cap.getValue().getDivineTime());
        }
        @Test @DisplayName("已是神评再设 -> 不变")
        void alreadyDivine() {
            mockAuth(USER_ID);
            securityUtils.when(SecurityUtils::getCurrentRole).thenReturn("ADMIN");
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(createComment(COMMENT_ID, POST_ID, 20, 10, 1));
            divineCommentService.forceSetDivine(COMMENT_ID, true);
            verify(commentMapper, never()).updateById(any(Comment.class));
        }
    }

    @Nested @DisplayName("listDivineComments")
    class ListDivineTests {
        @Test @DisplayName("返回神评列表")
        void returnsList() {
            Comment d1 = createComment(101L, POST_ID, 15, 8, 1);
            d1.setDivineTime(LocalDateTime.now().minusDays(1));
            Comment d2 = createComment(102L, POST_ID, 12, 6, 1);
            d2.setDivineTime(LocalDateTime.now());
            when(commentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(d2, d1));
            when(userMapper.selectById(USER_ID)).thenReturn(createUser(USER_ID, LocalDateTime.now().minusDays(30)));

            List<CommentVO> result = divineCommentService.listDivineComments(POST_ID);
            assertEquals(2, result.size());
            assertTrue(result.get(0).getIsDivine());
        }
        @Test @DisplayName("无神评 -> 空列表")
        void empty() {
            when(commentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
            assertTrue(divineCommentService.listDivineComments(POST_ID).isEmpty());
        }
    }
}
