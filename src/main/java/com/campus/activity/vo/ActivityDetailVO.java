package com.campus.activity.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动详情。在列表字段基础上补充内容、审核意见与参与情况统计。
 */
@Data
public class ActivityDetailVO {

    private Integer id;

    private String title;

    private Integer categoryId;

    private String categoryName;

    private String coverUrl;

    private String summary;

    private String content;

    private String location;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime signupDeadline;

    private Integer capacity;

    private Integer signedCount;

    private Integer status;

    private Integer organizerId;

    private String organizerName;

    private String auditRemark;

    private Integer needAudit;

    private LocalDateTime createTime;

    /** 当前用户报名状态，未报名为 null */
    private Integer regStatus;

    /** 已报名人数（含已签到与爽约，与 activity.signed_count 口径一致） */
    private Integer joinedCount;

    /** 已签到人数 */
    private Integer checkinCount;

    /** 爽约人数 */
    private Integer absentCount;

    /** 已取消人数 */
    private Integer cancelCount;

    /** 平均评分，无人评价为 null */
    private Double avgScore;

    /** 评价条数 */
    private Integer reviewCount;

    /** 剩余名额 */
    private Integer remaining;

    /**
     * 活动质量分（0~100）= 签到率×40 + 评分归一化×40 + 完成率×20
     * 用于热门排序和活动复盘，计算放在 Java 层，避免依赖数据库函数。
     */
    private Double qualityScore;
}
