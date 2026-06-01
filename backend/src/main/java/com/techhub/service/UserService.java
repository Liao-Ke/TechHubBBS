package com.techhub.service;

import com.techhub.common.PageResult;
import com.techhub.dto.post.PostListQuery;
import com.techhub.dto.post.PostVO;
import com.techhub.dto.user.UserDetailVO;
import com.techhub.dto.user.UserProfileVO;
import com.techhub.dto.user.UserUpdateRequest;

public interface UserService {
    UserDetailVO getCurrentUser();
    void updateProfile(UserUpdateRequest request);
    UserProfileVO getUserProfile(Long userId);
    PageResult<PostVO> getUserPosts(Long userId, PostListQuery query);
    void changePassword(Long userId, String oldPwd, String newPwd);
    PageResult<PostVO> getFavorites(Long userId, int page, int size);
}
