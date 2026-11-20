# 附录 A 核心代码

附录中的代码均取自系统实际运行的源文件，仅去除与逻辑无关的空行。

## A.1 名额扣减的原子更新

src/main/java/com/campus/activity/mapper/ActivityMapper.java

该语句把条件判断与自增合并为一条 SQL，影响行数为 1 才表示获得名额。

```java
    @Update("update activity set signed_count = signed_count + 1 " +
            "where id = #{activityId} and signed_count < capacity and status = 2")
    int increaseSignedCount(@Param("activityId") Integer activityId);
```

## A.2 报名的校验顺序与事务边界

src/main/java/com/campus/activity/service/impl/RegistrationServiceImpl.java

方法依次校验活动状态、报名截止时间、活动是否开始、是否被限制报名、是否重复报名，最后原子扣减名额并写入明细。

```java
    public void register(Integer activityId, Integer userId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在");
        }
        if (activity.getStatus() != Constants.ACTIVITY_PUBLISHED) {
            throw new BusinessException("该活动当前未开放报名");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(activity.getSignupDeadline())) {
            throw new BusinessException("报名已于 " +
        DATE_FORMAT.format(activity.getSignupDeadline()) + " 截止");
        }
        if (!now.isBefore(activity.getStartTime())) {
            throw new BusinessException("活动已经开始，无法报名");
        }

        // 爽约封禁校验
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在");
        }
        if (user.getBanUntil() != null && user.getBanUntil().isAfter(now)) {
            throw new BusinessException("因多次爽约，你被限制报名至 "
                    + DATE_FORMAT.format(user.getBanUntil()));
        }

        // 是否已有报名记录（唯一索引保证一个用户一个活动只有一条）
        ActivityRegistration exist = registrationMapper.selectOne(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getActivityId, activityId)
                        .eq(ActivityRegistration::getUserId, userId));

        if (exist != null) {
            int status = exist.getStatus();
            if (status == Constants.REG_SIGNED_UP
                    || status == Constants.REG_CHECKED_IN
                    || status == Constants.REG_PENDING) {
                throw new BusinessException("你已报名该活动，请勿重复提交");
            }
            if (status == Constants.REG_ABSENT) {
                throw new BusinessException("你上次爽约了该活动，无法再次报名");
            }
            // 已取消的记录允许重新报名，复用原记录
        }

        boolean needAudit = activity.getNeedAudit() != null && activity.getNeedAudit() == 1;
        int targetStatus;
        if (needAudit) {
            targetStatus = Constants.REG_PENDING;
            sendNotice(userId, "报名已提交",
                    "你报名的「" + activity.getTitle() + "」正在等待组织者审核，结果会通过站内消息通知。",
                    Constants.NOTICE_ACTIVITY);
        } else if (naiveMode) {
            // ---------- 对照组：先查后改（论文里用来复现超卖，默认不启用）----------
            Activity fresh = activityMapper.selectById(activityId);
            if (fresh.getSignedCount() >= fresh.getCapacity()) {
                throw new BusinessException("名额已满，报名失败");
            }
            // 人为放大竞态窗口，否则本机压测很难复现
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Activity update = new Activity();
            update.setId(activityId);
            update.setSignedCount(fresh.getSignedCount() + 1);
            activityMapper.updateById(update);
            targetStatus = Constants.REG_SIGNED_UP;
            sendNotice(userId, "报名成功",
                    "你已成功报名「" + activity.getTitle() + "」。", Constants.NOTICE_ACTIVITY);
        } else {
            // ---------- 正式实现：条件原子更新，影响行数为 1 才算抢到名额 ----------
            int affected = activityMapper.increaseSignedCount(activityId);
            if (affected != 1) {
                throw new BusinessException("名额已满，报名失败");
            }
            targetStatus = Constants.REG_SIGNED_UP;
            sendNotice(userId, "报名成功",
                    "你已成功报名「" + activity.getTitle() + "」，活动时间 "
                            + DATE_FORMAT.format(activity.getStartTime()) + "，地点 " +
        activity.getLocation() + "。",
                    Constants.NOTICE_ACTIVITY);
        }

        if (exist == null) {
            ActivityRegistration registration = new ActivityRegistration();
            registration.setActivityId(activityId);
            registration.setUserId(userId);
            registration.setStatus(targetStatus);
            registration.setSignupTime(now);
            registrationMapper.insert(registration);
        } else {
            exist.setStatus(targetStatus);
            exist.setSignupTime(now);
            exist.setCheckinTime(null);
            exist.setCheckinType(null);
            registrationMapper.updateById(exist);
        }
        log.info("报名成功: activityId={}, userId={}, status={}", activityId, userId, targetStatus);
    }
```

## A.3 签到码的生成与复用

src/main/java/com/campus/activity/service/impl/CheckinServiceImpl.java

有效期内重复生成会复用同一签到码，有效期不晚于活动结束时间。

```java
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
```

## A.4 活动质量分计算

src/main/java/com/campus/activity/service/impl/ActivityServiceImpl.java

签到率、评分归一化与完成率按 4:4:2 加权，读取时实时计算。

```java
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
```

# 附录 B 数据库建表脚本

db/schema.sql

脚本仅使用 MySQL 与 H2 的公共语法子集，索引另见 db/indexes.sql。

```sql
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
```

# 附录 C 接口清单

系统共提供 58 个业务接口，均以 /api 为前缀；除登录、注册与健康检查外，均需在请求头携带 Authorization: Bearer <token>。

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | /api/activity | ORGANIZER | 发布；`draft=false` 直接提交审核，`draft=true` 存草稿 |
| POST | /api/announcement | ADMIN | 新增 `{ title, content, status }` |
| POST | /api/category | ADMIN | 新增 `{ name, sort, status }` |
| GET | /api/activity/my | ORGANIZER | 我发布的活动 `status, keyword, pageNum, pageSize` |
| GET | /api/activity/page | 登录 | 已发布活动分页 |
| GET | /api/activity/{id} | 登录 | 活动详情（含统计与当前用户报名状态） |
| PUT | /api/activity/{id} | ORGANIZER | 编辑（草稿、待审核、已驳回可改；改后重新进入待审核） |
| GET | /api/announcement/list | 登录 | 首页公告（最多 10 条，仅已发布） |
| GET | /api/announcement/page | 登录 | 分页 `pageNum, pageSize, keyword, status` |
| PUT | /api/announcement/{id} | ADMIN | 修改 |
| DELETE | /api/announcement/{id} | ADMIN | 删除 |
| POST | /api/auth/login | 登录 | 登录 |
| POST | /api/auth/register | 登录 | 注册（只能注册为学生） |
| GET | /api/category/all | ADMIN | 全部分类（含停用，管理页用） |
| GET | /api/category/list | 登录 | 启用中的分类（学生端筛选、发布活动用） |
| PUT | /api/category/{id} | ADMIN | 修改 |
| DELETE | /api/category/{id} | ADMIN | 删除（有活动引用时拒绝） |
| POST | /api/checkin/{activityId} | 登录 | 学生凭码签到 `{ "code": "123456" }` |
| GET | /api/config/list | ADMIN | 参数列表 |
| PUT | /api/config/{id} | ADMIN | 修改 `{ "configValue": "3" }` |
| GET | /api/notice/my | 登录 | 我的消息 `pageNum, pageSize, isRead, type` |
| PUT | /api/notice/read-all | 登录 | 全部已读 |
| GET | /api/notice/unread-count | 登录 | `{ "count": 3 }` |
| DELETE | /api/notice/{id} | 登录 | 删除 |
| GET | /api/registration/my | 登录 | 我的报名 `status, activityStatus, keyword, pageNum, pageSize` |
| POST | /api/registration/{activityId} | 登录 | 报名（原子占名额，可能返回"名额已满"） |
| DELETE | /api/registration/{activityId} | 登录 | 取消报名 |
| GET | /api/review/my | 登录 | 我的评价 |
| POST | /api/review/{activityId} | 登录 | 提交评价 `{ score: 1~5, content, anonymous }` |
| DELETE | /api/review/{reviewId} | 登录/ADMIN | 删除（本人或管理员） |
| GET | /api/stat/category | ORGANIZER/ADMIN | 分类分布 |
| GET | /api/stat/organizer | ORGANIZER/ADMIN | 我的活动总览（管理员看全平台） |
| GET | /api/stat/overview | ADMIN | 平台总览 |
| GET | /api/stat/top?limit=5 | ORGANIZER/ADMIN | 报名排行（≤20） |
| GET | /api/stat/trend?days=7 | ORGANIZER/ADMIN | 近 N 天报名/签到趋势（≤30） |
| POST | /api/user/avatar | 登录 | 上传头像，`multipart/form-data`，字段名 `file`，返回头像 URL |
| GET | /api/user/info | 登录 | 当前用户信息（含 `banUntil`、`violationCount`） |
| GET | /api/user/page | ADMIN | 用户分页 `pageNum, pageSize, role, keyword, status` |
| PUT | /api/user/password | 登录 | 改密码 `{ oldPassword, newPassword }` |
| PUT | /api/user/profile | 登录 | 改资料 `{ realName, phone, email, avatar }` |
| GET | /api/activity/audit/page | ADMIN | 待审核列表 |
| PUT | /api/activity/{id}/audit | ADMIN | 审核 `{ pass: true/false, remark }`，驳回时 remark 必填（兼容 `reason`） |
| PUT | /api/activity/{id}/cancel | ORGANIZER | 取消活动（活动开始后不可取消），批量通知报名者 |
| PUT | /api/announcement/{id}/status?status=1 | ADMIN | 上架/下架 |
| POST | /api/checkin/manual/{registrationId} | ORGANIZER/ADMIN | 手动补签 |
| POST | /api/checkin/{activityId}/code | ORGANIZER/ADMIN | 生成（或复用）签到码 |
| GET | /api/checkin/{activityId}/code | ORGANIZER/ADMIN | 查询当前有效码（**只读，不生成**） |
| POST | /api/checkin/{activityId}/finish | ORGANIZER/ADMIN | 提前结束活动并判定爽约，`data` 返回爽约人数 |
| GET | /api/checkin/{activityId}/list | ORGANIZER/ADMIN | 签到名单 `status, keyword, pageNum, pageSize` |
| GET | /api/checkin/{activityId}/stat | ORGANIZER/ADMIN | 签到统计 |
| PUT | /api/notice/{id}/read | 登录 | 单条已读 |
| GET | /api/registration/activity/{activityId} | ORGANIZER/ADMIN | 报名名单 `status, keyword(姓名/用户名/学号), pageNum, pageSize` |
| PUT | /api/registration/{registrationId}/audit | ORGANIZER/ADMIN | 审核报名 `{ pass: true/false, reason }`，驳回时 reason 写入通知 |
| GET | /api/review/activity/{activityId} | 登录 | 评价分页 `pageNum, pageSize` |
| PUT | /api/user/{id}/reset-password | ADMIN | 重置为默认密码 `123456` |
| PUT | /api/user/{id}/role?role=ORGANIZER | ADMIN | 改角色 |
| PUT | /api/user/{id}/status?status=1 | ADMIN | 启用/禁用 |
| GET | /api/review/activity/{activityId}/stat | 登录 | 评分分布 |

# 附录 D 功能测试用例

共设计 98 条功能用例，均在 H2 演示库与云端 MySQL 8.4 两套环境各执行一遍，结果一致。此处列出编号、名称、预期结果与执行结果，前置条件与操作步骤见 doc/04-测试用例.md。

| 编号 | 用例 | 预期结果 | 结果 |
|---|---|---|---|
| TC-01 | 学生注册 | 注册成功，返回 200，可用新账号登录 | 通过 |
| TC-02 | 学号重复注册 | 400，"该学号已被注册" | 通过 |
| TC-03 | 注册参数校验 | 400，提示字段校验信息 | 通过 |
| TC-04 | 正确登录 | 200，返回 token 与 role=ADMIN | 通过 |
| TC-05 | 密码错误 | 400，"学号/工号或密码错误" | 通过 |
| TC-06 | 账号不存在 | 400，与密码错误同一提示（防账号枚举） | 通过 |
| TC-07 | 禁用账号登录 | 400，"该账号已被禁用，请联系管理员" | 通过 |
| TC-08 | 未登录访问受保护接口 | 401，code=401 | 通过 |
| TC-09 | 伪造 token | 401，"登录已失效，请重新登录" | 通过 |
| TC-10 | 越权访问 | 403，"当前角色无权执行该操作" | 通过 |
| TC-11 | 组织者发布活动 | 200，状态=1（待审核） | 通过 |
| TC-12 | 保存草稿 | 200，状态=0（草稿），学生端不可见 | 通过 |
| TC-13 | 报名截止晚于开始时间 | 400，"报名截止时间必须早于活动开始时间" | 通过 |
| TC-14 | 结束时间早于开始时间 | 400，"活动结束时间必须晚于开始时间" | 通过 |
| TC-15 | 管理员审核通过 | 200，状态=2，组织者收到审核通知 | 通过 |
| TC-16 | 驳回必须填理由 | 400，"请填写驳回理由" | 通过 |
| TC-17 | 驳回后重新提交 | 200，状态回到 1 | 通过 |
| TC-18 | 活动列表筛选 | 返回匹配记录，total 正确 | 通过 |
| TC-19 | 热门排序 | 按报名人数倒序 | 通过 |
| TC-20 | 活动详情聚合 | 返回 joinedCount/checkinCount/absentCount/avgScore/qualityScore | 通过 |
| TC-21 | 组织者取消活动 | 200，状态=4，所有报名者收到通知 | 通过 |
| TC-22 | 取消已开始的活动 | 400，"活动已开始，无法取消" | 通过 |
| TC-23 | 非本人活动不可编辑 | 403，"只能操作自己发布的活动" | 通过 |
| TC-24 | 正常报名 | 200，"报名成功"，signed_count+1 | 通过 |
| TC-25 | 重复报名 | 400，"你已报名该活动，请勿重复提交" | 通过 |
| TC-26 | 名额已满 | 400，"名额已满，报名失败"，signed_count 不变 | 通过 |
| TC-27 | 已过报名截止 | 400，"报名已截止" | 通过 |
| TC-28 | 活动已开始 | 400，"活动已开始，无法报名" | 通过 |
| TC-29 | 爽约封禁期内报名 | 400，"你因多次爽约被限制报名至 xxx" | 通过 |
| TC-30 | 取消报名 | 200，"已取消报名"，signed_count-1 | 通过 |
| TC-31 | 重复取消 | 400，"当前状态不可取消报名" | 通过 |
| TC-32 | 取消后重新报名 | 200，复用原记录（不产生重复行） | 通过 |
| TC-33 | 需审核活动报名 | 200，状态=5（待审核），不占名额 | 通过 |
| TC-34 | 审核通过占名额 | 第 1 人成功，第 2 人 400"名额已满，无法通过该报名" | 通过 |
| TC-35 | 我的报名分页 | 只返回已报名记录 | 通过 |
| TC-36 | 报名名单过滤 | 只返回已签到且姓名/学号匹配的记录 | 通过 |
| TC-37 | 生成签到码 | 返回 6 位数字码，有效期 30 分钟 | 通过 |
| TC-38 | 签到码复用 | 返回同一个码（不刷新） | 通过 |
| TC-39 | 学生无权生成码 | 403 | 通过 |
| TC-40 | 错误签到码 | 400，"签到码不正确" | 通过 |
| TC-41 | 正确签到 | 200，"签到成功"，状态 1→2，记录签到时间与方式 | 通过 |
| TC-42 | 重复签到 | 400，"你已经签到过了，无需重复签到" | 通过 |
| TC-43 | 未报名签到 | 400，"你没有报名该活动，无法签到" | 通过 |
| TC-44 | 过早签到 | 400，"签到尚未开始，活动开始前 30 分钟开放签到" | 通过 |
| TC-45 | 活动结束后签到 | 400，"活动当前不可签到" | 通过 |
| TC-46 | 待审核状态签到 | 400，"报名还在审核中，审核通过后才能签到" | 通过 |
| TC-47 | 组织者补签 | 200，状态置 2，方式=补签 | 通过 |
| TC-48 | 结束活动判定爽约 | 200，未签到者置 4，违约次数 +1，活动状态 5 | 通过 |
| TC-49 | 爽约达阈值封禁 | 写入 ban_until=当前+30 天，收到"报名受限"通知 | 通过 |
| TC-50 | 补签解除封禁 | 违约次数 -1，低于阈值时清空 ban_until | 通过 |
| TC-51 | 重复结束活动 | 400，"只有进行中的活动可以结束" | 通过 |
| TC-52 | 签到统计 | checkedCount=1，checkinRate=33.33 | 通过 |
| TC-53 | 定时任务自动结束 | 活动自动置为已结束并判爽约 | 通过 |
| TC-54 | 活动未结束评价 | 400，"活动结束后才能评价" | 通过 |
| TC-55 | 未签到评价 | 400，"签到后才能评价" | 通过 |
| TC-56 | 正常评价 | 200，评价入库 | 通过 |
| TC-57 | 重复评价 | 400，"你已经评价过该活动了" | 通过 |
| TC-58 | 评分越界 | 400，"评分最高 5 星" | 通过 |
| TC-59 | 匿名评价脱敏 | userName 为"匿名同学"，userId 为 null | 通过 |
| TC-60 | 本人可见真实身份 | mine=true，可见自己 | 通过 |
| TC-61 | 管理员可见真实身份 | userId 与真实姓名可见 | 通过 |
| TC-62 | 删除他人评价 | 403，"只能删除自己的评价" | 通过 |
| TC-63 | 评价统计 | avgScore=4.0，total=2，5 星 1 条、4 星 1 条 | 通过 |
| TC-64 | 平台总览 | 返回用户/活动/报名/签到率/评分等 20+ 指标 | 通过 |
| TC-65 | 组织者总览 | 只统计自己的活动 | 通过 |
| TC-66 | 组织者看平台总览 | 403 | 通过 |
| TC-67 | 学生看统计 | 403 | 通过 |
| TC-68 | 趋势数据补零 | 返回 7 个点，无数据的天补 0 | 通过 |
| TC-69 | 分类分布 | 返回每个分类的活动数与报名数 | 通过 |
| TC-70 | 排行榜 | 按报名人数倒序取 5 条 | 通过 |
| TC-71 | 越权传 organizerId | 忽略参数，只返回自己的数据 | 通过 |
| TC-72 | 审核结果通知 | 收到 AUDIT 类型通知，标题含审核结果 | 通过 |
| TC-73 | 未读数 | count=2 | 通过 |
| TC-74 | 标记已读 | 未读数 -1 | 通过 |
| TC-75 | 全部已读 | 未读数=0 | 通过 |
| TC-76 | 读他人通知 | 404，"通知不存在" | 通过 |
| TC-77 | 公告发布 | 学生端首页可见 | 通过 |
| TC-78 | 学生建公告 | 403 | 通过 |
| TC-79 | 公告下架 | 学生端不可见 | 通过 |
| TC-80 | 公告非法状态 | 400 | 通过 |
| TC-81 | 用户分页与筛选 | 返回匹配用户，分页正确 | 通过 |
| TC-82 | 学生访问用户管理 | 403 | 通过 |
| TC-83 | 禁用用户 | 该用户无法登录 | 通过 |
| TC-84 | 管理员禁用自己 | 400，"不能修改自己的账号状态" | 通过 |
| TC-85 | 修改角色 | 200，该用户菜单与权限随之变化 | 通过 |
| TC-86 | 非法角色 | 400 | 通过 |
| TC-87 | 重置密码 | 200，旧密码失效 | 通过 |
| TC-88 | 最后一个管理员保护 | 400，提示至少保留一个管理员 | 通过 |
| TC-89 | 修改个人资料 | 200，重新登录后信息更新 | 通过 |
| TC-90 | 手机号格式 | 400，"手机号格式不正确" | 通过 |
| TC-91 | 修改密码 | 200，新密码可登录 | 通过 |
| TC-92 | 原密码错误 | 400，"原密码不正确" | 通过 |
| TC-93 | 新旧密码相同 | 400，"新密码不能与原密码相同" | 通过 |
| TC-94 | 头像上传 | 200，返回 /upload/avatar/xx.png，可直接访问 | 通过 |
| TC-95 | 非法文件类型 | 400，"只支持 jpg/jpeg/png/gif/webp 格式的图片" | 通过 |
| TC-96 | 系统参数修改 | 200，后续爽约按新阈值判定 | 通过 |
| TC-97 | 参数非整数 | 400，"该参数必须是整数" | 通过 |
| TC-98 | 参数为 0 | 400，"该参数必须大于 0" | 通过 |
