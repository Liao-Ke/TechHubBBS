package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.ResultCode;
import com.techhub.dto.post.PostListQuery;
import com.techhub.dto.post.PostVO;
import com.techhub.dto.user.UserDetailVO;
import com.techhub.dto.user.UserProfileVO;
import com.techhub.dto.user.UserUpdateRequest;
import com.techhub.entity.Post;
import com.techhub.entity.User;
import com.techhub.mapper.PostMapper;
import com.techhub.mapper.UserMapper;
import com.techhub.security.SecurityUtils;
import com.techhub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PostMapper postMapper;

    @Override
    public UserDetailVO getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        return toUserDetailVO(user);
    }

    @Override
    @Transactional
    public void updateProfile(UserUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = new User();
        user.setId(userId);
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        userMapper.updateById(user);
    }

    @Override
    public UserProfileVO getUserProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return toUserProfileVO(user);
    }

    @Override
    public PageResult<PostVO> getUserPosts(Long userId, PostListQuery query) {
        Page<Post> page = new Page<>(query.getPage(), query.getSize());
        Page<Post> resultPage = postMapper.selectPage(page,
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getAuthorId, userId)
                        .eq(Post::getDeleted, 0)
                        .orderByDesc(Post::getCreateTime));
        List<PostVO> records = resultPage.getRecords().stream()
                .map(this::toPostVO)
                .collect(Collectors.toList());
        return PageResult.of(records, resultPage.getTotal(), resultPage.getSize(), resultPage.getCurrent());
    }

    // ---- mapping helpers ----

    private UserDetailVO toUserDetailVO(User user) {
        if (user == null) {
            return null;
        }
        UserDetailVO vo = new UserDetailVO();
        vo.setId(user.getId().toString());
        vo.setUsername(user.getUsername());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setBio(user.getBio());
        vo.setRole(user.getRole());
        vo.setCreateTime(user.getCreateTime());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setUpdateTime(user.getUpdateTime());
        return vo;
    }

    private UserProfileVO toUserProfileVO(User user) {
        if (user == null) {
            return null;
        }
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId().toString());
        vo.setUsername(user.getUsername());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setBio(user.getBio());
        vo.setRole(user.getRole());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    private PostVO toPostVO(Post post) {
        if (post == null) {
            return null;
        }
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
        return vo;
    }
}
