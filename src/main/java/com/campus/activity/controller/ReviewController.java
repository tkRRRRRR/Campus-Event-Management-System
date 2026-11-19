package com.campus.activity.controller;

import com.campus.activity.common.PageResult;
import com.campus.activity.common.Result;
import com.campus.activity.dto.ReviewDTO;
import com.campus.activity.service.ReviewService;
import com.campus.activity.util.UserContext;
import com.campus.activity.vo.ReviewStatVO;
import com.campus.activity.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** 提交评价 */
    @PostMapping("/{activityId}")
    public Result<Void> create(@PathVariable Integer activityId,
                               @RequestBody @Valid ReviewDTO dto) {
        reviewService.create(activityId, dto, UserContext.getUserId());
        return Result.ok("评价成功", null);
    }

    /** 活动评价列表 */
    @GetMapping("/activity/{activityId}")
    public Result<PageResult<ReviewVO>> list(@PathVariable Integer activityId,
                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(reviewService.pageByActivity(activityId, pageNum, pageSize,
                UserContext.getUserId(), UserContext.isAdmin()));
    }

    /** 评分统计 + 我的评价 */
    @GetMapping("/activity/{activityId}/stat")
    public Result<ReviewStatVO> stat(@PathVariable Integer activityId) {
        return Result.ok(reviewService.stat(activityId, UserContext.getUserId()));
    }

    /** 我的评价 */
    @GetMapping("/my")
    public Result<PageResult<ReviewVO>> my(@RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(reviewService.myPage(pageNum, pageSize, UserContext.getUserId()));
    }

    /** 删除评价（本人或管理员） */
    @DeleteMapping("/{reviewId}")
    public Result<Void> delete(@PathVariable Integer reviewId) {
        reviewService.delete(reviewId, UserContext.getUserId(), UserContext.isAdmin());
        return Result.ok("已删除评价", null);
    }
}
