package com.campus.activity.util;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 当前登录用户的轻量快照（从 token 里解析出来）。
 */
@Data
@AllArgsConstructor
public class LoginUser {

    private Integer userId;
    private String username;
    private String role;
}
