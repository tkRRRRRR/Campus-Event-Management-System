package com.campus.activity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class RegisterDTO {

    @NotBlank(message = "学号/工号不能为空")
    @Size(min = 4, max = 20, message = "学号/工号长度需在 4~20 位之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "密码长度需在 6~20 位之间")
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    /** STUDENT / ORGANIZER，管理员由后台指定 */
    private String role;

    private String studentNo;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
