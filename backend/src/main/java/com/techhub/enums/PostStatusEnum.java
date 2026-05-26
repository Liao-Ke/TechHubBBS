package com.techhub.enums;

import lombok.Getter;

/**
 * 帖子状态枚举
 */
@Getter
public enum PostStatusEnum {

    LOCKED(0, "锁定"),
    NORMAL(1, "正常"),
    DELETED(2, "已删除");

    private final int code;
    private final String desc;

    PostStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据code查找枚举
     */
    public static PostStatusEnum fromCode(int code) {
        for (PostStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
