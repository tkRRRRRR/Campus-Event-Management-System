package com.campus.activity.service;

import com.campus.activity.common.PageResult;
import com.campus.activity.dto.ActivityAuditDTO;
import com.campus.activity.dto.ActivityQuery;
import com.campus.activity.dto.ActivitySaveDTO;
import com.campus.activity.vo.ActivityDetailVO;
import com.campus.activity.vo.ActivityListVO;

public interface ActivityService {

    /** 分页查询活动列表 */
    PageResult<ActivityListVO> page(ActivityQuery query, Integer currentUserId);

    /** 活动详情，含参与统计与质量分 */
    ActivityDetailVO detail(Integer id, Integer currentUserId);

    /** 组织者发布活动，返回新活动 id */
    Integer create(ActivitySaveDTO dto, Integer organizerId);

    /** 组织者编辑活动（仅草稿或已驳回状态可编辑） */
    void update(ActivitySaveDTO dto, Integer organizerId);

    /** 管理员审核 */
    void audit(Integer id, ActivityAuditDTO dto, Integer adminId);

    /** 组织者取消活动 */
    void cancel(Integer id, Integer organizerId);

    /** 上传活动封面，返回可访问的相对路径 */
    String uploadCover(org.springframework.web.multipart.MultipartFile file);
}
