package com.campus.activity.controller;

import com.campus.activity.annotation.RequireRole;
import com.campus.activity.common.Constants;
import com.campus.activity.common.PageResult;
import com.campus.activity.common.Result;
import com.campus.activity.dto.CheckinDTO;
import com.campus.activity.service.CheckinService;
import com.campus.activity.util.UserContext;
import com.campus.activity.vo.CheckinCodeVO;
import com.campus.activity.vo.CheckinStatVO;
import com.campus.activity.vo.RegistrationMemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    /** 组织者生成签到码（已有效则复用） */
    @PostMapping("/{activityId}/code")
    @RequireRole({Constants.ROLE_ORGANIZER, Constants.ROLE_ADMIN})
    public Result<CheckinCodeVO> generateCode(@PathVariable Integer activityId) {
        return Result.ok("签到码已生成", checkinService.generateCode(
                activityId, UserContext.getUserId(), UserContext.isAdmin()));
    }

    /** 查看当前签到码 */
    @GetMapping("/{activityId}/code")
    @RequireRole({Constants.ROLE_ORGANIZER, Constants.ROLE_ADMIN})
    public Result<CheckinCodeVO> currentCode(@PathVariable Integer activityId) {
        return Result.ok(checkinService.currentCode(
                activityId, UserContext.getUserId(), UserContext.isAdmin()));
    }

    /** 学生凭码签到 */
    @PostMapping("/{activityId}")
    public Result<Void> checkin(@PathVariable Integer activityId,
                                @RequestBody @Valid CheckinDTO dto) {
        checkinService.checkinByCode(activityId, dto.getCode(), UserContext.getUserId());
        return Result.ok("签到成功", null);
    }

    /** 组织者手动补签 */
    @PostMapping("/manual/{registrationId}")
    @RequireRole({Constants.ROLE_ORGANIZER, Constants.ROLE_ADMIN})
    public Result<Void> manual(@PathVariable Integer registrationId) {
        checkinService.manualCheckin(registrationId, UserContext.getUserId(), UserContext.isAdmin());
        return Result.ok("补签成功", null);
    }

    /** 签到名单 */
    @GetMapping("/{activityId}/list")
    @RequireRole({Constants.ROLE_ORGANIZER, Constants.ROLE_ADMIN})
    public Result<PageResult<RegistrationMemberVO>> list(@PathVariable Integer activityId,
                                                         @RequestParam(required = false) Integer status,
                                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(checkinService.memberPage(activityId, status, pageNum, pageSize,
                UserContext.getUserId(), UserContext.isAdmin()));
    }

    /** 签到统计 */
    @GetMapping("/{activityId}/stat")
    @RequireRole({Constants.ROLE_ORGANIZER, Constants.ROLE_ADMIN})
    public Result<CheckinStatVO> stat(@PathVariable Integer activityId) {
        return Result.ok(checkinService.stat(activityId, UserContext.getUserId(), UserContext.isAdmin()));
    }

    /** 提前结束活动并判定爽约（演示用，正常由定时任务触发） */
    @PostMapping("/{activityId}/finish")
    @RequireRole({Constants.ROLE_ORGANIZER, Constants.ROLE_ADMIN})
    public Result<Void> finish(@PathVariable Integer activityId) {
        int absent = checkinService.finishAndMarkAbsent(
                activityId, UserContext.getUserId(), UserContext.isAdmin());
        return Result.ok("活动已结束，爽约 " + absent + " 人", null);
    }
}
