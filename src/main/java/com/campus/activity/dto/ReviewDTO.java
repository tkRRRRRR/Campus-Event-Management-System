package com.campus.activity.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 提交评价。
 */
@Data
public class ReviewDTO {

    @NotNull(message = "请选择评分")
    @Min(value = 1, message = "评分最低 1 星")
    @Max(value = 5, message = "评分最高 5 星")
    private Integer score;

    @Size(max = 500, message = "评价内容不能超过 500 字")
    private String content;

    /** 是否匿名，默认实名 */
    private Boolean anonymous;
}
