package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.dto.ActivityQuery;
import com.campus.activity.entity.Activity;
import com.campus.activity.vo.ActivityDetailVO;
import com.campus.activity.vo.ActivityListVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ActivityMapper extends BaseMapper<Activity> {

    /**
     * 原子扣减名额：影响行数为 1 才代表抢到名额。
     * 这条 SQL 是整个系统防止超卖的核心，放在 Mapper 层是为了避免 select + update 之间的竞态。
     */
    @Update("update activity set signed_count = signed_count + 1 " +
            "where id = #{activityId} and signed_count < capacity and status = 2")
    int increaseSignedCount(@Param("activityId") Integer activityId);

    /**
     * 原子归还名额：取消报名时调用。
     */
    @Update("update activity set signed_count = signed_count - 1 " +
            "where id = #{activityId} and signed_count > 0")
    int decreaseSignedCount(@Param("activityId") Integer activityId);

    /** 分页查询（联表，带当前用户报名状态） */
    Page<ActivityListVO> selectPageVO(Page<?> page,
                                      @Param("q") ActivityQuery query,
                                      @Param("userId") Integer userId);

    /** 详情（含统计数据） */
    ActivityDetailVO selectDetailVO(@Param("id") Integer id, @Param("userId") Integer userId);
}
