package com.campus.activity.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动分类（文艺、体育、学术、志愿……）。
 */
@Data
@TableName("activity_category")
public class ActivityCategory {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    /** 排序值，越小越靠前 */
    private Integer sort;

    /** 1 启用 0 停用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
