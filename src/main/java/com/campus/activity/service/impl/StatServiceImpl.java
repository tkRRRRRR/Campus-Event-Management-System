package com.campus.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.activity.common.Constants;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.SysUser;
import com.campus.activity.mapper.ActivityCategoryMapper;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.StatMapper;
import com.campus.activity.mapper.SysUserMapper;
import com.campus.activity.service.StatService;
import com.campus.activity.vo.CategoryStatVO;
import com.campus.activity.vo.OrganizerStatVO;
import com.campus.activity.vo.PlatformStatVO;
import com.campus.activity.vo.RegistrationAggVO;
import com.campus.activity.vo.RegistrationTimeVO;
import com.campus.activity.vo.ReviewAggVO;
import com.campus.activity.vo.TopActivityVO;
import com.campus.activity.vo.TrendPointVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("MM-dd");
    private static final int TREND_MAX_DAYS = 30;
    private static final int TOP_MAX = 20;

    private final StatMapper statMapper;
    private final SysUserMapper userMapper;
    private final ActivityMapper activityMapper;
    private final ActivityCategoryMapper categoryMapper;

    @Override
    public PlatformStatVO overview() {
        PlatformStatVO vo = new PlatformStatVO();

        vo.setUserCount(countUser(null));
        vo.setStudentCount(countUser(Constants.ROLE_STUDENT));
        vo.setOrganizerCount(countUser(Constants.ROLE_ORGANIZER));
        vo.setBannedUserCount(userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .gt(SysUser::getBanUntil, LocalDateTime.now())).intValue());

        vo.setActivityCount(countActivity(null, null));
        vo.setDraftCount(countActivity(Constants.ACTIVITY_DRAFT, null));
        vo.setPendingCount(countActivity(Constants.ACTIVITY_PENDING, null));
        vo.setPublishedCount(countActivity(Constants.ACTIVITY_PUBLISHED, null));
        vo.setFinishedCount(countActivity(Constants.ACTIVITY_FINISHED, null));
        vo.setCanceledCount(countActivity(Constants.ACTIVITY_CANCELED, null));

        RegistrationAggVO agg = statMapper.selectRegAgg(null);
        fillReg(vo, agg);
        vo.setCheckinRate(checkinRate(agg));

        ReviewAggVO review = statMapper.selectReviewAgg(null);
        vo.setReviewCount(review == null || review.getTotal() == null ? 0 : review.getTotal());
        vo.setAvgScore(scale(review == null ? null : review.getAvgScore()));

        vo.setCategoryCount(categoryMapper.selectCount(null).intValue());
        return vo;
    }

    @Override
    public OrganizerStatVO organizer(Integer organizerId) {
        OrganizerStatVO vo = new OrganizerStatVO();

        vo.setActivityCount(countActivity(null, organizerId));
        vo.setDraftCount(countActivity(Constants.ACTIVITY_DRAFT, organizerId));
        vo.setPendingCount(countActivity(Constants.ACTIVITY_PENDING, organizerId));
        vo.setPublishedCount(countActivity(Constants.ACTIVITY_PUBLISHED, organizerId));
        vo.setFinishedCount(countActivity(Constants.ACTIVITY_FINISHED, organizerId));
        vo.setCanceledCount(countActivity(Constants.ACTIVITY_CANCELED, organizerId));

        RegistrationAggVO agg = statMapper.selectRegAgg(organizerId);
        vo.setRegistrationCount(agg == null || agg.getTotal() == null ? 0 : agg.getTotal());
        vo.setPendingRegCount(nvl(agg == null ? null : agg.getPending()));
        vo.setSignedUpCount(nvl(agg == null ? null : agg.getSignedUp()));
        vo.setCheckedCount(nvl(agg == null ? null : agg.getChecked()));
        vo.setCancelRegCount(nvl(agg == null ? null : agg.getCanceled()));
        vo.setAbsentCount(nvl(agg == null ? null : agg.getAbsent()));
        vo.setCheckinRate(checkinRate(agg));

        ReviewAggVO review = statMapper.selectReviewAgg(organizerId);
        vo.setReviewCount(review == null || review.getTotal() == null ? 0 : review.getTotal());
        vo.setAvgScore(scale(review == null ? null : review.getAvgScore()));
        return vo;
    }

    @Override
    public List<CategoryStatVO> category(Integer organizerId) {
        return statMapper.selectCategoryStat(organizerId);
    }

    @Override
    public List<TrendPointVO> trend(int days, Integer organizerId) {
        int span = Math.max(1, Math.min(days, TREND_MAX_DAYS));
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(span - 1L);

        // 先把空的天补齐，前端 X 轴才不会缺格子
        Map<LocalDate, TrendPointVO> buckets = new HashMap<>();
        for (int i = 0; i < span; i++) {
            LocalDate day = from.plusDays(i);
            TrendPointVO point = new TrendPointVO();
            point.setDate(day.format(DAY_FORMAT));
            buckets.put(day, point);
        }

        List<RegistrationTimeVO> rows = statMapper.selectRegTimes(
                organizerId, from.atStartOfDay());
        for (RegistrationTimeVO row : rows) {
            if (row.getSignupTime() != null) {
                TrendPointVO point = buckets.get(row.getSignupTime().toLocalDate());
                if (point != null) {
                    point.setRegistrationCount(point.getRegistrationCount() + 1);
                }
            }
            if (row.getCheckinTime() != null) {
                TrendPointVO point = buckets.get(row.getCheckinTime().toLocalDate());
                if (point != null) {
                    point.setCheckinCount(point.getCheckinCount() + 1);
                }
            }
        }

        List<TrendPointVO> result = new ArrayList<>(span);
        for (int i = 0; i < span; i++) {
            result.add(buckets.get(from.plusDays(i)));
        }
        return result;
    }

    @Override
    public List<TopActivityVO> top(int limit, Integer organizerId) {
        int size = Math.max(1, Math.min(limit, TOP_MAX));
        return statMapper.selectTopActivities(organizerId, size);
    }

    /* ---------------- 私有方法 ---------------- */

    private int countUser(String role) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, Constants.USER_ENABLED);
        if (role != null) {
            wrapper.eq(SysUser::getRole, role);
        }
        return userMapper.selectCount(wrapper).intValue();
    }

    private int countActivity(Integer status, Integer organizerId) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Activity::getStatus, status);
        }
        if (organizerId != null) {
            wrapper.eq(Activity::getOrganizerId, organizerId);
        }
        return activityMapper.selectCount(wrapper).intValue();
    }

    /** 平台概览和组织者看板字段一致，抽出来复用 */
    private void fillReg(PlatformStatVO vo, RegistrationAggVO agg) {
        vo.setRegistrationCount(agg == null || agg.getTotal() == null ? 0 : agg.getTotal());
        vo.setPendingRegCount(nvl(agg == null ? null : agg.getPending()));
        vo.setSignedUpCount(nvl(agg == null ? null : agg.getSignedUp()));
        vo.setCheckedCount(nvl(agg == null ? null : agg.getChecked()));
        vo.setCancelRegCount(nvl(agg == null ? null : agg.getCanceled()));
        vo.setAbsentCount(nvl(agg == null ? null : agg.getAbsent()));
    }

    /**
     * 签到率 = 已签到 / (已签到 + 爽约 + 已报名未签到)。
     * 已取消报名和被驳回的活动都不进分母，否则一场取消率高的活动会显得签到率很低。
     */
    private BigDecimal checkinRate(RegistrationAggVO agg) {
        if (agg == null) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        int checked = nvl(agg.getChecked());
        int absent = nvl(agg.getAbsent());
        int signedUp = nvl(agg.getSignedUp());
        int denominator = checked + absent + signedUp;
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(checked * 100.0 / denominator).setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP)
                : value.setScale(1, RoundingMode.HALF_UP);
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
