package com.techhub.service;

/**
 * 用户画像服务 — 基于交互行为构建关键词偏好。
 */
public interface UserProfileService {

    /**
     * 为指定用户构建/更新兴趣画像。
     */
    void buildProfile(Long userId);

    /**
     * 为所有用户构建画像（批量调度用）。
     */
    void buildAllProfiles();
}
