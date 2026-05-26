package com.techhub.service;

import java.util.Map;

/**
 * 管理后台统计数据服务接口。
 */
public interface AdminStatisticsService {

    /**
     * 获取平台统计数据。
     *
     * @return Map 包含 userCount, postCount, commentCount, todayNewUsers, todayNewPosts, activeUsersToday
     */
    Map<String, Object> getStatistics();
}
