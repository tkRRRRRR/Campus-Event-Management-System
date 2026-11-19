package com.campus.activity.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生视角的"我的报名"。
 */
@Data
public class MyRegistrationVO {

    private Integer id;

    private Integer activityId;

    private String activityTitle;

    private String coverUrl;

    private String location;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime signupDeadline;

    /** 活动状态 0~5 */
    private Integer activityStatus;

    /** 报名状态 1 已报名 2 已签到 3 已取消 4 爽约 5 待审核 */
    private Integer status;

    private LocalDateTime signupTime;

    private LocalDateTime checkinTime;

    private Integer checkinType;

    /** 是否已评价（前端据此决定"去评价"按钮是否显示） */
    private Boolean reviewed;
}
