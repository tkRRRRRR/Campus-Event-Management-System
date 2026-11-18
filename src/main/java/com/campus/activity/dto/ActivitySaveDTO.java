package com.campus.activity.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 活动发布/编辑参数。
 * 时间合法性（结束晚于开始、报名截止不晚于开始）在 Service 里校验，
 * 因为跨字段校验用注解写起来很别扭。
 */
@Data
public class ActivitySaveDTO {

    /** 编辑时必填 */
    private Integer id;

    @NotBlank(message = "活动标题不能为空")
    @Size(max = 100, message = "标题最多 100 个字")
    private String title;

    @NotNull(message = "请选择活动分类")
    private Integer categoryId;

    private String coverUrl;

    @Size(max = 255, message = "简介最多 255 个字")
    private String summary;

    private String content;

    @NotBlank(message = "活动地点不能为空")
    private String location;

    @NotNull(message = "请选择开始时间")
    private LocalDateTime startTime;

    @NotNull(message = "请选择结束时间")
    private LocalDateTime endTime;

    @NotNull(message = "请选择报名截止时间")
    private LocalDateTime signupDeadline;

    @NotNull(message = "请填写人数上限")
    @Min(value = 1, message = "人数上限至少为 1")
    @Max(value = 5000, message = "人数上限过大")
    private Integer capacity;

    /** 报名是否需要审核 0 否 1 是 */
    private Integer needAudit;

    /** true 存草稿，false 直接提交审核 */
    private Boolean draft;
}
