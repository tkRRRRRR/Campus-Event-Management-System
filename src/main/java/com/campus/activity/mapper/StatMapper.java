package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.entity.ActivityReview;
import com.campus.activity.vo.CategoryStatVO;
import com.campus.activity.vo.RegistrationAggVO;
import com.campus.activity.vo.RegistrationTimeVO;
import com.campus.activity.vo.ReviewAggVO;
import com.campus.activity.vo.TopActivityVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统计模块专用 Mapper。
 * 报名/评价的聚合放在 XML 里，组织者维度通过 organizerId 是否为空动态联表，
 * 一套 SQL 同时服务"全平台"和"我的活动"两种口径。
 */
public interface StatMapper extends BaseMapper<ActivityReview> {

    /** 报名记录按状态聚合 */
    RegistrationAggVO selectRegAgg(@Param("organizerId") Integer organizerId);

    /** 评价总数与平均分 */
    ReviewAggVO selectReviewAgg(@Param("organizerId") Integer organizerId);

    /** 分类维度的活动数与报名人次 */
    List<CategoryStatVO> selectCategoryStat(@Param("organizerId") Integer organizerId);

    /** 报名人数排行 */
    List<TopActivityVO> selectTopActivities(@Param("organizerId") Integer organizerId,
                                            @Param("limit") int limit);

    /** 某天之后的报名/签到时间，用于 Java 层按天归并出趋势 */
    List<RegistrationTimeVO> selectRegTimes(@Param("organizerId") Integer organizerId,
                                            @Param("start") LocalDateTime start);
}
