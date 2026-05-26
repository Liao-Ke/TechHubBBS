package com.techhub.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Spring Security 上下文工具类，用于获取当前登录用户信息。
 */
public class SecurityUtils {

    private SecurityUtils() {
        // 工具类禁止实例化
    }

    /**
     * 获取当前登录用户 ID（认证主体为 Long 类型）。
     *
     * @return 用户 ID，未认证时返回 null
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            return (Long) auth.getPrincipal();
        }
        return null;
    }

    /**
     * 获取当前登录用户角色（去除 ROLE_ 前缀）。
     *
     * @return 角色字符串（如 "ADMIN"、"USER"），未认证时返回 null
     */
    public static String getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        }
        return null;
    }

    /**
     * 判断当前请求是否已认证。
     *
     * @return true 表示已认证
     */
    public static boolean isAuthenticated() {
        return getCurrentUserId() != null;
    }
}
