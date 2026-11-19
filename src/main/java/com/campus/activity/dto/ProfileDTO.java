package com.campus.activity.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 个人资料修改。学号/工号、角色不允许自己改。
 */
@Data
public class ProfileDTO {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 20, message = "姓名最多 20 个字")
    private String realName;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 64, message = "邮箱最多 64 个字符")
    private String email;

    @Size(max = 255, message = "头像地址过长")
    private String avatar;
}
