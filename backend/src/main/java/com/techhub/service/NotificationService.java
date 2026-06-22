package com.techhub.service;

import com.techhub.common.PageResult;
import com.techhub.dto.notification.NotificationVO;

/**
 * 通知服务接口 — 通知分页查询、已读标记与未读数统计。
 */
public interface NotificationService {

    /**
     * 分页查询当前用户的通知，按创建时间倒序。
     *
     * @param page   页码
     * @param size   每页条数（上限 50）
     * @param userId 用户 ID
     * @return 分页结果
     */
    PageResult<NotificationVO> listNotifications(int page, int size, Long userId);

    /**
     * 将指定通知标记为已读（仅限本人通知）。
     *
     * @param notificationId 通知 ID
     * @param userId         当前用户 ID
     */
    void markAsRead(Long notificationId, Long userId);

    /**
     * 将当前用户所有未读通知标记为已读。
     *
     * @param userId 用户 ID
     */
    void markAllAsRead(Long userId);

    /**
     * 获取当前用户未读通知数。
     *
     * @param userId 用户 ID
     * @return 未读通知数量
     */
    int getUnreadCount(Long userId);

    /**
     * 创建一条通知（纯数据写入，不做任何查询或业务逻辑）。
     *
     * @param userId     接收通知的用户 ID
     * @param type       通知类型（REPLY / LIKE / FOLLOW / DIVINE）
     * @param sourceId   触发通知的源 ID
     * @param sourceType 来源类型（POST / COMMENT / USER）
     * @param parentId   上级帖子 ID（来源为评论时记录所属帖子 ID，否则传 null）
     * @param content    通知内容（纯文本）
     */
    void create(Long userId, String type, Long sourceId, String sourceType, Long parentId, String content);
}
