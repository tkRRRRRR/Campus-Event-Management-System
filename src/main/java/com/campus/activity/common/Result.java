package com.campus.activity.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回体：所有接口都返回这一个结构，前端拦截器只认 code。
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;
    private T data;

    public Result() {
    }

    public Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(ResultCode.OK, "操作成功", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.OK, "操作成功", data);
    }

    public static <T> Result<T> ok(String msg, T data) {
        return new Result<>(ResultCode.OK, msg, data);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(ResultCode.ERROR, msg, null);
    }

    public static <T> Result<T> fail(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }
}
