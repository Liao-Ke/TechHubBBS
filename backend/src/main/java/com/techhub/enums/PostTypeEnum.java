package com.techhub.enums;

import lombok.Getter;

/**
 * 帖子类型枚举
 */
@Getter
public enum PostTypeEnum {

    NORMAL(0, "普通"),
    FEATURED(1, "精华"),
    PINNED(2, "置顶");

    private final int code;
    private final String desc;

    PostTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据code查找枚举
     */
    public static PostTypeEnum fromCode(int code) {
        for (PostTypeEnum type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
