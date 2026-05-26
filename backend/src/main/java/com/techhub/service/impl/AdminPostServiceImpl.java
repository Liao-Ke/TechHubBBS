package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.ResultCode;
import com.techhub.dto.post.PostVO;
import com.techhub.entity.Category;
import com.techhub.entity.Post;
import com.techhub.entity.User;
import com.techhub.enums.PostStatusEnum;
import com.techhub.mapper.CategoryMapper;
import com.techhub.mapper.PostMapper;
import com.techhub.mapper.UserMapper;
import com.techhub.service.AdminPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPostServiceImpl implements AdminPostService {

    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public PageResult<PostVO> listAllPosts(int page, int size, String keyword) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Post::getTitle, keyword)
                    .or()
                    .like(Post::getContent, keyword));
        }

        wrapper.orderByDesc(Post::getType).orderByDesc(Post::getCreateTime);

        Page<Post> mpPage = new Page<>(page, size);
        Page<Post> result = postMapper.selectPage(mpPage, wrapper);

        List<PostVO> vos = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(vos, result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    @Transactional
    public void setPostType(Long postId, int type) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        post.setType(type);
        postMapper.updateById(post);
    }

    @Override
    @Transactional
    public void togglePostLock(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        Integer currentStatus = post.getStatus();
        if (currentStatus == null || currentStatus.equals(PostStatusEnum.NORMAL.getCode())) {
            post.setStatus(PostStatusEnum.LOCKED.getCode());
        } else {
            post.setStatus(PostStatusEnum.NORMAL.getCode());
        }
        postMapper.updateById(post);
    }

    @Override
    @Transactional
    public void forceDeletePost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        // 物理删除，绕过逻辑删除
        postMapper.delete(new LambdaQueryWrapper<Post>().eq(Post::getId, postId));
    }

    private PostVO toVO(Post post) {
        PostVO vo = new PostVO();
        vo.setId(post.getId().toString());
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setCategoryId(post.getCategoryId() != null ? post.getCategoryId().toString() : null);
        vo.setAuthorId(post.getAuthorId() != null ? post.getAuthorId().toString() : null);
        vo.setType(post.getType());
        vo.setStatus(post.getStatus());
        vo.setVisibility(post.getVisibility());
        vo.setViewCount(post.getViewCount());
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setDivineCommentCount(post.getDivineCommentCount());
        vo.setCreateTime(post.getCreateTime());
        vo.setUpdateTime(post.getUpdateTime());

        User author = userMapper.selectById(post.getAuthorId());
        if (author != null) {
            vo.setAuthorName(author.getUsername());
            vo.setAuthorAvatar(author.getAvatarUrl());
        }

        if (post.getCategoryId() != null) {
            Category cat = categoryMapper.selectById(post.getCategoryId());
            if (cat != null) {
                vo.setCategoryName(cat.getName());
            }
        }

        return vo;
    }
}
