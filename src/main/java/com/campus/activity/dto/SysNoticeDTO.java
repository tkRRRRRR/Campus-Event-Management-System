package com.campus.activity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 平台公告的新增/修改参数。
 */
@Data
public class SysNoticeDTO {

    @NotBlank(message = "公告标题不能为空")
    @Size(max = 50, message = "标题最多 50 个字")
    private String title;

    @NotBlank(message = "公告内容不能为空")
    @Size(max = 2000, message = "内容最多 2000 个字")
    private String content;

    /** 1 发布 0 下架，新增时不填默认发布 */
    private Integer status;
}
