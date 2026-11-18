package com.campus.activity.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动列表项。比实体多了分类名、组织者名和当前用户的报名状态。
 */
@Data
public class ActivityListVO {

    private Integer id;

    private String title;

    private Integer categoryId;

    private String categoryName;

    private String coverUrl;

    private String summary;

    private String location;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime signupDeadline;

    private Integer capacity;

    private Integer signedCount;

    private Integer status;

    private Integer organizerId;

    private String organizerName;

    private LocalDateTime createTime;

    /**
     * 当前登录用户在该活动下的报名状态：1 已报名 2 已签到 3 已取消 4 爽约
     * 没报过则为 null
     */
    private Integer regStatus;

    /** 剩余名额，避免前端自己算 */
    private Integer remaining;
}
