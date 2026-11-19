package com.campus.activity.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 单个活动的签到统计，供组织者页面顶部的数据卡片使用。
 */
@Data
public class CheckinStatVO {

    private Integer activityId;

    /** 名额上限 */
    private Integer capacity;

    /** 已占用名额（报名成功 + 已签到 + 爽约） */
    private Integer signedCount;

    /** 已签到人数 */
    private Integer checkedCount;

    /** 爽约人数 */
    private Integer absentCount;

    /** 已报名但还没签到的人数 */
    private Integer waitingCount;

    /** 待审核报名人数 */
    private Integer pendingCount;

    /** 已取消报名人数 */
    private Integer cancelCount;

    /** 签到率 = 已签到 / 应到人数，保留两位小数 */
    private BigDecimal checkinRate;
}
