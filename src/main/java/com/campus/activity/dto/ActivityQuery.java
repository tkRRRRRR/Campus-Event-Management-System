package com.campus.activity.dto;

import lombok.Data;

/**
 * 活动列表查询条件。
 */
@Data
public class ActivityQuery {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private Integer categoryId;

    private String keyword;

    /** 按状态筛选，管理员/组织者后台用 */
    private Integer status;

    /** 只看组织者自己的活动 */
    private Integer organizerId;

    /** 只返回"已发布且未结束"的活动（学生端列表） */
    private Boolean onlyOpen;

    /** latest 最新发布在前 / hot 报名人数多的在前 */
    private String sort = "latest";
}
