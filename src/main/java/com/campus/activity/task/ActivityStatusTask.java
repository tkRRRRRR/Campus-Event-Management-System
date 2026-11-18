package com.campus.activity.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.activity.common.Constants;
import com.campus.activity.entity.Activity;
import com.campus.activity.mapper.ActivityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 活动状态自动流转。
 * 每 5 分钟扫一次，把已过结束时间、还处于"已发布"的活动置为"已结束"，
 * 这样学生端列表不用每次都在前端比对时间。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityStatusTask {

    private final ActivityMapper activityMapper;

    @Scheduled(cron = "0 */5 * * * ?")
    public void autoFinishExpired() {
        LambdaUpdateWrapper<Activity> wrapper = new LambdaUpdateWrapper<Activity>()
                .set(Activity::getStatus, Constants.ACTIVITY_FINISHED)
                .eq(Activity::getStatus, Constants.ACTIVITY_PUBLISHED)
                .lt(Activity::getEndTime, LocalDateTime.now());
        int count = activityMapper.update(null, wrapper);
        if (count > 0) {
            log.info("定时任务：{} 场活动已自动置为已结束", count);
        }
    }
}
