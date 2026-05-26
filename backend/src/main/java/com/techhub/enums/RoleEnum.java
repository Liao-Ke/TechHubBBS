package com.techhub.enums;

import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum RoleEnum {

    USER("user"),
    MODERATOR("moderator"),
    ADMIN("admin");

    private final String code;

    RoleEnum(String code) {
        this.code = code;
    }

    /**
     * 根据角色代码查找枚举
     */
    public static RoleEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (RoleEnum role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        return null;
    }

    /**
     * 判断给定的角色字符串是否匹配当前枚举值
     */
    public boolean hasRole(String role) {
        return this.code.equals(role);
    }
}
