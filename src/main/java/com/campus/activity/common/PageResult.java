package com.campus.activity.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页返回结构，和 MyBatis-Plus 的 Page 解耦，前端只关心这四个字段。
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long pageNum;
    private Long pageSize;
    private Long total;
    private List<T> records;

    public PageResult() {
    }

    public PageResult(Long pageNum, Long pageSize, Long total, List<T> records) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.records = records;
    }

    public static <T> PageResult<T> of(com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page) {
        return new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }
}
