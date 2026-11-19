package com.campus.activity.task;

import com.campus.activity.service.CheckinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 活动状态自动流转。
 * 每 5 分钟扫一次，把已过结束时间、还处于"已发布"的活动置为"已结束"，
 * 同时把该活动下"已报名未签到"的记录判为爽约并累加违约次数。
 * 这样学生端列表不用每次都在前端比对时间，爽约统计也不依赖人工操作。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityStatusTask {

    private final CheckinService checkinService;

    @Scheduled(cron = "0 */5 * * * ?")
    public void autoFinishExpired() {
        int count = checkinService.autoFinishExpiredActivities();
        if (count > 0) {
            log.info("定时任务：{} 场活动已自动置为已结束并完成爽约判定", count);
        }
    }
}
