package com.techhub.enums;

import lombok.Getter;

/**
 * 公告类型枚举
 */
@Getter
public enum NoticeTypeEnum {

    NOTICE(0, "须知"),
    EVENT(1, "活动");

    private final int code;
    private final String desc;

    NoticeTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据code查找枚举
     */
    public static NoticeTypeEnum fromCode(int code) {
        for (NoticeTypeEnum type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
