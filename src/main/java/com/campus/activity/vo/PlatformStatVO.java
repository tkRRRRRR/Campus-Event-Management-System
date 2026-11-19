package com.campus.activity.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台运营概览（管理员首页卡片 + 状态分布饼图）。
 */
@Data
public class PlatformStatVO {

    /** 用户总数 / 学生数 / 组织者数 / 被封禁人数 */
    private int userCount;
    private int studentCount;
    private int organizerCount;
    private int bannedUserCount;

    /** 活动总数与各状态数量 */
    private int activityCount;
    private int draftCount;
    private int pendingCount;
    private int publishedCount;
    private int finishedCount;
    private int canceledCount;

    /** 报名相关：总数、待审核、已报名未签到、已签到、已取消、爽约 */
    private int registrationCount;
    private int pendingRegCount;
    private int signedUpCount;
    private int checkedCount;
    private int cancelRegCount;
    private int absentCount;

    /** 签到率 = 已签到 / (已签到 + 爽约 + 已报名未签到) */
    private BigDecimal checkinRate;

    /** 评价总数与平均分 */
    private int reviewCount;
    private BigDecimal avgScore;

    /** 分类数量 */
    private int categoryCount;
}
