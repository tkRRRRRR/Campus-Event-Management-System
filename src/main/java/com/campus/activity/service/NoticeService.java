package com.campus.activity.service;

import com.campus.activity.common.PageResult;
import com.campus.activity.entity.Notice;

public interface NoticeService {

    /** 我的消息（只含发给自己的，不含平台公告） */
    PageResult<Notice> myPage(Integer userId, Integer pageNum, Integer pageSize,
                              Integer isRead, String noticeType);

    /** 未读条数，首页红点用 */
    long unreadCount(Integer userId);

    /** 标记单条已读 */
    void markRead(Integer id, Integer userId);

    /** 一键全部已读 */
    void markAllRead(Integer userId);

    /** 删除消息（只能删自己的） */
    void delete(Integer id, Integer userId);

    /** 给某个用户发消息，供其他业务模块调用 */
    void send(Integer userId, String title, String content, String noticeType);
}
