package com.campus.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.common.BusinessException;
import com.campus.activity.common.Constants;
import com.campus.activity.common.PageResult;
import com.campus.activity.common.ResultCode;
import com.campus.activity.dto.ReviewDTO;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityRegistration;
import com.campus.activity.entity.ActivityReview;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityRegistrationMapper;
import com.campus.activity.mapper.ActivityReviewMapper;
import com.campus.activity.service.ReviewService;
import com.campus.activity.vo.ReviewStatVO;
import com.campus.activity.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final String ANONYMOUS_NAME = "匿名同学";

    private final ActivityMapper activityMapper;
    private final ActivityRegistrationMapper registrationMapper;
    private final ActivityReviewMapper reviewMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(Integer activityId, ReviewDTO dto, Integer userId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在");
        }
        if (activity.getStatus() != Constants.ACTIVITY_FINISHED) {
            throw new BusinessException("活动结束后才能评价");
        }

        ActivityRegistration registration = registrationMapper.selectOne(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getActivityId, activityId)
                        .eq(ActivityRegistration::getUserId, userId));
        if (registration == null) {
            throw new BusinessException("你没有参加该活动，无法评价");
        }
        if (registration.getStatus() == Constants.REG_ABSENT) {
            throw new BusinessException("你未签到该活动，无法评价");
        }
        if (registration.getStatus() != Constants.REG_CHECKED_IN) {
            throw new BusinessException("签到后才能评价该活动");
        }

        ActivityReview exist = reviewMapper.selectOne(new LambdaQueryWrapper<ActivityReview>()
                .eq(ActivityReview::getActivityId, activityId)
                .eq(ActivityReview::getUserId, userId));
        if (exist != null) {
            throw new BusinessException("你已经评价过该活动了");
        }

        ActivityReview review = new ActivityReview();
        review.setActivityId(activityId);
        review.setUserId(userId);
        review.setScore(dto.getScore());
        review.setContent(dto.getContent() == null ? "" : dto.getContent().trim());
        review.setAnonymous(Boolean.TRUE.equals(dto.getAnonymous()) ? 1 : 0);
        reviewMapper.insert(review);
        log.info("新增评价: activityId={}, userId={}, score={}", activityId, userId, dto.getScore());
    }

    @Override
    public PageResult<ReviewVO> pageByActivity(Integer activityId, Integer pageNum, Integer pageSize,
                                               Integer currentUserId, boolean admin) {
        Page<ReviewVO> page = new Page<>(pageNorm(pageNum), sizeNorm(pageSize));
        reviewMapper.selectPageVO(page, activityId);

        List<ReviewVO> records = page.getRecords();
        for (ReviewVO vo : records) {
            maskIfAnonymous(vo, currentUserId, admin);
        }
        return PageResult.of(page);
    }

    @Override
    public PageResult<ReviewVO> myPage(Integer pageNum, Integer pageSize, Integer userId) {
        Page<ReviewVO> page = new Page<>(pageNorm(pageNum), sizeNorm(pageSize));
        reviewMapper.selectMyPage(page, userId);
        for (ReviewVO vo : page.getRecords()) {
            vo.setMine(true);
        }
        return PageResult.of(page);
    }

    @Override
    public ReviewStatVO stat(Integer activityId, Integer currentUserId) {
        if (activityMapper.selectById(activityId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在");
        }
        ReviewStatVO stat = reviewMapper.selectStat(activityId);
        if (stat == null) {
            stat = new ReviewStatVO();
        }
        stat.setActivityId(activityId);
        int total = stat.getTotal() == null ? 0 : stat.getTotal();
        stat.setTotal(total);
        stat.setScore5(zeroIfNull(stat.getScore5()));
        stat.setScore4(zeroIfNull(stat.getScore4()));
        stat.setScore3(zeroIfNull(stat.getScore3()));
        stat.setScore2(zeroIfNull(stat.getScore2()));
        stat.setScore1(zeroIfNull(stat.getScore1()));
        stat.setAvgScore(stat.getAvgScore() == null
                ? BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP)
                : stat.getAvgScore().setScale(1, RoundingMode.HALF_UP));

        ActivityReview mine = reviewMapper.selectOne(new LambdaQueryWrapper<ActivityReview>()
                .eq(ActivityReview::getActivityId, activityId)
                .eq(ActivityReview::getUserId, currentUserId));
        if (mine != null) {
            ReviewVO vo = new ReviewVO();
            vo.setId(mine.getId());
            vo.setActivityId(activityId);
            vo.setUserId(mine.getUserId());
            vo.setScore(mine.getScore());
            vo.setContent(mine.getContent());
            vo.setAnonymous(mine.getAnonymous());
            vo.setCreateTime(mine.getCreateTime());
            vo.setMine(true);
            stat.setMyReview(vo);
        }
        return stat;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer reviewId, Integer currentUserId, boolean admin) {
        ActivityReview review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }
        if (!admin && !review.getUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己的评价");
        }
        reviewMapper.deleteById(reviewId);
        log.info("删除评价: reviewId={}, operator={}", reviewId, currentUserId);
    }

    /* ---------------- 私有方法 ---------------- */

    /** 匿名评价脱敏：非本人且非管理员，一律不返回真实姓名和用户 id */
    private void maskIfAnonymous(ReviewVO vo, Integer currentUserId, boolean admin) {
        boolean mine = currentUserId != null && currentUserId.equals(vo.getUserId());
        vo.setMine(mine);
        if (vo.getAnonymous() != null && vo.getAnonymous() == 1 && !mine && !admin) {
            vo.setUserName(ANONYMOUS_NAME);
            vo.setUserId(null);
        }
    }

    private int pageNorm(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int sizeNorm(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
    }

    private int zeroIfNull(Integer value) {
        return value == null ? 0 : value;
    }
}
