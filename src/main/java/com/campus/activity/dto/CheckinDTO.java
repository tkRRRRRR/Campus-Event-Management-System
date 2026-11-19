package com.campus.activity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 签到请求。学生扫到的其实就是这串 6 位数字。
 */
@Data
public class CheckinDTO {

    @NotBlank(message = "请输入签到码")
    private String code;
}
