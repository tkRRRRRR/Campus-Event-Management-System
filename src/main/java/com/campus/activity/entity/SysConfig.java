package com.campus.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 系统参数表。爽约阈值、封禁天数、签到码有效期都放这里，
 * 由管理员在后台调整，而不是写死在代码里。
 */
@Data
@TableName("sys_config")
public class SysConfig {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String configKey;

    private String configValue;

    private String remark;
}
