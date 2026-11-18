package com.campus.activity.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 活动审核参数。
 */
@Data
public class ActivityAuditDTO {

    @NotNull(message = "请选择审核结果")
    private Boolean pass;

    /** 驳回时必填，通过时可为空 */
    @Size(max = 255, message = "审核意见最多 255 个字")
    private String remark;
}
