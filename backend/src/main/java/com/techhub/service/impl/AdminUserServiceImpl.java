package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.ResultCode;
import com.techhub.dto.user.UserProfileVO;
import com.techhub.entity.User;
import com.techhub.mapper.UserMapper;
import com.techhub.security.SecurityUtils;
import com.techhub.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;

    @Override
    public PageResult<UserProfileVO> listUsers(int page, int size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or()
                    .like(User::getEmail, keyword));
        }
        wrapper.orderByDesc(User::getCreateTime);

        Page<User> mpPage = new Page<>(page, size);
        Page<User> result = userMapper.selectPage(mpPage, wrapper);

        List<UserProfileVO> vos = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(vos, result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    @Transactional
    public void banUser(Long userId, boolean ban) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (userId.equals(currentUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能封禁/解封自己");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        user.setStatus(ban ? 0 : 1);
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void changeRole(Long userId, String role) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (userId.equals(currentUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能修改自己的角色");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        user.setRole(role);
        userMapper.updateById(user);
    }

    private UserProfileVO toVO(User user) {
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
