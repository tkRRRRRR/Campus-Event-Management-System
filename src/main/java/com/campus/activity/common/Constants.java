package com.campus.activity.common;

/**
 * 系统常量。枚举值直接落库存数字/字符串，避免数据库用 MySQL ENUM 类型（H2 不支持）。
 */
public final class Constants {

    private Constants() {
    }

    /* ---------------- 角色 ---------------- */
    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_ORGANIZER = "ORGANIZER";
    public static final String ROLE_ADMIN = "ADMIN";

    /* ---------------- 账号状态 ---------------- */
    public static final int USER_ENABLED = 1;
    public static final int USER_DISABLED = 0;

    /** 管理员重置密码后使用的初始密码 */
    public static final String DEFAULT_PASSWORD = "123456";

    /* ---------------- 活动状态 ---------------- */
    /** 草稿 */
    public static final int ACTIVITY_DRAFT = 0;
    /** 待审核 */
    public static final int ACTIVITY_PENDING = 1;
    /** 已发布 */
    public static final int ACTIVITY_PUBLISHED = 2;
    /** 已驳回 */
    public static final int ACTIVITY_REJECTED = 3;
    /** 已取消 */
    public static final int ACTIVITY_CANCELED = 4;
    /** 已结束 */
    public static final int ACTIVITY_FINISHED = 5;

    /* ---------------- 报名状态 ---------------- */
    /** 已报名，未签到（占用名额） */
    public static final int REG_SIGNED_UP = 1;
    /** 已签到（占用名额） */
    public static final int REG_CHECKED_IN = 2;
    /** 已取消报名（不占名额） */
    public static final int REG_CANCELED = 3;
    /** 爽约（名额已被消耗，不退还） */
    public static final int REG_ABSENT = 4;
    /** 待组织者审核（暂不占名额，通过后原子占位） */
    public static final int REG_PENDING = 5;

    /* ---------------- 签到方式 ---------------- */
    /** 签到码自助签到 */
    public static final int CHECKIN_BY_CODE = 1;
    /** 组织者手动补签 */
    public static final int CHECKIN_BY_MANUAL = 2;

    /* ---------------- 消息类型 ---------------- */
    public static final String NOTICE_AUDIT = "AUDIT";
    public static final String NOTICE_ACTIVITY = "ACTIVITY";
    public static final String NOTICE_SYSTEM = "SYSTEM";
}
