package com.campus.activity.dto;

import lombok.Data;

/**
 * 管理员用户列表的查询条件。
 */
@Data
public class UserQuery {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /** STUDENT / ORGANIZER / ADMIN */
    private String role;

    /** 姓名、学号、手机号模糊搜索 */
    private String keyword;

    /** 1 正常 0 禁用 */
    private Integer status;
}
