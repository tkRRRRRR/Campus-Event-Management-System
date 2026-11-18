package com.campus.activity.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动主表。
 * signedCount 是冗余字段，报名时用 "update ... where signed_count < capacity" 原子扣减，
 * 这是整个系统防止超卖的关键设计。
 */
@Data
@TableName("activity")
public class Activity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String title;

    private Integer categoryId;

    private String coverUrl;

    /** 一句话简介，列表页展示 */
    private String summary;

    /** 详细说明，详情页展示 */
    private String content;

    private String location;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime signupDeadline;

    /** 人数上限 */
    private Integer capacity;

    /** 已报名人数（冗余字段，用于并发控制） */
    private Integer signedCount;

    /** 0 草稿 1 待审核 2 已发布 3 已驳回 4 已取消 5 已结束 */
    private Integer status;

    private Integer organizerId;

    /** 审核意见（驳回时填写） */
    private String auditRemark;

    /** 当前有效的签到码，6 位数字 */
    private String checkinCode;

    /** 签到码失效时间 */
    private LocalDateTime codeExpireTime;

    /** 报名是否需要组织者审核 0 否 1 是 */
    private Integer needAudit;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
