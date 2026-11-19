package com.campus.activity.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 组织者看板：只看自己发布的活动。
 */
@Data
public class OrganizerStatVO {

    private int activityCount;
    private int draftCount;
    private int pendingCount;
    private int publishedCount;
    private int finishedCount;
    private int canceledCount;

    private int registrationCount;
    private int pendingRegCount;
    private int signedUpCount;
    private int checkedCount;
    private int cancelRegCount;
    private int absentCount;

    private BigDecimal checkinRate;

    private int reviewCount;
    private BigDecimal avgScore;
}
