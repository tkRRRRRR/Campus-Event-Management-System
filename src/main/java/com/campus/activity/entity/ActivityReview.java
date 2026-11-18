package com.campus.activity.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动评价。(activity_id, user_id) 唯一，保证一人一条。
 */
@Data
@TableName("activity_review")
public class ActivityReview {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer activityId;

    private Integer userId;

    /** 1~5 星 */
    private Integer score;

    private String content;

    /** 0 实名 1 匿名 */
    private Integer anonymous;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
