package com.campus.activity.service;

import com.campus.activity.common.PageResult;
import com.campus.activity.dto.SysNoticeDTO;
import com.campus.activity.entity.SysNotice;

import java.util.List;

public interface SysNoticeService {

    /** 已发布的公告，首页展示用（所有登录用户可看） */
    List<SysNotice> listPublished();

    /** 管理端分页，含已下架的 */
    PageResult<SysNotice> page(Integer pageNum, Integer pageSize, String keyword, Integer status);

    void create(SysNoticeDTO dto, Integer publisherId);

    void update(Integer id, SysNoticeDTO dto);

    void changeStatus(Integer id, Integer status);

    void delete(Integer id);
}
