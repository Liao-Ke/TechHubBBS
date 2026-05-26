package com.techhub.common;

import lombok.Data;

@Data
public class R<T> {
    private int code;
    private String message;
    private T data;

    private R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> R<T> ok(T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), "success", data);
    }

    public static <T> R<T> ok(String message) {
        return new R<>(ResultCode.SUCCESS.getCode(), message, null);
    }

    public static <T> R<T> ok() {
        return new R<>(ResultCode.SUCCESS.getCode(), "success", null);
    }

    public static <T> R<T> error(int code, String message) {
        return new R<>(code, message, null);
    }

    public static <T> R<T> error(ResultCode resultCode) {
        return new R<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> R<T> error(ResultCode resultCode, String message) {
        return new R<>(resultCode.getCode(), message, null);
    }
}
