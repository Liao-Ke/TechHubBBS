package com.techhub.service;

import com.techhub.common.PageResult;
import com.techhub.dto.user.UserProfileVO;

/**
 * 管理员用户管理服务。
 */
public interface AdminUserService {

    /**
     * 分页查询用户列表，支持关键词搜索。
     */
    PageResult<UserProfileVO> listUsers(int page, int size, String keyword);

    /**
     * 封禁/解封用户。禁止封禁自己。
     */
    void banUser(Long userId, boolean ban);

    /**
     * 修改用户角色。禁止修改自己的角色。
     */
    void changeRole(Long userId, String role);
}
