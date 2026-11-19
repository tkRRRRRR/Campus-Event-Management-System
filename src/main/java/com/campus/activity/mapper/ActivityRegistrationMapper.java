package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.dto.RegistrationQuery;
import com.campus.activity.entity.ActivityRegistration;
import com.campus.activity.vo.CheckinStatVO;
import com.campus.activity.vo.MyRegistrationVO;
import com.campus.activity.vo.RegistrationMemberVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ActivityRegistrationMapper extends BaseMapper<ActivityRegistration> {

    /** 我的报名列表 */
    Page<MyRegistrationVO> selectMyPage(Page<?> page,
                                        @Param("userId") Integer userId,
                                        @Param("q") RegistrationQuery query);

    /** 某活动的报名名单 */
    Page<RegistrationMemberVO> selectMemberPage(Page<?> page,
                                                @Param("activityId") Integer activityId,
                                                @Param("q") RegistrationQuery query);

    /** 签到名单：只按状态过滤，不参与分组统计 */
    Page<RegistrationMemberVO> selectMemberList(Page<?> page,
                                                @Param("activityId") Integer activityId,
                                                @Param("status") Integer status,
                                                @Param("keyword") String keyword);

    /** 某活动某状态下所有报名用户 id，用于批量判定爽约 */
    @Select("select user_id from activity_registration " +
            "where activity_id = #{activityId} and status = #{status} order by id")
    List<Integer> selectUserIds(@Param("activityId") Integer activityId,
                                @Param("status") Integer status);

    /** 签到统计：子查询分别数各种状态的人数 */
    @Select("select a.id as activity_id, a.capacity, a.signed_count, " +
            "(select count(*) from activity_registration r where r.activity_id = a.id and r.status = 2) as checked_count, " +
            "(select count(*) from activity_registration r where r.activity_id = a.id and r.status = 4) as absent_count, " +
            "(select count(*) from activity_registration r where r.activity_id = a.id and r.status = 1) as waiting_count, " +
            "(select count(*) from activity_registration r where r.activity_id = a.id and r.status = 5) as pending_count, " +
            "(select count(*) from activity_registration r where r.activity_id = a.id and r.status = 3) as cancel_count " +
            "from activity a where a.id = #{activityId}")
    CheckinStatVO selectStat(@Param("activityId") Integer activityId);
}
