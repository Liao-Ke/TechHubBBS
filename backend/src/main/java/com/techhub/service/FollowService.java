package com.techhub.service;

import com.techhub.dto.user.FollowStatusVO;

public interface FollowService {

    void follow(Long followeeId);

    void unfollow(Long followeeId);

    FollowStatusVO getFollowStatus(Long followeeId);
}
