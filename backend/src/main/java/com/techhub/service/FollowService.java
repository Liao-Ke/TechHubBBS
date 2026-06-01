package com.techhub.service;

import com.techhub.common.PageResult;
import com.techhub.dto.user.FollowStatusVO;
import com.techhub.dto.user.UserProfileVO;

public interface FollowService {

    void follow(Long followeeId);

    void unfollow(Long followeeId);

    FollowStatusVO getFollowStatus(Long followeeId);

    PageResult<UserProfileVO> getFollowers(Long userId, int page, int size);

    PageResult<UserProfileVO> getFollowings(Long userId, int page, int size);
}
