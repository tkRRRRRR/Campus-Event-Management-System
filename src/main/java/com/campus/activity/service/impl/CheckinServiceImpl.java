package com.campus.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.common.BusinessException;
import com.campus.activity.common.Constants;
import com.campus.activity.common.PageResult;
import com.campus.activity.common.ResultCode;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityRegistration;
import com.campus.activity.entity.Notice;
import com.campus.activity.entity.SysConfig;
import com.campus.activity.entity.SysUser;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityRegistrationMapper;
import com.campus.activity.mapper.NoticeMapper;
import com.campus.activity.mapper.SysConfigMapper;
import com.campus.activity.mapper.SysUserMapper;
import com.campus.activity.service.CheckinService;
import com.campus.activity.vo.CheckinCodeVO;
import com.campus.activity.vo.CheckinStatVO;
import com.campus.activity.vo.RegistrationMemberVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 签到业务实现。
 * 三件事：发签到码（有时效）、核销签到码、活动结束后统一判定爽约。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckinServiceImpl implements CheckinService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** sys_config 里的三项可配置参数，管理员可以在后台改，不用重新打包 */
    private static final String CFG_VIOLATION_LIMIT = "violation_limit";
    private static final String CFG_BAN_DAYS = "ban_days";
    private static final String CFG_CODE_MINUTES = "checkin_code_minutes";

    private final ActivityMapper activityMapper;
    private final ActivityRegistrationMapper registrationMapper;
    private final SysUserMapper userMapper;
    private final NoticeMapper noticeMapper;
    private final SysConfigMapper configMapper;

    /** 签到码有效期（分钟），sys_config 没配时用这个 */
    @Value("${app.checkin.code-minutes:30}")
    private int codeMinutes;

    /** 允许提前多久开始签到（分钟） */
    @Value("${app.checkin.early-minutes:30}")
    private int earlyMinutes;

    /* ==================== 签到码 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckinCodeVO generateCode(Integer activityId, Integer operatorId, boolean admin) {
        Activity activity = checkOwner(activityId, operatorId, admin);
        LocalDateTime now = LocalDateTime.now();
        checkInTimeWindow(activity, now);

        // 已有一个没过期的码就直接复用，避免组织者反复点刷新导致大屏上的码一直变
        if (activity.getCheckinCode() != null && activity.getCodeExpireTime() != null
                && activity.getCodeExpireTime().isAfter(now)) {
            return buildCodeVO(activity, now);
        }

        String code = nextUniqueCode();
        LocalDateTime expire = now.plusMinutes(configInt(CFG_CODE_MINUTES, codeMinutes));
        if (expire.isAfter(activity.getEndTime())) {
            expire = activity.getEndTime();
        }

        activityMapper.update(null, new LambdaUpdateWrapper<Activity>()
                .set(Activity::getCheckinCode, code)
                .set(Activity::getCodeExpireTime, expire)
                .set(Activity::getUpdateTime, now)
                .eq(Activity::getId, activityId));

        activity.setCheckinCode(code);
        activity.setCodeExpireTime(expire);
        log.info("生成签到码: activityId={}, code={}, expire={}", activityId, code, expire);
        return buildCodeVO(activity, now);
    }

    @Override
    public CheckinCodeVO currentCode(Integer activityId, Integer operatorId, boolean admin) {
        Activity activity = checkOwner(activityId, operatorId, admin);
        return buildCodeVO(activity, LocalDateTime.now());
    }

    /* ==================== 学生签到 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkinByCode(Integer activityId, String code, Integer userId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在");
        }
        if (activity.getStatus() != Constants.ACTIVITY_PUBLISHED) {
            throw new BusinessException("活动当前不可签到");
        }

        LocalDateTime now = LocalDateTime.now();
        checkInTimeWindow(activity, now);

        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException("请输入签到码");
        }
        if (activity.getCheckinCode() == null) {
            throw new BusinessException("组织者尚未生成签到码，请稍后再试");
        }
        if (!activity.getCheckinCode().equals(code.trim())) {
            throw new BusinessException("签到码不正确");
        }
        if (activity.getCodeExpireTime() == null || now.isAfter(activity.getCodeExpireTime())) {
            throw new BusinessException("签到码已过期，请向组织者索取新的签到码");
        }

        ActivityRegistration registration = registrationMapper.selectOne(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getActivityId, activityId)
                        .eq(ActivityRegistration::getUserId, userId));
        if (registration == null) {
            throw new BusinessException("你没有报名该活动，无法签到");
        }
        int status = registration.getStatus();
        if (status == Constants.REG_CHECKED_IN) {
            throw new BusinessException("你已经签到过了，无需重复签到");
        }
        if (status == Constants.REG_ABSENT) {
            throw new BusinessException("你已爽约该活动，无法签到");
        }
        if (status == Constants.REG_PENDING) {
            throw new BusinessException("报名还在审核中，审核通过后才能签到");
        }
        if (status != Constants.REG_SIGNED_UP) {
            throw new BusinessException("当前报名状态不可签到");
        }

        // 条件更新：只有还是"已报名未签到"才改成已签到，防止连点两次重复签到
        int affected = registrationMapper.update(null, new LambdaUpdateWrapper<ActivityRegistration>()
                .set(ActivityRegistration::getStatus, Constants.REG_CHECKED_IN)
                .set(ActivityRegistration::getCheckinTime, now)
                .set(ActivityRegistration::getCheckinType, Constants.CHECKIN_BY_CODE)
                .set(ActivityRegistration::getUpdateTime, now)
                .eq(ActivityRegistration::getId, registration.getId())
                .eq(ActivityRegistration::getStatus, Constants.REG_SIGNED_UP));
        if (affected != 1) {
            throw new BusinessException("签到失败，请刷新后重试");
        }
        log.info("签到成功: activityId={}, userId={}", activityId, userId);
    }

    /* ==================== 补签与名单 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualCheckin(Integer registrationId, Integer operatorId, boolean admin) {
        ActivityRegistration registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报名记录不存在");
        }
        Activity activity = checkOwner(registration.getActivityId(), operatorId, admin);
        if (activity.getStatus() == Constants.ACTIVITY_CANCELED) {
            throw new BusinessException("活动已取消，无法补签");
        }

        int status = registration.getStatus();
        if (status != Constants.REG_SIGNED_UP && status != Constants.REG_ABSENT) {
            throw new BusinessException("只有已报名未签到或爽约的记录可以补签");
        }

        LocalDateTime now = LocalDateTime.now();
        int affected = registrationMapper.update(null, new LambdaUpdateWrapper<ActivityRegistration>()
                .set(ActivityRegistration::getStatus, Constants.REG_CHECKED_IN)
                .set(ActivityRegistration::getCheckinTime, now)
                .set(ActivityRegistration::getCheckinType, Constants.CHECKIN_BY_MANUAL)
                .set(ActivityRegistration::getUpdateTime, now)
                .eq(ActivityRegistration::getId, registrationId)
                .eq(ActivityRegistration::getStatus, status));
        if (affected != 1) {
            throw new BusinessException("补签失败，请刷新后重试");
        }

        // 爽约记录被补签：撤销一次爽约计数，人数回到阈值以下就顺带解除封禁
        if (status == Constants.REG_ABSENT) {
            userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                    .setSql("violation_count = violation_count - 1")
                    .set(SysUser::getUpdateTime, now)
                    .eq(SysUser::getId, registration.getUserId())
                    .gt(SysUser::getViolationCount, 0));
            clearBanIfRecovered(registration.getUserId(), now);
        }

        sendNotice(registration.getUserId(), "签到成功",
                "组织者已为你补签「" + activity.getTitle() + "」。", Constants.NOTICE_ACTIVITY);
        log.info("补签成功: registrationId={}, operatorId={}", registrationId, operatorId);
    }

    @Override
    public PageResult<RegistrationMemberVO> memberPage(Integer activityId, Integer status, String keyword,
                                                       Integer pageNum, Integer pageSize,
                                                       Integer operatorId, boolean admin) {
        checkOwner(activityId, operatorId, admin);
        int num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<RegistrationMemberVO> page = new Page<>(num, size);
        registrationMapper.selectMemberList(page, activityId, status, keyword);
        return PageResult.of(page);
    }

    @Override
    public CheckinStatVO stat(Integer activityId, Integer operatorId, boolean admin) {
        checkOwner(activityId, operatorId, admin);
        CheckinStatVO stat = registrationMapper.selectStat(activityId);
        if (stat == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在");
        }
        int checked = stat.getCheckedCount() == null ? 0 : stat.getCheckedCount();
        int absent = stat.getAbsentCount() == null ? 0 : stat.getAbsentCount();
        int waiting = stat.getWaitingCount() == null ? 0 : stat.getWaitingCount();
        int should = checked + absent + waiting;
        stat.setCheckinRate(should == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(checked * 100L)
                        .divide(BigDecimal.valueOf(should), 2, RoundingMode.HALF_UP));
        return stat;
    }

    /* ==================== 结束活动 + 爽约判定 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int finishAndMarkAbsent(Integer activityId, Integer operatorId, boolean admin) {
        Activity activity = checkOwner(activityId, operatorId, admin);
        if (activity.getStatus() != Constants.ACTIVITY_PUBLISHED) {
            throw new BusinessException("只有进行中的活动可以结束");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            throw new BusinessException("活动尚未开始，不能结算爽约");
        }
        return closeActivity(activity, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoFinishExpiredActivities() {
        LocalDateTime now = LocalDateTime.now();
        List<Activity> expired = activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getStatus, Constants.ACTIVITY_PUBLISHED)
                .lt(Activity::getEndTime, now));
        for (Activity activity : expired) {
            closeActivity(activity, now);
        }
        return expired.size();
    }

    /**
     * 结束一场活动：把"已报名未签到"的统一判为爽约，累加违约次数，超过阈值封禁。
     * 事务由调用方控制（本类内部调用不经过代理，所以这里不能再加 @Transactional）。
     */
    private int closeActivity(Activity activity, LocalDateTime now) {
        int closed = activityMapper.update(null, new LambdaUpdateWrapper<Activity>()
                .set(Activity::getStatus, Constants.ACTIVITY_FINISHED)
                .set(Activity::getUpdateTime, now)
                .eq(Activity::getId, activity.getId())
                .eq(Activity::getStatus, Constants.ACTIVITY_PUBLISHED));
        if (closed != 1) return 0;
        List<Integer> absentUserIds = registrationMapper.selectUserIds(
                activity.getId(), Constants.REG_SIGNED_UP);

        int markedAbsent = 0;
        if (!absentUserIds.isEmpty()) {

            int threshold = configInt(CFG_VIOLATION_LIMIT, 3);
            int banDays = configInt(CFG_BAN_DAYS, 30);
            for (Integer userId : absentUserIds) {
                int changed = registrationMapper.update(null, new LambdaUpdateWrapper<ActivityRegistration>()
                        .set(ActivityRegistration::getStatus, Constants.REG_ABSENT)
                        .set(ActivityRegistration::getUpdateTime, now)
                        .eq(ActivityRegistration::getActivityId, activity.getId())
                        .eq(ActivityRegistration::getUserId, userId)
                        .eq(ActivityRegistration::getStatus, Constants.REG_SIGNED_UP));
                if (changed != 1) continue;
                markedAbsent++;
                userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                        .setSql("violation_count = violation_count + 1")
                        .set(SysUser::getUpdateTime, now)
                        .eq(SysUser::getId, userId));

                SysUser user = userMapper.selectById(userId);
                int count = user == null || user.getViolationCount() == null ? 1 : user.getViolationCount();
                if (count >= threshold) {
                    LocalDateTime banUntil = now.plusDays(banDays);
                    userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                            .set(SysUser::getBanUntil, banUntil)
                            .set(SysUser::getUpdateTime, now)
                            .eq(SysUser::getId, userId));
                    sendNotice(userId, "报名受限通知",
                            "你累计 " + count + " 次未签到，「" + activity.getTitle()
                                    + "」的爽约记录已生效，在 " + DATE_FORMAT.format(banUntil)
                                    + " 之前无法报名新活动。",
                            Constants.NOTICE_SYSTEM);
                } else {
                    sendNotice(userId, "爽约提醒",
                            "你报名的「" + activity.getTitle() + "」未签到，已记为爽约。"
                                    + "再爽约 " + (threshold - count) + " 次将被限制报名。",
                            Constants.NOTICE_ACTIVITY);
                }
            }
        }

        log.info("活动结束: activityId={}, 爽约人数={}", activity.getId(), markedAbsent);
        return markedAbsent;
    }

    /** 补签后：违约次数低于阈值就解除封禁 */
    private void clearBanIfRecovered(Integer userId, LocalDateTime now) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getBanUntil() == null || user.getBanUntil().isBefore(now)) {
            return;
        }
        int count = user.getViolationCount() == null ? 0 : user.getViolationCount();
        if (count < configInt(CFG_VIOLATION_LIMIT, 3)) {
            userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                    .set(SysUser::getBanUntil, null)
                    .set(SysUser::getUpdateTime, now)
                    .eq(SysUser::getId, userId));
        }
    }

    /* ==================== 私有方法 ==================== */

    private Activity checkOwner(Integer activityId, Integer operatorId, boolean admin) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在");
        }
        if (!admin && !activity.getOrganizerId().equals(operatorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能操作自己发布的活动");
        }
        return activity;
    }

    /** 签到时间窗：活动开始前 earlyMinutes 分钟开放，活动结束后关闭 */
    private void checkInTimeWindow(Activity activity, LocalDateTime now) {
        if (now.isBefore(activity.getStartTime().minusMinutes(earlyMinutes))) {
            throw new BusinessException("签到尚未开始，活动开始前 " + earlyMinutes + " 分钟开放签到");
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new BusinessException("活动已结束，无法签到");
        }
    }

    private String nextUniqueCode() {
        for (int i = 0; i < 5; i++) {
            String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
            Long used = activityMapper.selectCount(new LambdaQueryWrapper<Activity>()
                    .eq(Activity::getCheckinCode, code)
                    .gt(Activity::getCodeExpireTime, LocalDateTime.now()));
            if (used == null || used == 0) {
                return code;
            }
        }
        // 极端情况下五位都撞了，用毫秒尾数兜底，保证一定生成得出来
        return String.format("%06d", System.currentTimeMillis() % 1000000);
    }

    private CheckinCodeVO buildCodeVO(Activity activity, LocalDateTime now) {
        CheckinCodeVO vo = new CheckinCodeVO();
        vo.setActivityId(activity.getId());
        vo.setCode(activity.getCheckinCode());
        vo.setExpireTime(activity.getCodeExpireTime());
        boolean valid = activity.getCheckinCode() != null
                && activity.getCodeExpireTime() != null
                && activity.getCodeExpireTime().isAfter(now);
        vo.setValid(valid);
        vo.setRemainSeconds(valid ? Duration.between(now, activity.getCodeExpireTime()).getSeconds() : 0);
        return vo;
    }

    private int configInt(String key, int defaultValue) {
        SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key));
        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(config.getConfigValue().trim());
        } catch (NumberFormatException e) {
            log.warn("系统参数 {} 的配置值不是整数: {}", key, config.getConfigValue());
            return defaultValue;
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
