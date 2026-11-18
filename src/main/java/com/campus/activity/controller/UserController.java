package com.campus.activity.controller;

import com.campus.activity.common.Result;
import com.campus.activity.service.UserService;
import com.campus.activity.util.UserContext;
import com.campus.activity.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 当前登录用户信息 */
    @GetMapping("/info")
    public Result<UserVO> info() {
        return Result.ok(userService.getInfo(UserContext.getUserId()));
    }
}
