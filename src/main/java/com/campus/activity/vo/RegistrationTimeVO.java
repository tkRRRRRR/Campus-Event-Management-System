package com.campus.activity.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 趋势统计用的原始行：只取时间字段，按天归并放在 Java 层做，
 * 避免 H2 与 MySQL 日期函数的方言差异。
 */
@Data
public class RegistrationTimeVO {

    private LocalDateTime signupTime;
    private LocalDateTime checkinTime;
}
