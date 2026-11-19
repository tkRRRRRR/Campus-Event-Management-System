package com.campus.activity.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 签到码。前端在大屏/投屏上展示 code，并倒计时 remainSeconds。
 */
@Data
public class CheckinCodeVO {

    private Integer activityId;

    private String code;

    private LocalDateTime expireTime;

    /** 距离过期还有多少秒，已过期或未生成时为 0 */
    private long remainSeconds;

    /** 当前是否存在有效签到码 */
    private Boolean valid;
}
