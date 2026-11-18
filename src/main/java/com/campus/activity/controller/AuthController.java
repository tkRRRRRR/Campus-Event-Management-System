package com.campus.activity.controller;

import com.campus.activity.common.Result;
import com.campus.activity.dto.LoginDTO;
import com.campus.activity.dto.RegisterDTO;
import com.campus.activity.service.UserService;
import com.campus.activity.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证相关接口（登录、注册），不需要 token。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO dto) {
        return Result.ok("登录成功", userService.login(dto));
    }

    @PostMapping("/register")
    public Result<Void> register(@Validated @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.ok("注册成功，请登录", null);
    }
}
