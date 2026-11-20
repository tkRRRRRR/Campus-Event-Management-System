package com.campus.activity.controller;

import com.campus.activity.annotation.RequireRole;
import com.campus.activity.common.Constants;
import com.campus.activity.common.PageResult;
import com.campus.activity.common.Result;
import com.campus.activity.dto.ActivityAuditDTO;
import com.campus.activity.dto.ActivityQuery;
import com.campus.activity.dto.ActivitySaveDTO;
import com.campus.activity.service.ActivityService;
import com.campus.activity.util.UserContext;
import com.campus.activity.vo.ActivityDetailVO;
import com.campus.activity.vo.ActivityListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    /** 上传活动封面（组织者、管理员） */
    @PostMapping("/cover")
    @RequireRole({Constants.ROLE_ORGANIZER, Constants.ROLE_ADMIN})
    public Result<String> uploadCover(@RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        return Result.ok("上传成功", activityService.uploadCover(file));
    }

    /** 学生端活动列表：只看已发布且未结束的活动 */
    @GetMapping("/page")
    public Result<PageResult<ActivityListVO>> page(ActivityQuery query) {
        query.setOnlyOpen(true);
        query.setStatus(null);
        query.setOrganizerId(null);
        return Result.ok(activityService.page(query, UserContext.getUserId()));
    }

    /** 组织者：我发布的活动（含草稿、待审核、已驳回） */
    @GetMapping("/my")
    @RequireRole(Constants.ROLE_ORGANIZER)
    public Result<PageResult<ActivityListVO>> my(ActivityQuery query) {
        query.setOrganizerId(UserContext.getUserId());
        query.setOnlyOpen(false);
        return Result.ok(activityService.page(query, UserContext.getUserId()));
    }

    /** 管理员：待审核列表 */
    @GetMapping("/audit/page")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<PageResult<ActivityListVO>> auditPage(ActivityQuery query) {
        query.setStatus(Constants.ACTIVITY_PENDING);
        query.setOnlyOpen(false);
        return Result.ok(activityService.page(query, UserContext.getUserId()));
    }

    /** 活动详情，所有登录用户可见 */
    @GetMapping("/{id}")
    public Result<ActivityDetailVO> detail(@PathVariable Integer id) {
        return Result.ok(activityService.detail(id, UserContext.getUserId()));
    }

    /** 组织者发布活动 */
    @PostMapping
    @RequireRole(Constants.ROLE_ORGANIZER)
    public Result<Integer> create(@Validated @RequestBody ActivitySaveDTO dto) {
        Integer id = activityService.create(dto, UserContext.getUserId());
        String msg = Boolean.TRUE.equals(dto.getDraft()) ? "已保存为草稿" : "已提交审核";
        return Result.ok(msg, id);
    }

    /** 组织者编辑活动 */
    @PutMapping("/{id}")
    @RequireRole(Constants.ROLE_ORGANIZER)
    public Result<Void> update(@PathVariable Integer id, @Validated @RequestBody ActivitySaveDTO dto) {
        dto.setId(id);
        activityService.update(dto, UserContext.getUserId());
        String msg = Boolean.TRUE.equals(dto.getDraft()) ? "草稿已保存" : "已重新提交审核";
        return Result.ok(msg, null);
    }

    /** 组织者取消活动 */
    @PutMapping("/{id}/cancel")
    @RequireRole(Constants.ROLE_ORGANIZER)
    public Result<Void> cancel(@PathVariable Integer id) {
        activityService.cancel(id, UserContext.getUserId());
        return Result.ok("活动已取消，已通知报名学生", null);
    }

    /** 管理员审核 */
    @PutMapping("/{id}/audit")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<Void> audit(@PathVariable Integer id, @Validated @RequestBody ActivityAuditDTO dto) {
        // 兼容前端用 reason 传驳回理由的写法
        if (dto.getRemark() == null || dto.getRemark().trim().isEmpty()) {
            dto.setRemark(dto.getReason());
        }
        activityService.audit(id, dto, UserContext.getUserId());
        return Result.ok(Boolean.TRUE.equals(dto.getPass()) ? "已通过审核" : "已驳回", null);
    }
}
