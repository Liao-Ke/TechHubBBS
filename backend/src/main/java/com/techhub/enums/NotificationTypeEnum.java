package com.techhub.enums;

import lombok.Getter;

/**
 * 通知类型枚举
 * code 为 String 类型，对应数据库 notification.type 字段（VARCHAR(20)）
 */
@Getter
public enum NotificationTypeEnum {

    REPLY("REPLY", "回复"),
    LIKE("LIKE", "点赞"),
    FOLLOW("FOLLOW", "关注"),
    DIVINE("DIVINE", "神评"),
    SYSTEM("SYSTEM", "系统");

    private final String code;
    private final String desc;

    NotificationTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据code查找枚举
     */
    public static NotificationTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (NotificationTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
