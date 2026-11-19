package com.campus.activity.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 活动评分统计：平均分 + 5 档分布 + 我的评价（前端用来决定显示"去评价"还是"修改"）。
 */
@Data
public class ReviewStatVO {

    private Integer activityId;

    private BigDecimal avgScore;

    private Integer total;

    private Integer score5;

    private Integer score4;

    private Integer score3;

    private Integer score2;

    private Integer score1;

    private ReviewVO myReview;
}
