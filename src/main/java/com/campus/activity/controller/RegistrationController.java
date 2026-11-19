package com.campus.activity.controller;

import com.campus.activity.annotation.RequireRole;
import com.campus.activity.common.Constants;
import com.campus.activity.common.PageResult;
import com.campus.activity.common.Result;
import com.campus.activity.dto.RegistrationQuery;
import com.campus.activity.service.RegistrationService;
import com.campus.activity.util.UserContext;
import com.campus.activity.vo.MyRegistrationVO;
import com.campus.activity.vo.RegistrationMemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/registration")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    /** 报名 */
    @PostMapping("/{activityId}")
    public Result<Void> register(@PathVariable Integer activityId) {
        registrationService.register(activityId, UserContext.getUserId());
        return Result.ok("报名成功", null);
    }

    /** 取消报名 */
    @DeleteMapping("/{activityId}")
    public Result<Void> cancel(@PathVariable Integer activityId) {
        registrationService.cancel(activityId, UserContext.getUserId());
        return Result.ok("已取消报名", null);
    }

    /** 我的报名 */
    @GetMapping("/my")
    public Result<PageResult<MyRegistrationVO>> my(RegistrationQuery query) {
        return Result.ok(registrationService.myPage(query, UserContext.getUserId()));
    }

    /** 某活动的报名名单（组织者或管理员） */
    @GetMapping("/activity/{activityId}")
    @RequireRole({Constants.ROLE_ORGANIZER, Constants.ROLE_ADMIN})
    public Result<PageResult<RegistrationMemberVO>> members(@PathVariable Integer activityId,
                                                            RegistrationQuery query) {
        return Result.ok(registrationService.memberPage(
                activityId, query, UserContext.getUserId(), UserContext.isAdmin()));
    }

    /** 组织者审核报名 */
    @PutMapping("/{registrationId}/audit")
    @RequireRole(Constants.ROLE_ORGANIZER)
    public Result<Void> audit(@PathVariable Integer registrationId,
                              @RequestBody Map<String, Object> body) {
        boolean pass = Boolean.TRUE.equals(body.get("pass"));
        registrationService.auditSignup(registrationId, pass, UserContext.getUserId());
        return Result.ok(pass ? "已通过报名" : "已拒绝报名", null);
    }
}
