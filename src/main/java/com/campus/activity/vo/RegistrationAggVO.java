package com.campus.activity.vo;

import lombok.Data;

/**
 * 报名记录聚合结果，仅供统计模块内部使用。
 */
@Data
public class RegistrationAggVO {

    private Integer total;
    private Integer signedUp;
    private Integer checked;
    private Integer canceled;
    private Integer absent;
    private Integer pending;
}
