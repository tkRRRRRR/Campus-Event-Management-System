package com.campus.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.common.BusinessException;
import com.campus.activity.common.PageResult;
import com.campus.activity.common.ResultCode;
import com.campus.activity.dto.SysNoticeDTO;
import com.campus.activity.entity.SysNotice;
import com.campus.activity.mapper.SysNoticeMapper;
import com.campus.activity.service.SysNoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysNoticeServiceImpl implements SysNoticeService {

    private final SysNoticeMapper sysNoticeMapper;

    @Override
    public List<SysNotice> listPublished() {
        return sysNoticeMapper.selectList(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getStatus, 1)
                .orderByDesc(SysNotice::getCreateTime)
                .orderByDesc(SysNotice::getId)
                .last("limit 10"));
    }

    @Override
    public PageResult<SysNotice> page(Integer pageNum, Integer pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(SysNotice::getTitle, keyword.trim());
        }
        if (status != null) {
            wrapper.eq(SysNotice::getStatus, status);
        }
        wrapper.orderByDesc(SysNotice::getCreateTime).orderByDesc(SysNotice::getId);
        Page<SysNotice> page = new Page<>(pageNum == null || pageNum < 1 ? 1 : pageNum,
                pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50));
        sysNoticeMapper.selectPage(page, wrapper);
        return PageResult.of(page);
    }

    @Override
    public void create(SysNoticeDTO dto, Integer publisherId) {
        SysNotice notice = new SysNotice();
        notice.setTitle(dto.getTitle().trim());
        notice.setContent(dto.getContent().trim());
        notice.setPublisherId(publisherId);
        notice.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        sysNoticeMapper.insert(notice);
        log.info("公告已发布: id={}, title={}", notice.getId(), notice.getTitle());
    }

    @Override
    public void update(Integer id, SysNoticeDTO dto) {
        SysNotice old = getExist(id);
        SysNotice notice = new SysNotice();
        notice.setId(old.getId());
        notice.setTitle(dto.getTitle().trim());
        notice.setContent(dto.getContent().trim());
        if (dto.getStatus() != null) {
            notice.setStatus(dto.getStatus());
        }
        sysNoticeMapper.updateById(notice);
    }

    @Override
    public void changeStatus(Integer id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态只能是 0（下架）或 1（发布）");
        }
        getExist(id);
        SysNotice notice = new SysNotice();
        notice.setId(id);
        notice.setStatus(status);
        sysNoticeMapper.updateById(notice);
    }

    @Override
    public void delete(Integer id) {
        getExist(id);
        sysNoticeMapper.deleteById(id);
    }

    private SysNotice getExist(Integer id) {
        SysNotice notice = sysNoticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        return notice;
    }
}
