package com.techhub.controller;

import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.R;
import com.techhub.common.ResultCode;
import com.techhub.dto.notification.NotificationVO;
import com.techhub.security.SecurityUtils;
import com.techhub.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public R<PageResult<NotificationVO>> listNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = requireAuth();
        return R.ok(notificationService.listNotifications(page, size, userId));
    }

    @PatchMapping("/{id}/read")
    public R<Void> markAsRead(@PathVariable Long id) {
        Long userId = requireAuth();
        notificationService.markAsRead(id, userId);
        return R.ok("已读");
    }

    @PatchMapping("/read-all")
    public R<Void> markAllAsRead() {
        Long userId = requireAuth();
        notificationService.markAllAsRead(userId);
        return R.ok("全部已读");
    }

    @GetMapping("/unread-count")
    public R<Map<String, Integer>> getUnreadCount() {
        Long userId = requireAuth();
        return R.ok(Map.of("count", notificationService.getUnreadCount(userId)));
    }

    private Long requireAuth() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
