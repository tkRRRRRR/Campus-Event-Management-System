package com.campus.activity.controller;

import com.campus.activity.annotation.RequireRole;
import com.campus.activity.common.Constants;
import com.campus.activity.common.Result;
import com.campus.activity.service.StatService;
import com.campus.activity.util.UserContext;
import com.campus.activity.vo.CategoryStatVO;
import com.campus.activity.vo.OrganizerStatVO;
import com.campus.activity.vo.PlatformStatVO;
import com.campus.activity.vo.TopActivityVO;
import com.campus.activity.vo.TrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 统计接口：管理员看全平台，组织者看自己的活动。
 * 这里不接收 organizerId 参数，一律由登录身份推导，避免越权查看别人的数据。
 */
@RestController
@RequestMapping("/api/stat")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;

    /** 平台概览（管理员） */
    @GetMapping("/overview")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<PlatformStatVO> overview() {
        return Result.ok(statService.overview());
    }

    /** 组织者看板 */
    @GetMapping("/organizer")
    @RequireRole({Constants.ROLE_ORGANIZER, Constants.ROLE_ADMIN})
    public Result<OrganizerStatVO> organizer() {
        return Result.ok(statService.organizer(scopeOrganizerId()));
    }

    /** 分类分布 */
    @GetMapping("/category")
    @RequireRole({Constants.ROLE_ORGANIZER, Constants.ROLE_ADMIN})
    public Result<List<CategoryStatVO>> category() {
        return Result.ok(statService.category(scopeOrganizerId()));
    }

    /** 报名/签到趋势，默认近 7 天 */
    @GetMapping("/trend")
    @RequireRole({Constants.ROLE_ORGANIZER, Constants.ROLE_ADMIN})
    public Result<List<TrendPointVO>> trend(@RequestParam(defaultValue = "7") Integer days) {
        return Result.ok(statService.trend(days, scopeOrganizerId()));
    }

    /** 报名人数排行 */
    @GetMapping("/top")
    @RequireRole({Constants.ROLE_ORGANIZER, Constants.ROLE_ADMIN})
    public Result<List<TopActivityVO>> top(@RequestParam(defaultValue = "5") Integer limit) {
        return Result.ok(statService.top(limit, scopeOrganizerId()));
    }

    /** 组织者只看自己的；管理员传 null 表示全平台 */
    private Integer scopeOrganizerId() {
        return UserContext.isOrganizer() ? UserContext.getUserId() : null;
    }
}
