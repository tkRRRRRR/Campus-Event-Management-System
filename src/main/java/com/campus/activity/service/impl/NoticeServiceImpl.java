package com.campus.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.common.BusinessException;
import com.campus.activity.common.PageResult;
import com.campus.activity.common.ResultCode;
import com.campus.activity.entity.Notice;
import com.campus.activity.mapper.NoticeMapper;
import com.campus.activity.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeMapper noticeMapper;

    @Override
    public PageResult<Notice> myPage(Integer userId, Integer pageNum, Integer pageSize,
                                     Integer isRead, String noticeType) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<Notice>()
                .eq(Notice::getUserId, userId)
                .orderByDesc(Notice::getCreateTime)
                .orderByDesc(Notice::getId);
        if (isRead != null) {
            wrapper.eq(Notice::getIsRead, isRead);
        }
        if (noticeType != null && !noticeType.trim().isEmpty()) {
            wrapper.eq(Notice::getNoticeType, noticeType.trim());
        }
        Page<Notice> page = new Page<>(pageNorm(pageNum), sizeNorm(pageSize));
        noticeMapper.selectPage(page, wrapper);
        return PageResult.of(page);
    }

    @Override
    public long unreadCount(Integer userId) {
        Long count = noticeMapper.selectCount(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getUserId, userId)
                .eq(Notice::getIsRead, 0));
        return count == null ? 0L : count;
    }

    @Override
    public void markRead(Integer id, Integer userId) {
        Notice notice = getOwned(id, userId);
        if (notice.getIsRead() != null && notice.getIsRead() == 1) {
            return;
        }
        noticeMapper.update(null, new LambdaUpdateWrapper<Notice>()
                .set(Notice::getIsRead, 1)
                .eq(Notice::getId, id)
                .eq(Notice::getUserId, userId));
    }

    @Override
    public void markAllRead(Integer userId) {
        noticeMapper.update(null, new LambdaUpdateWrapper<Notice>()
                .set(Notice::getIsRead, 1)
                .eq(Notice::getUserId, userId)
                .eq(Notice::getIsRead, 0));
    }

    @Override
    public void delete(Integer id, Integer userId) {
        getOwned(id, userId);
        noticeMapper.deleteById(id);
    }

    @Override
    public void send(Integer userId, String title, String content, String noticeType) {
        Notice notice = new Notice();
        notice.setUserId(userId);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setNoticeType(noticeType);
        notice.setIsRead(0);
        notice.setCreateTime(LocalDateTime.now());
        noticeMapper.insert(notice);
    }

    /* ---------------- 私有方法 ---------------- */

    private Notice getOwned(Integer id, Integer userId) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "消息不存在");
        }
        if (!userId.equals(notice.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能操作自己的消息");
        }
        return notice;
    }

    private int pageNorm(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int sizeNorm(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
    }
}
