package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.ResultCode;
import com.techhub.dto.notification.NotificationVO;
import com.techhub.entity.Notification;
import com.techhub.mapper.NotificationMapper;
import com.techhub.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public PageResult<NotificationVO> listNotifications(int page, int size, Long userId) {
        if (size > 50) {
            size = 50;
        }

        Page<Notification> mpPage = new Page<>(page, size);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreateTime);
        Page<Notification> result = notificationMapper.selectPage(mpPage, wrapper);

        List<NotificationVO> voList = new ArrayList<>();
        for (Notification n : result.getRecords()) {
            voList.add(toVO(n));
        }

        return new PageResult<>(voList, result.getTotal(), size, page);
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通知不存在");
        }
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此通知");
        }

        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, notificationId)
                .set(Notification::getIsRead, 1);
        notificationMapper.update(null, wrapper);
    }

    @Override
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1);
        notificationMapper.update(null, wrapper);
    }

    @Override
    public int getUnreadCount(Long userId) {
        return notificationMapper.selectUnreadCount(userId);
    }

    @Override
    public void create(Long userId, String type, Long sourceId, String content) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setSourceId(sourceId);
        notification.setContent(content);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);
    }

    // --- 私有辅助方法 -------------------------------------------------------

    private NotificationVO toVO(Notification n) {
        NotificationVO vo = new NotificationVO();
        vo.setId(n.getId().toString());
        vo.setType(n.getType());
        vo.setSourceId(n.getSourceId() != null ? n.getSourceId().toString() : null);
        vo.setContent(n.getContent());
        vo.setIsRead(n.getIsRead() != null && n.getIsRead() == 1);
        vo.setCreateTime(n.getCreateTime());
        // 根据通知类型推导 sourceType
        switch (n.getType()) {
            case "REPLY":
                vo.setSourceType("post");
                break;
            case "LIKE":
                vo.setSourceType("post");
                break;
            case "FOLLOW":
                vo.setSourceType("user");
                break;
            case "DIVINE":
                vo.setSourceType("comment");
                break;
            default:
                vo.setSourceType("unknown");
        }
        return vo;
    }
}
