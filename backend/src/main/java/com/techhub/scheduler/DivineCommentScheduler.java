package com.techhub.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.techhub.entity.Comment;
import com.techhub.entity.Post;
import com.techhub.mapper.CommentMapper;
import com.techhub.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DivineCommentScheduler {

    private static final int LIKE_THRESHOLD = 10;
    private static final int RECOMMEND_THRESHOLD = 5;

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void scanDivineComments() {
        log.info("DivineCommentScheduler: 开始扫描神评状态...");

        int demotedCount = 0;
        int promotedCount = 0;

        // 1. 扫描当前神评 → 撤销不达标的
        List<Comment> divineComments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>().eq(Comment::getIsDivine, 1));
        log.info("DivineCommentScheduler: 当前神评数 = {}", divineComments.size());

        for (Comment c : divineComments) {
            int like = c.getLikeCount() == null ? 0 : c.getLikeCount();
            int rec = c.getRecommendCount() == null ? 0 : c.getRecommendCount();
            if (like < LIKE_THRESHOLD || rec < RECOMMEND_THRESHOLD) {
                demoteComment(c);
                demotedCount++;
            }
        }

        // 2. 扫描 eligibleForDivine=1 的帖子下可能晋升的评论
        List<Post> eligiblePosts = postMapper.selectList(
                new LambdaQueryWrapper<Post>().eq(Post::getEligibleForDivine, 1));
        log.info("DivineCommentScheduler: 支持神评的帖子数 = {}", eligiblePosts.size());

        for (Post post : eligiblePosts) {
            List<Comment> candidates = commentMapper.selectList(
                    new LambdaQueryWrapper<Comment>()
                            .eq(Comment::getPostId, post.getId())
                            .eq(Comment::getIsDivine, 0));
            for (Comment c : candidates) {
                int like = c.getLikeCount() == null ? 0 : c.getLikeCount();
                int rec = c.getRecommendCount() == null ? 0 : c.getRecommendCount();
                if (like >= LIKE_THRESHOLD && rec >= RECOMMEND_THRESHOLD) {
                    promoteComment(c);
                    promotedCount++;
                }
            }
        }

        log.info("DivineCommentScheduler: 扫描完成 — 晋升 {} 条, 撤销 {} 条", promotedCount, demotedCount);
    }

    private void promoteComment(Comment comment) {
        // 仅更新 isDivine 和 divineTime，避免 updateById 覆盖并发的计数器变更
        commentMapper.update(null, new UpdateWrapper<Comment>()
                .eq("id", comment.getId())
                .set("is_divine", 1)
                .set("divine_time", LocalDateTime.now()));
        // 原子更新 divineCommentCount
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, comment.getPostId())
                .setSql("divine_comment_count = IFNULL(divine_comment_count, 0) + 1"));
        log.debug("DivineCommentScheduler: 评论 {} 晋升为神评", comment.getId());
    }

    private void demoteComment(Comment comment) {
        // 仅更新 isDivine 和 divineTime，避免 updateById 覆盖并发的计数器变更
        commentMapper.update(null, new UpdateWrapper<Comment>()
                .eq("id", comment.getId())
                .set("is_divine", 0)
                .set("divine_time", null));
        // 原子更新 divineCommentCount
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, comment.getPostId())
                .gt(Post::getDivineCommentCount, 0)
                .setSql("divine_comment_count = divine_comment_count - 1"));
        log.debug("DivineCommentScheduler: 评论 {} 撤销神评", comment.getId());
    }
}
