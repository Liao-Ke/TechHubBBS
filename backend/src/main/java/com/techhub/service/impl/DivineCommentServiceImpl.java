package com.techhub.service.impl;

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
import com.techhub.service.DivineCommentService;
import com.techhub.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DivineCommentServiceImpl implements DivineCommentService {

    private final CommentMapper commentMapper;
    private final CommentRecommendMapper commentRecommendMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void recommend(Long commentId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(MIN_REGISTER_DAYS);
        if (user.getCreateTime().isAfter(sevenDaysAgo)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "注册不足7天，无法推荐神评");
        }
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }
        Post post = postMapper.selectById(comment.getPostId());
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        if (post.getCommentCount() == null || post.getCommentCount() < MIN_POST_COMMENTS) {
            throw new BusinessException(ResultCode.FORBIDDEN, "帖子评论数不足10条，暂不支持神评推荐");
        }
        Long existingCount = commentRecommendMapper.selectCount(
                new LambdaQueryWrapper<CommentRecommend>()
                        .eq(CommentRecommend::getCommentId, commentId)
                        .eq(CommentRecommend::getUserId, userId));
        if (existingCount != null && existingCount > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "已经推荐过该评论");
        }
        CommentRecommend record = new CommentRecommend();
        record.setCommentId(commentId);
        record.setUserId(userId);
        commentRecommendMapper.insert(record);
        comment.setRecommendCount(comment.getRecommendCount() == null ? 1 : comment.getRecommendCount() + 1);
        commentMapper.updateById(comment);
        checkAndUpdateDivineStatus(comment);
    }

    @Override
    @Transactional
    public void cancelRecommend(Long commentId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }
        int deleted = commentRecommendMapper.delete(
                new LambdaQueryWrapper<CommentRecommend>()
                        .eq(CommentRecommend::getCommentId, commentId)
                        .eq(CommentRecommend::getUserId, userId));
        if (deleted == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "未推荐过该评论");
        }
        int currentCount = comment.getRecommendCount() == null ? 0 : comment.getRecommendCount();
        comment.setRecommendCount(Math.max(0, currentCount - 1));
        commentMapper.updateById(comment);
        checkAndUpdateDivineStatus(comment);
    }

    @Override
    @Transactional
    public void checkAndUpdateDivineStatus(Comment comment) {
        Comment fresh = commentMapper.selectById(comment.getId());
        if (fresh == null) return;
        int likeCount = fresh.getLikeCount() == null ? 0 : fresh.getLikeCount();
        int recommendCount = fresh.getRecommendCount() == null ? 0 : fresh.getRecommendCount();
        boolean thresholdMet = likeCount >= LIKE_THRESHOLD && recommendCount >= RECOMMEND_THRESHOLD;
        boolean currentlyDivine = fresh.getIsDivine() != null && fresh.getIsDivine() == 1;
        if (thresholdMet && !currentlyDivine) {
            promoteToDivine(fresh);
        } else if (!thresholdMet && currentlyDivine) {
            demoteFromDivine(fresh);
        }
    }

    @Override
    @Transactional
    public void forceSetDivine(Long commentId, boolean divine) {
        String role = SecurityUtils.getCurrentRole();
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可强制设置神评状态");
        }
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }
        boolean currentlyDivine = comment.getIsDivine() != null && comment.getIsDivine() == 1;
        if (divine && !currentlyDivine) {
            promoteToDivine(comment);
        } else if (!divine && currentlyDivine) {
            demoteFromDivine(comment);
        }
    }

    @Override
    public List<CommentVO> listDivineComments(Long postId) {
        List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, postId)
                        .eq(Comment::getIsDivine, 1)
                        .orderByDesc(Comment::getDivineTime));
        List<CommentVO> voList = new ArrayList<>();
        for (Comment c : comments) {
            voList.add(toCommentVO(c));
        }
        return voList;
    }

    private void promoteToDivine(Comment comment) {
        comment.setIsDivine(1);
        comment.setDivineTime(LocalDateTime.now());
        commentMapper.updateById(comment);
        Post post = postMapper.selectById(comment.getPostId());
        if (post != null) {
            post.setDivineCommentCount(post.getDivineCommentCount() == null ? 1 : post.getDivineCommentCount() + 1);
            postMapper.updateById(post);
        }

        try {
            notificationService.create(comment.getUserId(), "DIVINE", comment.getId(), "你的评论被推荐为神评");
        } catch (Exception e) {
            log.warn("创建神评通知失败: commentId={}, error={}", comment.getId(), e.getMessage());
        }
    }

    private void demoteFromDivine(Comment comment) {
        comment.setIsDivine(0);
        comment.setDivineTime(null);
        commentMapper.updateById(comment);
        Post post = postMapper.selectById(comment.getPostId());
        if (post != null && post.getDivineCommentCount() != null && post.getDivineCommentCount() > 0) {
            post.setDivineCommentCount(post.getDivineCommentCount() - 1);
            postMapper.updateById(post);
        }
    }

    private CommentVO toCommentVO(Comment comment) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId().toString());
        vo.setContent(comment.getContent());
        vo.setPostId(comment.getPostId().toString());
        vo.setUserId(comment.getUserId().toString());
        vo.setParentId(comment.getParentId());
        vo.setReplyToUserId(comment.getReplyToUserId());
        vo.setLikeCount(comment.getLikeCount());
        vo.setRecommendCount(comment.getRecommendCount());
        vo.setIsDivine(comment.getIsDivine() != null && comment.getIsDivine() == 1);
        vo.setDivineTime(comment.getDivineTime());
        vo.setLiked(false);
        vo.setCreateTime(comment.getCreateTime());
        User author = userMapper.selectById(comment.getUserId());
        if (author != null) {
            vo.setUsername(author.getUsername());
            vo.setAvatarUrl(author.getAvatarUrl());
        }
        return vo;
    }
}
