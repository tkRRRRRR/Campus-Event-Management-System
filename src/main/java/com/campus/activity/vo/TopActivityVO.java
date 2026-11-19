package com.campus.activity.vo;

import lombok.Data;

/**
 * 报名人数排行里的一条活动。
 */
@Data
public class TopActivityVO {

    private Integer id;
    private String title;
    private String categoryName;
    private Integer capacity;
    /** 实际报名人次（已报名 + 已签到 + 爽约，不含已取消与待审核） */
    private Integer joinedCount;
    private Integer signedCount;
    private Integer status;
}
