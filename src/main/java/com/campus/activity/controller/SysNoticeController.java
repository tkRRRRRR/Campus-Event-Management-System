package com.campus.activity.controller;

import com.campus.activity.annotation.RequireRole;
import com.campus.activity.common.Constants;
import com.campus.activity.common.PageResult;
import com.campus.activity.common.Result;
import com.campus.activity.dto.SysNoticeDTO;
import com.campus.activity.entity.SysNotice;
import com.campus.activity.service.SysNoticeService;
import com.campus.activity.util.UserContext;
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

import javax.validation.Valid;
import java.util.List;

/**
 * 平台公告：登录用户都能看已发布的，只有管理员能增删改。
 */
@RestController
@RequestMapping("/api/announcement")
@RequiredArgsConstructor
public class SysNoticeController {

    private final SysNoticeService sysNoticeService;

    /** 首页公告栏 */
    @GetMapping("/list")
    public Result<List<SysNotice>> list() {
        return Result.ok(sysNoticeService.listPublished());
    }

    /** 管理端列表 */
    @GetMapping("/page")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<PageResult<SysNotice>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Integer status) {
        return Result.ok(sysNoticeService.page(pageNum, pageSize, keyword, status));
    }

    @PostMapping
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<Void> create(@RequestBody @Valid SysNoticeDTO dto) {
        sysNoticeService.create(dto, UserContext.getUserId());
        return Result.ok("公告已发布", null);
    }

    @PutMapping("/{id}")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<Void> update(@PathVariable Integer id, @RequestBody @Valid SysNoticeDTO dto) {
        sysNoticeService.update(id, dto);
        return Result.ok("公告已更新", null);
    }

    @PutMapping("/{id}/status")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<Void> changeStatus(@PathVariable Integer id, @RequestParam Integer status) {
        sysNoticeService.changeStatus(id, status);
        return Result.ok(status == 1 ? "公告已发布" : "公告已下架", null);
    }

    @DeleteMapping("/{id}")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<Void> delete(@PathVariable Integer id) {
        sysNoticeService.delete(id);
        return Result.ok("公告已删除", null);
    }
}
