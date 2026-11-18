package com.campus.activity.vo;

import lombok.Data;

@Data
public class LoginVO {

    private String token;

    private Integer userId;

    private String username;

    private String realName;

    private String role;
}
