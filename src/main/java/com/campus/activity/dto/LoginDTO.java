package com.campus.activity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LoginDTO {

    @NotBlank(message = "请输入学号或工号")
    private String username;

    @NotBlank(message = "请输入密码")
    private String password;
}
