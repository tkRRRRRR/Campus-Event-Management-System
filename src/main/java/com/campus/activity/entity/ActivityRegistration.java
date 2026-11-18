package com.campus.activity.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报名记录表。(activity_id, user_id) 建唯一索引，是防重复报名的最后一道防线。
 */
@Data
@TableName("activity_registration")
public class ActivityRegistration {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer activityId;

    private Integer userId;

    /** 1 已报名 2 已签到 3 已取消 4 爽约 */
    private Integer status;

    private LocalDateTime signupTime;

    private LocalDateTime checkinTime;

    /** 1 签到码签到 2 组织者补签 */
    private Integer checkinType;

    private String remark;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
