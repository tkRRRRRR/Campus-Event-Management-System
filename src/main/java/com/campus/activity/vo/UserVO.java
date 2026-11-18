package com.campus.activity.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    private Integer id;

    private String username;

    private String realName;

    private String role;

    private String studentNo;

    private String phone;

    private String email;

    private String avatar;

    private Integer status;

    private Integer violationCount;

    private LocalDateTime banUntil;

    private LocalDateTime createTime;
}
