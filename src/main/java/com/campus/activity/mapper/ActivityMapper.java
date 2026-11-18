package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.entity.Activity;
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
}
