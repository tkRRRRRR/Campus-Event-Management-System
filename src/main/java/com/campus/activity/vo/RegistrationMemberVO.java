package com.campus.activity.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 组织者视角的报名名单。
 */
@Data
public class RegistrationMemberVO {

    private Integer id;

    private Integer userId;

    private String username;

    private String realName;

    private String studentNo;

    private String phone;

    /** 1 已报名 2 已签到 3 已取消 4 爽约 5 待审核 */
    private Integer status;

    private LocalDateTime signupTime;

    private LocalDateTime checkinTime;

    private Integer checkinType;
}
