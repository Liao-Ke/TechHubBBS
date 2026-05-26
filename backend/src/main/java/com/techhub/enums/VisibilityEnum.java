package com.techhub.enums;

import lombok.Getter;

/**
 * 帖子可见性枚举
 */
@Getter
public enum VisibilityEnum {

    PUBLIC(0, "公开"),
    LOGIN_ONLY(1, "登录可见"),
    FOLLOWERS_ONLY(2, "粉丝可见"),
    PRIVATE(3, "私密");

    private final int code;
    private final String desc;

    VisibilityEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据code查找枚举
     */
    public static VisibilityEnum fromCode(int code) {
        for (VisibilityEnum visibility : values()) {
            if (visibility.code == code) {
                return visibility;
            }
        }
        return null;
    }
}
