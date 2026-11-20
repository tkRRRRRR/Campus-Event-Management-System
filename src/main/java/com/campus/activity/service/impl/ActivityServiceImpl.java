package com.campus.activity.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.common.BusinessException;
import com.campus.activity.common.Constants;
import com.campus.activity.common.PageResult;
import com.campus.activity.common.ResultCode;
import com.campus.activity.dto.ActivityAuditDTO;
import com.campus.activity.dto.ActivityQuery;
import com.campus.activity.dto.ActivitySaveDTO;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityRegistration;
import com.campus.activity.entity.Notice;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityRegistrationMapper;
import com.campus.activity.mapper.NoticeMapper;
import com.campus.activity.service.ActivityService;
import com.campus.activity.vo.ActivityDetailVO;
import com.campus.activity.vo.ActivityListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private static final List<String> COVER_EXT = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

    private final ActivityMapper activityMapper;
    private final ActivityRegistrationMapper registrationMapper;
    private final NoticeMapper noticeMapper;

    @Value("${app.upload.path:./upload}")
    private String uploadPath;

    @Override
    public PageResult<ActivityListVO> page(ActivityQuery query, Integer currentUserId) {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : Math.min(query.getPageSize(), 50);

        Page<ActivityListVO> page = new Page<>(pageNum, pageSize);
        activityMapper.selectPageVO(page, query, currentUserId);

        for (ActivityListVO vo : page.getRecords()) {
            vo.setRemaining(Math.max(0, vo.getCapacity() - vo.getSignedCount()));
        }
        return PageResult.of(page);
    }

    @Override
    public ActivityDetailVO detail(Integer id, Integer currentUserId) {
        ActivityDetailVO vo = activityMapper.selectDetailVO(id, currentUserId);
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在或已被删除");
        }
        vo.setRemaining(Math.max(0, vo.getCapacity() - vo.getSignedCount()));
        vo.setQualityScore(calcQualityScore(vo));
        return vo;
    }

    /**
     * 质量分 = 签到率 × 40 + 评分归一化 × 40 + 完成率 × 20
     * 签到率 = 已签到 / 报名人数；评分归一化 = (均分 - 1) / 4，把 1~5 星映射到 0~1；
     * 完成率 = 1 - 取消率，即报名后没有取消的比例。
     * 没有报名数据时记 0，避免除零；没有评价时评分项按 0 计算。
     */
    private double calcQualityScore(ActivityDetailVO vo) {
        int joined = vo.getJoinedCount() == null ? 0 : vo.getJoinedCount();
        int checkin = vo.getCheckinCount() == null ? 0 : vo.getCheckinCount();
        int cancel = vo.getCancelCount() == null ? 0 : vo.getCancelCount();
        if (joined == 0) {
            return 0.0;
        }
        double checkinRate = (double) checkin / joined;
        double scoreNorm = vo.getAvgScore() == null ? 0.0 : (vo.getAvgScore() - 1) / 4.0;
        double cancelRate = (double) cancel / (joined + cancel);
        double raw = checkinRate * 40 + scoreNorm * 40 + (1 - cancelRate) * 20;
        return Math.round(raw * 10) / 10.0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer create(ActivitySaveDTO dto, Integer organizerId) {
        validateTime(dto);

        Activity activity = new Activity();
        BeanUtil.copyProperties(dto, activity);
        activity.setId(null);
        activity.setOrganizerId(organizerId);
        activity.setStatus(Boolean.TRUE.equals(dto.getDraft())
                ? Constants.ACTIVITY_DRAFT
                : Constants.ACTIVITY_PENDING);
        activity.setSignedCount(0);
        activity.setNeedAudit(dto.getNeedAudit() == null ? 0 : dto.getNeedAudit());
        activity.setAuditRemark(null);
        activity.setCheckinCode(null);
        activity.setCodeExpireTime(null);

        activityMapper.insert(activity);
        log.info("活动已创建: id={}, title={}, status={}", activity.getId(), activity.getTitle(), activity.getStatus());
        return activity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ActivitySaveDTO dto, Integer organizerId) {
        if (dto.getId() == null) {
            throw new BusinessException("缺少活动 id");
        }
        validateTime(dto);

        Activity old = getOwnedActivity(dto.getId(), organizerId);
        // 草稿可继续改、待审核可改后重新提交、被驳回可修改后重新提交
        if (old.getStatus() != Constants.ACTIVITY_DRAFT
                && old.getStatus() != Constants.ACTIVITY_PENDING
                && old.getStatus() != Constants.ACTIVITY_REJECTED) {
            throw new BusinessException("只有草稿、待审核或已驳回的活动才能编辑");
        }
        // 已经有人报名时不允许把人数上限改到低于已报名人数
        if (dto.getCapacity() < old.getSignedCount()) {
            throw new BusinessException("人数上限不能小于当前已报名人数 " + old.getSignedCount());
        }

        Activity activity = new Activity();
        BeanUtil.copyProperties(dto, activity);
        activity.setId(old.getId());
        activity.setOrganizerId(old.getOrganizerId());
        activity.setSignedCount(old.getSignedCount());
        activity.setStatus(Boolean.TRUE.equals(dto.getDraft())
                ? Constants.ACTIVITY_DRAFT
                : Constants.ACTIVITY_PENDING);
        activity.setAuditRemark(null);

        activityMapper.updateById(activity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Integer id, ActivityAuditDTO dto, Integer adminId) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在");
        }
        if (activity.getStatus() != Constants.ACTIVITY_PENDING) {
            throw new BusinessException("该活动当前不处于待审核状态");
        }
        if (!Boolean.TRUE.equals(dto.getPass()) && (dto.getRemark() == null || dto.getRemark().trim().isEmpty())) {
            throw new BusinessException("驳回时必须填写审核意见");
        }

        Activity update = new Activity();
        update.setId(id);
        if (Boolean.TRUE.equals(dto.getPass())) {
            update.setStatus(Constants.ACTIVITY_PUBLISHED);
            update.setAuditRemark(dto.getRemark());
            sendNotice(activity.getOrganizerId(),
                    "活动审核通过",
                    "你提交的「" + activity.getTitle() + "」已通过审核，现已发布。",
                    Constants.NOTICE_AUDIT);
        } else {
            update.setStatus(Constants.ACTIVITY_REJECTED);
            update.setAuditRemark(dto.getRemark());
            sendNotice(activity.getOrganizerId(),
                    "活动审核未通过",
                    "你提交的「" + activity.getTitle() + "」未通过审核，原因：" + dto.getRemark(),
                    Constants.NOTICE_AUDIT);
        }
        activityMapper.updateById(update);
        log.info("活动审核完成: id={}, adminId={}, pass={}", id, adminId, dto.getPass());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Integer id, Integer organizerId) {
        Activity activity = getOwnedActivity(id, organizerId);
        if (activity.getStatus() != Constants.ACTIVITY_PUBLISHED) {
            throw new BusinessException("只有已发布的活动才能取消");
        }
        if (activity.getStartTime() != null && !activity.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("活动已开始，无法取消");
        }

        Activity update = new Activity();
        update.setId(id);
        update.setStatus(Constants.ACTIVITY_CANCELED);
        activityMapper.updateById(update);

        // 通知所有有效报名者
        List<ActivityRegistration> registrations = registrationMapper.selectList(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getActivityId, id)
                        .in(ActivityRegistration::getStatus,
                                Constants.REG_SIGNED_UP, Constants.REG_CHECKED_IN, Constants.REG_PENDING));
        for (ActivityRegistration registration : registrations) {
            sendNotice(registration.getUserId(),
                    "活动已取消",
                    "你报名的「" + activity.getTitle() + "」已由组织者取消，请留意后续安排。",
                    Constants.NOTICE_ACTIVITY);
        }
        log.info("活动已取消: id={}, 通知报名者 {} 人", id, registrations.size());
    }

    /* ---------------- 私有方法 ---------------- */

    @Override
    public String uploadCover(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot < 0 ? "" : original.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!COVER_EXT.contains(ext)) {
            throw new BusinessException("只支持 jpg/jpeg/png/gif/webp 格式的图片");
        }
        if (file.getSize() > 5 * 1024 * 1024L) {
            throw new BusinessException("图片不能超过 5MB");
        }
        // transferTo 遇到相对路径会落到 Tomcat 临时目录下，这里一律转成绝对路径
        File dir = new File(uploadPath, "cover").getAbsoluteFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BusinessException("创建封面目录失败");
        }
        String filename = "cover_" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            file.transferTo(new File(dir, filename));
        } catch (IOException e) {
            log.error("封面保存失败", e);
            throw new BusinessException("封面保存失败，请稍后重试");
        }
        return "/upload/cover/" + filename;
    }

    private Activity getOwnedActivity(Integer id, Integer organizerId) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在");
        }
        if (!activity.getOrganizerId().equals(organizerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能操作自己发布的活动");
        }
        return activity;
    }

    private void validateTime(ActivitySaveDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        if (dto.getSignupDeadline().isAfter(dto.getStartTime())) {
            throw new BusinessException("报名截止时间不能晚于活动开始时间");
        }
        if (dto.getStartTime().isBefore(now)) {
            throw new BusinessException("活动开始时间不能早于当前时间");
        }
    }

    private void sendNotice(Integer userId, String title, String content, String type) {
        Notice notice = new Notice();
        notice.setUserId(userId);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setNoticeType(type);
        notice.setIsRead(0);
        noticeMapper.insert(notice);
    }
}
