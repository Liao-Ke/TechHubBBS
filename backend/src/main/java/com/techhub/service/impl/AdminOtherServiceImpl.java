package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.techhub.common.PageResult;
import com.techhub.dto.category.CategoryNoticeVO;
import com.techhub.dto.comment.CommentVO;
import com.techhub.entity.CategoryNotice;
import com.techhub.entity.Comment;
import com.techhub.entity.User;
import com.techhub.mapper.CategoryNoticeMapper;
import com.techhub.mapper.CommentMapper;
import com.techhub.mapper.UserMapper;
import com.techhub.service.AdminOtherService;
import com.techhub.service.DivineCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOtherServiceImpl implements AdminOtherService {

    private final DivineCommentService divineCommentService;
    private final CategoryNoticeMapper categoryNoticeMapper;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public void forceSetDivine(Long commentId, boolean divine) {
        divineCommentService.forceSetDivine(commentId, divine);
    }

    @Override
    public PageResult<CategoryNoticeVO> listNotices(int page, int size, Long categoryId, Integer type) {
        LambdaQueryWrapper<CategoryNotice> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(CategoryNotice::getCategoryId, categoryId);
        }
        if (type != null) {
            wrapper.eq(CategoryNotice::getType, type);
        }
        wrapper.orderByDesc(CategoryNotice::getIsPinned)
                .orderByDesc(CategoryNotice::getCreateTime);

        Page<CategoryNotice> mpPage = new Page<>(page, size);
        Page<CategoryNotice> result = categoryNoticeMapper.selectPage(mpPage, wrapper);

        List<CategoryNoticeVO> vos = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(vos, result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    public PageResult<CommentVO> listComments(int page, int size, String keyword) {
        Page<Comment> mpPage = new Page<>(page, size);
        IPage<Comment> result = commentMapper.selectPageWithPostTitle(mpPage, keyword);

        List<CommentVO> vos = result.getRecords().stream()
                .map(this::toCommentVO)
                .collect(Collectors.toList());
        return PageResult.of(vos, result.getTotal(), result.getSize(), result.getCurrent());
    }

    private CommentVO toCommentVO(Comment comment) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId() != null ? comment.getId().toString() : null);
        vo.setContent(comment.getContent());
        vo.setPostId(comment.getPostId() != null ? comment.getPostId().toString() : null);
        vo.setUserId(comment.getUserId() != null ? comment.getUserId().toString() : null);
        vo.setUsername(comment.getUsername());
        vo.setAvatarUrl(comment.getAvatarUrl());
        vo.setPostTitle(comment.getPostTitle());
        vo.setParentId(comment.getParentId());
        vo.setReplyToUserId(comment.getReplyToUserId());
        vo.setLikeCount(comment.getLikeCount());
        vo.setRecommendCount(comment.getRecommendCount());
        vo.setIsDivine(comment.getIsDivine() != null && comment.getIsDivine() == 1);
        vo.setDivineTime(comment.getDivineTime());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }

    private CategoryNoticeVO toVO(CategoryNotice notice) {
        CategoryNoticeVO vo = new CategoryNoticeVO();
        BeanUtils.copyProperties(notice, vo);
        vo.setId(notice.getId().toString());
        vo.setCategoryId(notice.getCategoryId() != null ? notice.getCategoryId().toString() : null);
        vo.setAuthorId(notice.getAuthorId() != null ? notice.getAuthorId().toString() : null);

        if (notice.getAuthorId() != null) {
            User author = userMapper.selectById(notice.getAuthorId());
            if (author != null) {
                vo.setAuthorName(author.getUsername());
            }
        }
        return vo;
    }
}
