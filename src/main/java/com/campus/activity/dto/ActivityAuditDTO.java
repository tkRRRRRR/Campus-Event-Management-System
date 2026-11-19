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

    /**
     * 驳回理由的别名。前端表单习惯用 reason 命名，
     * 这里保留兼容，Controller 里会把它归一化到 remark。
     */
    @Size(max = 255, message = "审核意见最多 255 个字")
    private String reason;
}
