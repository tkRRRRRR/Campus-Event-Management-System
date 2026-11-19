package com.campus.activity.service;

import com.campus.activity.common.PageResult;
import com.campus.activity.dto.ReviewDTO;
import com.campus.activity.vo.ReviewStatVO;
import com.campus.activity.vo.ReviewVO;

public interface ReviewService {

    /** 提交评价 */
    void create(Integer activityId, ReviewDTO dto, Integer userId);

    /** 活动评价列表；匿名评价在这里被脱敏 */
    PageResult<ReviewVO> pageByActivity(Integer activityId, Integer pageNum, Integer pageSize,
                                        Integer currentUserId, boolean admin);

    /** 我的评价 */
    PageResult<ReviewVO> myPage(Integer pageNum, Integer pageSize, Integer userId);

    /** 评分统计 */
    ReviewStatVO stat(Integer activityId, Integer currentUserId);

    /** 删除评价：本人或管理员 */
    void delete(Integer reviewId, Integer currentUserId, boolean admin);
}
