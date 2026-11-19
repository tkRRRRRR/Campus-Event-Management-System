package com.campus.activity.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 评价聚合结果，仅供统计模块内部使用。
 */
@Data
public class ReviewAggVO {

    private Integer total;
    private BigDecimal avgScore;
}
