package com.techhub.controller.admin;

import com.techhub.common.R;
import com.techhub.service.AdminStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理后台统计数据接口
 * 仅 ADMIN 与 MODERATOR 可访问，Redis 缓存 5 分钟。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final AdminStatisticsService adminStatisticsService;

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public R<Map<String, Object>> getStatistics() {
        return R.ok(adminStatisticsService.getStatistics());
    }
}
