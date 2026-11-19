package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.entity.ActivityReview;
import com.campus.activity.vo.ReviewStatVO;
import com.campus.activity.vo.ReviewVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ActivityReviewMapper extends BaseMapper<ActivityReview> {

    /** 某活动的评价列表（联表拿活动标题和评价人姓名） */
    Page<ReviewVO> selectPageVO(Page<?> page, @Param("activityId") Integer activityId);

    /** 我的评价列表 */
    Page<ReviewVO> selectMyPage(Page<?> page, @Param("userId") Integer userId);

    /** 评分统计：只做聚合，不返回任何个人信息 */
    @Select("select count(*) as total, " +
            "coalesce(avg(score), 0) as avg_score, " +
            "sum(case when score = 5 then 1 else 0 end) as score5, " +
            "sum(case when score = 4 then 1 else 0 end) as score4, " +
            "sum(case when score = 3 then 1 else 0 end) as score3, " +
            "sum(case when score = 2 then 1 else 0 end) as score2, " +
            "sum(case when score = 1 then 1 else 0 end) as score1 " +
            "from activity_review where activity_id = #{activityId}")
    ReviewStatVO selectStat(@Param("activityId") Integer activityId);
}
