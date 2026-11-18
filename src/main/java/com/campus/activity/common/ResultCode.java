package com.campus.activity.common;

/**
 * 业务返回码。前端只对 200 放行，401 跳登录页，其余弹错误提示。
 */
public final class ResultCode {

    private ResultCode() {
    }

    /** 成功 */
    public static final int OK = 200;
    /** 参数校验不通过 */
    public static final int BAD_REQUEST = 400;
    /** 未登录 / token 失效 */
    public static final int UNAUTHORIZED = 401;
    /** 已登录但无权限 */
    public static final int FORBIDDEN = 403;
    /** 资源不存在 */
    public static final int NOT_FOUND = 404;
    /** 服务器内部错误 */
    public static final int ERROR = 500;
}
