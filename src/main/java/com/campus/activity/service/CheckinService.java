package com.campus.activity.service;

import com.campus.activity.common.PageResult;
import com.campus.activity.vo.CheckinCodeVO;
import com.campus.activity.vo.CheckinStatVO;
import com.campus.activity.vo.RegistrationMemberVO;

/**
 * 签到相关业务。
 * 设计要点：签到码有时效（默认 30 分钟），活动结束后由定时任务统一判定爽约。
 */
public interface CheckinService {

    /** 组织者生成（或复用）签到码 */
    CheckinCodeVO generateCode(Integer activityId, Integer operatorId, boolean admin);

    /** 查看当前签到码 */
    CheckinCodeVO currentCode(Integer activityId, Integer operatorId, boolean admin);

    /** 学生凭签到码签到 */
    void checkinByCode(Integer activityId, String code, Integer userId);

    /** 组织者手动补签 */
    void manualCheckin(Integer registrationId, Integer operatorId, boolean admin);

    /** 签到名单（复用报名名单，按状态过滤，keyword 支持姓名/用户名/学号） */
    PageResult<RegistrationMemberVO> memberPage(Integer activityId, Integer status, String keyword,
                                                Integer pageNum, Integer pageSize,
                                                Integer operatorId, boolean admin);

    /** 签到统计 */
    CheckinStatVO stat(Integer activityId, Integer operatorId, boolean admin);

    /** 组织者提前结束活动，并立即判定爽约，返回爽约人数 */
    int finishAndMarkAbsent(Integer activityId, Integer operatorId, boolean admin);

    /** 定时任务调用：结束所有已过结束时间的活动并判定爽约，返回处理的活动数 */
    int autoFinishExpiredActivities();
}
