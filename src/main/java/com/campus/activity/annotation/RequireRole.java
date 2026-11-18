package com.campus.activity.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在 Controller 类或方法上，声明允许访问的角色。
 * 不写则代表"登录即可访问"。
 * <p>
 * 用法：@RequireRole({Constants.ROLE_ADMIN, Constants.ROLE_ORGANIZER})
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    String[] value();
}
