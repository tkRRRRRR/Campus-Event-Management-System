package com.campus.activity.dto;

import lombok.Data;

/**
 * 报名相关列表的查询条件。
 */
@Data
public class RegistrationQuery {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /** 报名状态筛选，为空则查全部 */
    private Integer status;

    /** 按姓名或学号搜索（组织者名单用） */
    private String keyword;
}
