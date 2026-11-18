package com.campus.activity.util;

import com.campus.activity.common.BusinessException;
import com.campus.activity.common.ResultCode;

/**
 * 用 ThreadLocal 保存当前请求的登录用户，Controller 里随处可取。
 * 一定要在拦截器 afterCompletion 里 clear，否则线程池复用会串数据。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Integer getUserId() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录");
        }
        return user.getUserId();
    }

    public static String getRole() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getRole();
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(getRole());
    }

    public static boolean isOrganizer() {
        return "ORGANIZER".equals(getRole());
    }

    public static void clear() {
        HOLDER.remove();
    }
}
