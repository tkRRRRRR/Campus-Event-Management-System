package com.campus.activity.controller;

import com.campus.activity.common.PageResult;
import com.campus.activity.common.Result;
import com.campus.activity.entity.Notice;
import com.campus.activity.service.NoticeService;
import com.campus.activity.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 站内消息。全部接口只操作"当前登录用户自己的"消息，不需要额外角色限制。
 */
@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /** 我的消息列表，可按已读状态与类型过滤 */
    @GetMapping("/my")
    public Result<PageResult<Notice>> my(@RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                         @RequestParam(required = false) Integer isRead,
                                         @RequestParam(required = false) String type) {
        return Result.ok(noticeService.myPage(UserContext.getUserId(), pageNum, pageSize, isRead, type));
    }

    /** 未读数，前端做红点 */
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        Map<String, Long> data = new HashMap<>(2);
        data.put("count", noticeService.unreadCount(UserContext.getUserId()));
        return Result.ok(data);
    }

    @PutMapping("/{id}/read")
    public Result<Void> read(@PathVariable Integer id) {
        noticeService.markRead(id, UserContext.getUserId());
        return Result.ok("已标记为已读", null);
    }

    @PutMapping("/read-all")
    public Result<Void> readAll() {
        noticeService.markAllRead(UserContext.getUserId());
        return Result.ok("全部标记为已读", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        noticeService.delete(id, UserContext.getUserId());
        return Result.ok("消息已删除", null);
    }
}
