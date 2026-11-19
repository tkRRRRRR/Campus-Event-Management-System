package com.campus.activity.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价展示对象。匿名评价在后端就把姓名替换掉，避免"隐藏靠前端"的漏洞。
 */
@Data
public class ReviewVO {

    private Integer id;

    private Integer activityId;

    private String activityTitle;

    private Integer userId;

    /** 匿名且不是本人时返回"匿名同学" */
    private String userName;

    private Integer score;

    private String content;

    private Integer anonymous;

    private LocalDateTime createTime;

    /** 是否是当前登录用户自己写的 */
    private Boolean mine;
}
