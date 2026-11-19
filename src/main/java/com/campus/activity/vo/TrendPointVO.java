package com.campus.activity.vo;

import lombok.Data;

/**
 * 趋势图上的一个点：某一天的新增报名数与签到数。
 */
@Data
public class TrendPointVO {

    /** 日期，格式 MM-dd，前端 X 轴直接用 */
    private String date;
    /** 当天新增报名数 */
    private int registrationCount;
    /** 当天签到数 */
    private int checkinCount;
}
