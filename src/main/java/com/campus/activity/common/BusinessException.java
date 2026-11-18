package com.campus.activity.common;

/**
 * 业务异常。Service 层遇到业务规则不满足时抛这个，由全局异常处理器统一转成 Result。
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BusinessException(String message) {
        this(ResultCode.BAD_REQUEST, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
