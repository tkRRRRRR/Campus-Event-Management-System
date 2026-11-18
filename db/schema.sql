-- ============================================================================
--  校园活动管理系统  建表脚本
--  兼容：MySQL 5.7 / 8.0 与  H2 1.4.200（MODE=MySQL）
--  约定：只使用两套数据库都支持的通用语法，
--        不使用 ENGINE / CHARSET / COMMENT / 内联 KEY 等方言写法
-- ============================================================================

DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(32)  NOT NULL,
    password        VARCHAR(100) NOT NULL,
    real_name       VARCHAR(32)  NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
    student_no      VARCHAR(32),
    phone           VARCHAR(20),
    email           VARCHAR(64),
    avatar          VARCHAR(255),
    status          TINYINT      NOT NULL DEFAULT 1,
    violation_count INT          NOT NULL DEFAULT 0,
    ban_until       DATETIME,
    create_time     DATETIME     NOT NULL,
    update_time     DATETIME     NOT NULL,
    UNIQUE (username)
);

DROP TABLE IF EXISTS activity_category;
CREATE TABLE activity_category (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(32) NOT NULL,
    sort        INT         NOT NULL DEFAULT 0,
    status      TINYINT     NOT NULL DEFAULT 1,
    create_time DATETIME    NOT NULL,
    UNIQUE (name)
);

DROP TABLE IF EXISTS activity;
CREATE TABLE activity (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    title            VARCHAR(100) NOT NULL,
    category_id      INT          NOT NULL,
    cover_url        VARCHAR(255),
    summary          VARCHAR(255),
    content          TEXT,
    location         VARCHAR(100) NOT NULL,
    start_time       DATETIME     NOT NULL,
    end_time         DATETIME     NOT NULL,
    signup_deadline  DATETIME     NOT NULL,
    capacity         INT          NOT NULL DEFAULT 100,
    signed_count     INT          NOT NULL DEFAULT 0,
    status           TINYINT      NOT NULL DEFAULT 0,
    organizer_id     INT          NOT NULL,
    audit_remark     VARCHAR(255),
    checkin_code     VARCHAR(10),
    code_expire_time DATETIME,
    need_audit       TINYINT      NOT NULL DEFAULT 0,
    create_time      DATETIME     NOT NULL,
    update_time      DATETIME     NOT NULL
);

DROP TABLE IF EXISTS activity_registration;
CREATE TABLE activity_registration (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    activity_id  INT          NOT NULL,
    user_id      INT          NOT NULL,
    status       TINYINT      NOT NULL DEFAULT 1,
    signup_time  DATETIME     NOT NULL,
    checkin_time DATETIME,
    checkin_type TINYINT,
    remark       VARCHAR(255),
    update_time  DATETIME     NOT NULL,
    UNIQUE (activity_id, user_id)
);

DROP TABLE IF EXISTS activity_review;
CREATE TABLE activity_review (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    activity_id INT          NOT NULL,
    user_id     INT          NOT NULL,
    score       INT          NOT NULL,
    content     VARCHAR(500),
    anonymous   TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL,
    UNIQUE (activity_id, user_id)
);

DROP TABLE IF EXISTS notice;
CREATE TABLE notice (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT,
    title       VARCHAR(100) NOT NULL,
    content     VARCHAR(500),
    notice_type VARCHAR(32)  NOT NULL DEFAULT 'SYSTEM',
    is_read     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL
);

DROP TABLE IF EXISTS sys_notice;
CREATE TABLE sys_notice (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(100) NOT NULL,
    content      TEXT,
    publisher_id INT          NOT NULL,
    status       TINYINT      NOT NULL DEFAULT 1,
    create_time  DATETIME     NOT NULL,
    update_time  DATETIME     NOT NULL
);

DROP TABLE IF EXISTS sys_config;
CREATE TABLE sys_config (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    config_key   VARCHAR(64)  NOT NULL,
    config_value VARCHAR(255) NOT NULL,
    remark       VARCHAR(255),
    UNIQUE (config_key)
);
