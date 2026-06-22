package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.ResultCode;
import com.techhub.dto.user.FollowStatusVO;
import com.techhub.dto.user.UserProfileVO;
import com.techhub.entity.Follow;
import com.techhub.entity.Notification;
import com.techhub.entity.User;
import com.techhub.mapper.FollowMapper;
import com.techhub.mapper.NotificationMapper;
import com.techhub.mapper.UserMapper;
import com.techhub.security.SecurityUtils;
import com.techhub.service.FollowService;
import com.techhub.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowMapper followMapper;
    private final UserMapper userMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void follow(Long followeeId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        if (userId.equals(followeeId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能关注自己");
        }

        Follow follow = new Follow();
        follow.setFollowerId(userId);
        follow.setFolloweeId(followeeId);

        try {
            followMapper.insert(follow);
        } catch (DuplicateKeyException e) {
            log.debug("用户 {} 已关注 {}", userId, followeeId);
            throw new BusinessException(ResultCode.CONFLICT, "已关注该用户");
        }

        try {
            Long count = notificationMapper.selectCount(
                    new LambdaQueryWrapper<Notification>()
                            .eq(Notification::getUserId, followeeId)
                            .eq(Notification::getType, "FOLLOW")
                            .eq(Notification::getSourceId, userId));
            if (count == null || count == 0) {
                notificationService.create(followeeId, "FOLLOW", userId, "USER", null, "关注了你");
            }
        } catch (Exception e) {
            log.warn("创建关注通知失败: followerId={}, followeeId={}, error={}", userId, followeeId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void unfollow(Long followeeId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }

        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, userId)
                .eq(Follow::getFolloweeId, followeeId);
        followMapper.delete(wrapper);
    }

    @Override
    public FollowStatusVO getFollowStatus(Long followeeId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return new FollowStatusVO(false);
        }

        Long count = followMapper.selectCount(
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, userId)
                        .eq(Follow::getFolloweeId, followeeId));
        return new FollowStatusVO(count != null && count > 0);
    }

    @Override
    public PageResult<UserProfileVO> getFollowers(Long userId, int page, int size) {
        Page<Follow> followPage = new Page<>(page, size);
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFolloweeId, userId);
        Page<Follow> result = followMapper.selectPage(followPage, wrapper);

        if (result.getRecords().isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, size, page);
        }

        List<Long> followerIds = result.getRecords().stream()
                .map(Follow::getFollowerId)
                .collect(Collectors.toList());
        List<User> users = userMapper.selectBatchIds(followerIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<UserProfileVO> vos = followerIds.stream()
                .map(userMap::get)
                .filter(Objects::nonNull)
                .map(this::toUserProfileVO)
                .collect(Collectors.toList());

        return PageResult.of(vos, result.getTotal(), size, page);
    }

    @Override
    public PageResult<UserProfileVO> getFollowings(Long userId, int page, int size) {
        Page<Follow> followPage = new Page<>(page, size);
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, userId);
        Page<Follow> result = followMapper.selectPage(followPage, wrapper);

        if (result.getRecords().isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, size, page);
        }

        List<Long> followeeIds = result.getRecords().stream()
                .map(Follow::getFolloweeId)
                .collect(Collectors.toList());
        List<User> users = userMapper.selectBatchIds(followeeIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<UserProfileVO> vos = followeeIds.stream()
                .map(userMap::get)
                .filter(Objects::nonNull)
                .map(this::toUserProfileVO)
                .collect(Collectors.toList());

        return PageResult.of(vos, result.getTotal(), size, page);
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
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
