package com.campus.activity.vo;

import lombok.Data;

/**
 * 分类维度统计：每个分类的活动数与报名人次，用于饼图/柱状图。
 */
@Data
public class CategoryStatVO {

    private Integer categoryId;
    private String name;
    private int activityCount;
    private int registrationCount;
}
