package com.techhub.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.techhub.entity.Comment;
import com.techhub.entity.Post;
import com.techhub.mapper.CommentMapper;
import com.techhub.mapper.PostMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DivineCommentScheduler")
class DivineCommentSchedulerTest {

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private DivineCommentScheduler scheduler;

    private Comment createComment(Long id, Long postId, int like, int rec, int divine) {
        Comment c = new Comment();
        c.setId(id);
        c.setPostId(postId);
        c.setUserId(1L);
        c.setContent("test");
        c.setLikeCount(like);
        c.setRecommendCount(rec);
        c.setIsDivine(divine);
        c.setDivineTime(divine == 1 ? LocalDateTime.now() : null);
        c.setCreateTime(LocalDateTime.now());
        return c;
    }

    private Post createPost(Long id, int divineCount, int eligible) {
        Post p = new Post();
        p.setId(id);
        p.setTitle("Test");
        p.setDivineCommentCount(divineCount);
        p.setEligibleForDivine(eligible);
        return p;
    }

    @Nested
    @DisplayName("scanDivineComments — 撤销不达标神评")
    class DemoteTests {

        @Test
        @DisplayName("likeCount < 10 则撤销神评")
        void demoteWhenLowLikes() {
            Comment c = createComment(1L, 100L, 9, 10, 1);
            when(commentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(c))  // isDivine=1 的神评
                    .thenReturn(List.of());  // eligible posts 下 isDivine=0 的评论
            when(postMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());
            when(postMapper.selectById(100L)).thenReturn(createPost(100L, 1, 1));

            scheduler.scanDivineComments();

            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentMapper, atLeastOnce()).updateById(captor.capture());
            Comment updated = captor.getValue();
            assertEquals(0, updated.getIsDivine());
            assertNull(updated.getDivineTime());
        }

        @Test
        @DisplayName("recommendCount < 5 则撤销神评")
        void demoteWhenLowRecommends() {
            Comment c = createComment(2L, 100L, 15, 4, 1);
            when(commentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(c)).thenReturn(List.of());
            when(postMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());
            when(postMapper.selectById(100L)).thenReturn(createPost(100L, 1, 1));

            scheduler.scanDivineComments();

            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentMapper, atLeastOnce()).updateById(captor.capture());
            assertEquals(0, captor.getValue().getIsDivine());
        }

        @Test
        @DisplayName("likeCount>=10 且 recommendCount>=5 则保持")
        void stayWhenThresholdMet() {
            Comment c = createComment(3L, 100L, 12, 8, 1);
            when(commentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(c)).thenReturn(List.of());
            when(postMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());

            scheduler.scanDivineComments();

            verify(commentMapper, never()).updateById(c);
        }
    }

    @Nested
    @DisplayName("scanDivineComments — 晋升达标评论")
    class PromoteTests {

        @Test
        @DisplayName("eligible 帖子下 like>=10 且 rec>=5 则晋升")
        void promoteWhenThresholdMet() {
            Comment c = createComment(1L, 200L, 12, 8, 0);
            Post post = createPost(200L, 0, 1);
            when(commentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of())   // isDivine=1: 无神评
                    .thenReturn(List.of(c)); // eligible post: isDivine=0
            when(postMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(post));
            when(postMapper.selectById(200L)).thenReturn(post);

            scheduler.scanDivineComments();

            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentMapper, atLeastOnce()).updateById(captor.capture());
            Comment promoted = captor.getValue();
            assertEquals(1, promoted.getIsDivine());
            assertNotNull(promoted.getDivineTime());
        }

        @Test
        @DisplayName("eligible 帖子下 like<10 则不晋升")
        void noPromoteWhenLowLikes() {
            Comment c = createComment(2L, 200L, 9, 10, 0);
            Post post = createPost(200L, 0, 1);
            when(commentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of()).thenReturn(List.of(c));
            when(postMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(post));

            scheduler.scanDivineComments();

            verify(commentMapper, never()).updateById(c);
        }

        @Test
        @DisplayName("非 eligible 帖子跳过不处理")
        void skipIfNotEligible() {
            Post post = createPost(300L, 0, 0);
            when(commentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(), List.of());
            when(postMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(post));

            scheduler.scanDivineComments();

            verify(commentMapper, never()).updateById(any(Comment.class));
        }

        @Test
        @DisplayName("已是神评不重复晋升")
        void skipIfAlreadyDivine() {
            Comment c = createComment(4L, 200L, 20, 10, 1);
            Post post = createPost(200L, 1, 1);
            when(commentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(c), List.of());
            when(postMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(post));

            scheduler.scanDivineComments();

            verify(commentMapper, never()).updateById(c);
        }
    }
}
