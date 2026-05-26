package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.techhub.common.BusinessException;
import com.techhub.common.ResultCode;
import com.techhub.dto.user.FollowStatusVO;
import com.techhub.entity.Follow;
import com.techhub.mapper.FollowMapper;
import com.techhub.security.SecurityUtils;
import com.techhub.service.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowMapper followMapper;

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
}
