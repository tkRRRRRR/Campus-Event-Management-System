package com.campus.activity.service;

import com.campus.activity.vo.CategoryStatVO;
import com.campus.activity.vo.OrganizerStatVO;
import com.campus.activity.vo.PlatformStatVO;
import com.campus.activity.vo.TopActivityVO;
import com.campus.activity.vo.TrendPointVO;

import java.util.List;

public interface StatService {

    /** 平台运营概览：管理员用 */
    PlatformStatVO overview();

    /** 组织者看板：只统计自己发布的活动 */
    OrganizerStatVO organizer(Integer organizerId);

    /** 分类维度统计；organizerId 为空表示全平台 */
    List<CategoryStatVO> category(Integer organizerId);

    /** 近 N 天的报名/签到趋势 */
    List<TrendPointVO> trend(int days, Integer organizerId);

    /** 报名人数排行 */
    List<TopActivityVO> top(int limit, Integer organizerId);
}
