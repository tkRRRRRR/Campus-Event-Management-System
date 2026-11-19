package com.campus.activity.service;

import com.campus.activity.common.PageResult;
import com.campus.activity.dto.RegistrationQuery;
import com.campus.activity.vo.MyRegistrationVO;
import com.campus.activity.vo.RegistrationMemberVO;

public interface RegistrationService {

    /** 学生报名 */
    void register(Integer activityId, Integer userId);

    /** 学生取消报名 */
    void cancel(Integer activityId, Integer userId);

    /** 我的报名 */
    PageResult<MyRegistrationVO> myPage(RegistrationQuery query, Integer userId);

    /** 某活动的报名名单（组织者本人或管理员） */
    PageResult<RegistrationMemberVO> memberPage(Integer activityId, RegistrationQuery query,
                                                Integer currentUserId, boolean admin);

    /** 组织者审核报名（活动开启"报名需审核"时才会走到这里） */
    void auditSignup(Integer registrationId, boolean pass, Integer organizerId);
}
