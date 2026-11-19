<#
    生成演示数据 db/data.sql
    为什么要脚本生成：报名表要 200 多条，而且 activity.signed_count 必须与
    报名明细严格相等 —— 手写容易对不上，脚本生成可以保证一致性。
    用法： powershell -File scripts/gen-demo-data.ps1
#>
$ErrorActionPreference = 'Stop'
$out = Join-Path (Split-Path -Parent $PSScriptRoot) 'db\data.sql'

$sb = New-Object System.Text.StringBuilder
function W($line) { [void]$sb.AppendLine($line) }

# ---------- 学生名单：10 姓 × 7 名 = 70 人 ----------
$surnames = @('李', '王', '张', '刘', '陈', '杨', '赵', '黄', '周', '吴')
$givens = @('思远', '嘉怡', '明轩', '欣怡', '宇航', '晓彤', '文博')

# ---------- 报名计划：活动 id -> 报名的学生序号 ----------
# 与 activity.signed_count 严格对应，改这里必须同步改 signed_count
$plan = @(
    @{ activity = 1; from = 1;  to = 36;  status = 1; withUser3 = $true }   # 王雨桐 + 36 人 = 37
    @{ activity = 2; from = 1;  to = 56;  status = 1; withUser3 = $true }   # 王雨桐 + 56 人 + 陈子豪 = 58
    @{ activity = 3; from = 1;  to = 60;  status = 1; withUser3 = $true }   # 王雨桐 + 60 人 = 61
    @{ activity = 4; from = 1;  to = 12;  status = 1; withUser3 = $false }  # 12 人
)
# 已结束活动 5：46 人报名，其中 41 人签到、5 人爽约
$plan5Checked = 1..40
$plan5Absent = 41..44

W '-- ============================================================================'
W '--  演示数据（H2 演示模式与 MySQL 首次初始化共用）'
W '--  由 scripts/gen-demo-data.ps1 生成，请勿手工逐条修改'
W '--  所有账号初始密码：123456'
W '--    admin       系统管理员'
W '--    2022010102  李静（组织者 / 辅导员）'
W '--    2023010115  王雨桐（学生）'
W '--    2023010126  陈子豪（学生）'
W '--  设计约束：activity.signed_count 必须等于该活动 status in (1,2,4) 的报名记录数'
W '-- ============================================================================'
W ''
W 'INSERT INTO sys_user (id, username, password, real_name, role, student_no, phone, email, avatar, status, violation_count, ban_until, create_time, update_time) VALUES'

$hashAdmin = '$2a$10$Ly3EmByhPTQ3iVQ4AO0VWeriOqqlUMLKlX1Dn8W9mZgCRO2MLyEp6'
$hashOrg = '$2a$10$XUtt0jk4Qg6cTsik9HlEve2nrXT9xpL5Mmc4MTBK2fqXN7peYECJK'
$hashStu = '$2a$10$IUHQpSK6mjFxK17z1KLemeB6ZYB4eoIcPLTttDAq7R305/z38Hz2y'
$hashStu2 = '$2a$10$LzydmsIPpgspDUPtLtKBUOcT51mIAuGlp876dq/IMnCYyPAqRZ802'

$userRows = @()
$userRows += "(1, 'admin', '$hashAdmin', '系统管理员', 'ADMIN', 'A0001', '13800000001', 'admin@campus.edu.cn', NULL, 1, 0, NULL, '2026-09-01 09:00:00', '2026-09-01 09:00:00')"
$userRows += "(2, '2022010102', '$hashOrg', '李静', 'ORGANIZER', 'T2022010', '13800000002', 'lijing@campus.edu.cn', NULL, 1, 0, NULL, '2026-09-02 10:20:00', '2026-09-02 10:20:00')"
$userRows += "(3, '2023010115', '$hashStu', '王雨桐', 'STUDENT', '2023010115', '13800000003', 'wangyutong@campus.edu.cn', NULL, 1, 0, NULL, '2026-09-05 08:30:00', '2026-09-05 08:30:00')"
$userRows += "(4, '2023010126', '$hashStu2', '陈子豪', 'STUDENT', '2023010126', '13800000004', 'chenzihao@campus.edu.cn', NULL, 1, 1, NULL, '2026-09-05 08:35:00', '2026-10-02 20:10:00')"

$studentIds = @()
$n = 0
foreach ($s in $surnames) {
    foreach ($g in $givens) {
        $n++
        $id = 100 + $n
        $studentIds += $id
        $username = '2023' + ('{0:D6}' -f (200 + $n))
        $phone = '138' + ('{0:D8}' -f (10000000 + $n))
        $email = "stu$n`@campus.edu.cn"
        $createDay = '{0:D2}' -f (5 + ($n % 20))
        $userRows += "($id, '$username', '$hashStu', '$s$g', 'STUDENT', '$username', '$phone', '$email', NULL, 1, 0, NULL, '2026-09-$createDay 09:00:00', '2026-09-$createDay 09:00:00')"
    }
}
W (($userRows -join ",`r`n") + ';')

W ''
W 'INSERT INTO activity_category (id, name, sort, status, create_time) VALUES'
W "(1, '文艺演出', 1, 1, '2026-09-01 09:00:00'),"
W "(2, '体育竞技', 2, 1, '2026-09-01 09:00:00'),"
W "(3, '学术讲座', 3, 1, '2026-09-01 09:00:00'),"
W "(4, '志愿服务', 4, 1, '2026-09-01 09:00:00'),"
W "(5, '社团招新', 5, 1, '2026-09-01 09:00:00');"
W ''
W 'INSERT INTO activity (id, title, category_id, cover_url, summary, content, location, start_time, end_time, signup_deadline, capacity, signed_count, status, organizer_id, audit_remark, checkin_code, code_expire_time, need_audit, create_time, update_time) VALUES'
W "(1, '2026 年校园十佳歌手大赛', 1, NULL, '一年一度的校园音乐盛事，20 位选手同台竞演', '比赛分为初赛、复赛、决赛三个阶段，决赛现场设专业评委与大众评审。观众报名后可入场观赛，请提前 15 分钟入场就座。', '大学生活动中心礼堂', '2026-12-05 19:00:00', '2026-12-05 21:30:00', '2026-12-03 18:00:00', 200, 37, 2, 2, NULL, NULL, NULL, 0, '2026-11-01 10:00:00', '2026-11-10 15:30:00'),"
W "(2, '冬季篮球联赛（院系组）', 2, NULL, '八个学院组队参赛，为期两周的循环赛', '以学院为单位报名，每队 12 人。比赛采用四节制，每节 10 分钟。参赛队员需自行购买意外险。', '东区室外篮球场', '2026-12-12 15:00:00', '2026-12-12 18:00:00', '2026-12-10 12:00:00', 120, 58, 2, 2, NULL, NULL, NULL, 0, '2026-11-02 14:20:00', '2026-11-12 09:00:00'),"
W "(3, '人工智能与校园生活专题讲座', 3, NULL, '计算机学院教授主讲，聊聊大模型在日常学习中的应用', '讲座围绕大模型的基本原理、常见应用场景和学术诚信边界展开，现场设提问环节，可携带笔记本入场。', '图书馆报告厅', '2026-11-28 14:00:00', '2026-11-28 16:00:00', '2026-11-27 20:00:00', 80, 61, 2, 2, NULL, NULL, NULL, 0, '2026-11-05 11:00:00', '2026-11-15 16:40:00'),"
W "(4, '图书馆志愿整理服务', 4, NULL, '整理书架、录入新书，计入志愿服务时长', '每周一次，每次 3 小时，可累计志愿时长。请穿着便于活动的衣物，馆内禁止饮食。', '图书馆三楼社科阅览室', '2026-11-22 09:00:00', '2026-11-22 12:00:00', '2026-11-21 18:00:00', 30, 12, 2, 2, NULL, NULL, NULL, 0, '2026-11-06 09:10:00', '2026-11-16 10:00:00'),"
W "(5, '2026 国庆文艺汇演', 1, NULL, '师生同台，共庆国庆', '汇演包含合唱、舞蹈、器乐、朗诵等 12 个节目，总时长约 2 小时。', '大学生活动中心礼堂', '2026-10-01 19:00:00', '2026-10-01 21:00:00', '2026-09-28 18:00:00', 300, 46, 5, 2, NULL, NULL, NULL, 0, '2026-08-20 10:00:00', '2026-10-02 08:00:00'),"
W "(6, '社团招新嘉年华', 5, NULL, '二十余个社团集中摆摊，现场可报名加入', '各社团在中心广场设展位，现场展示社团成果，学生可自由咨询并现场登记。', '大学生活动中心前广场', '2026-12-20 09:00:00', '2026-12-20 17:00:00', '2026-12-18 18:00:00', 500, 0, 1, 2, NULL, NULL, NULL, 0, '2026-11-16 15:30:00', '2026-11-16 15:30:00'),"
W "(7, '期末复习经验分享会', 3, NULL, '邀请高年级同学分享复习方法', '分享会内容包括复习计划制定、专业课资料整理、应试心态调整。', '图书馆报告厅', '2026-11-25 19:00:00', '2026-11-25 21:00:00', '2026-11-24 18:00:00', 60, 0, 3, 2, '活动时间与图书馆闭馆时间冲突，请调整后重新提交', NULL, NULL, 0, '2026-11-15 09:00:00', '2026-11-17 10:20:00'),"
W "(8, '新生心理健康与适应讲座', 3, NULL, '心理咨询中心老师主讲，聊聊入学后的情绪与压力', '讲座内容包括常见心理困扰识别、简单放松训练、校内心理咨询预约方式，现场可匿名提问。', '大学生活动中心报告厅', '2026-11-14 14:00:00', '2026-11-14 16:00:00', '2026-11-13 18:00:00', 100, 18, 5, 2, NULL, NULL, NULL, 0, '2026-11-08 10:00:00', '2026-11-14 16:05:00');"
W ''

# ---------- 报名记录 ----------
$regRows = @()
$rid = 0
foreach ($p in $plan) {
    if ($p.withUser3) {
        $rid++
        $regRows += "($rid, $($p.activity), 3, $($p.status), '2026-11-12 20:15:00', NULL, NULL, NULL, '2026-11-12 20:15:00')"
    }
    foreach ($i in ($p.from..$p.to)) {
        $rid++
        $uid = $studentIds[$i - 1]
        $day = '{0:D2}' -f (10 + ($i % 10))
        $regRows += "($rid, $($p.activity), $uid, $($p.status), '2026-11-$day 19:30:00', NULL, NULL, NULL, '2026-11-$day 19:30:00')"
    }
}
# 活动 2 的 陈子豪
$rid++
$regRows += "($rid, 2, 4, 1, '2026-11-13 12:00:00', NULL, NULL, NULL, '2026-11-13 12:00:00')"
# 活动 5：已结束，王雨桐签到、陈子豪爽约
$rid++
$regRows += "($rid, 5, 3, 2, '2026-09-20 19:30:00', '2026-10-01 18:52:00', 1, NULL, '2026-10-01 18:52:00')"
$rid++
$regRows += "($rid, 5, 4, 4, '2026-09-21 10:00:00', NULL, NULL, NULL, '2026-10-02 08:00:00')"
foreach ($i in $plan5Checked) {
    $rid++
    $uid = $studentIds[$i - 1]
    $regRows += "($rid, 5, $uid, 2, '2026-09-22 10:00:00', '2026-10-01 18:5$($i % 10):00', 1, NULL, '2026-10-01 18:5$($i % 10):00')"
}
foreach ($i in $plan5Absent) {
    $rid++
    $uid = $studentIds[$i - 1]
    $regRows += "($rid, 5, $uid, 4, '2026-09-22 10:05:00', NULL, NULL, NULL, '2026-10-02 08:00:00')"
}

# 活动 8：最近结束的讲座，15 人签到、3 人爽约、2 人取消，用于让最近 7 天趋势图有签到数据
$plan8Checked = 45..59
$plan8Absent = 60..62
$plan8Canceled = 63..64
foreach ($i in $plan8Checked) {
    $rid++
    $uid = $studentIds[$i - 1]
    $minute = ('{0:D2}' -f ($i % 60))
    $regRows += "($rid, 8, $uid, 2, '2026-11-12 20:${minute}:00', '2026-11-14 13:${minute}:00', 1, NULL, '2026-11-14 13:${minute}:00')"
}
foreach ($i in $plan8Absent) {
    $rid++
    $uid = $studentIds[$i - 1]
    $regRows += "($rid, 8, $uid, 4, '2026-11-12 21:05:00', NULL, NULL, NULL, '2026-11-14 16:05:00')"
}
foreach ($i in $plan8Canceled) {
    $rid++
    $uid = $studentIds[$i - 1]
    $regRows += "($rid, 8, $uid, 3, '2026-11-12 21:30:00', NULL, NULL, NULL, '2026-11-13 09:10:00')"
}

W 'INSERT INTO activity_registration (id, activity_id, user_id, status, signup_time, checkin_time, checkin_type, remark, update_time) VALUES'
W (($regRows -join ",`r`n") + ';')
W ''

W 'INSERT INTO activity_review (id, activity_id, user_id, score, content, anonymous, create_time) VALUES'
W "(1, 5, 3, 5, '舞台效果比去年好很多，节目衔接也很顺畅，看得出排练了很久。', 0, '2026-10-02 10:20:00'),"
W "(2, 5, 4, 4, '整体不错，就是中途音响有点小问题，后半场人太多有点挤。', 0, '2026-10-02 11:05:00'),"
W "(3, 5, 101, 5, '合唱节目很震撼，希望能多办几场。', 1, '2026-10-02 12:10:00'),"
W "(4, 5, 105, 3, '入场排队时间有点长，其他都还行。', 0, '2026-10-02 13:30:00'),"
W "(5, 8, 144, 5, '老师讲得很实在，关于情绪波动的解释让我踏实了不少。', 0, '2026-11-14 20:10:00'),"
W "(6, 8, 145, 4, '内容不错，就是时间有点紧，放松训练只做了一轮。', 1, '2026-11-14 21:05:00'),"
W "(7, 8, 146, 4, '希望以后能多安排一些类似的讲座。', 0, '2026-11-15 09:20:00');"
W ''

W 'INSERT INTO notice (id, user_id, title, content, notice_type, is_read, create_time) VALUES'
W "(1, 3, '报名成功', '你已成功报名「2026 年校园十佳歌手大赛」，请于 12 月 5 日 18:45 前入场。', 'ACTIVITY', 0, '2026-11-12 20:15:10'),"
W "(2, 3, '活动评分邀请', '「2026 国庆文艺汇演」已结束，欢迎为活动打分并留下你的评价。', 'SYSTEM', 1, '2026-10-02 08:00:00'),"
W "(3, 4, '爽约提醒', '你报名的「2026 国庆文艺汇演」未签到，已记录一次爽约。累计 2 次将限制报名 30 天。', 'SYSTEM', 0, '2026-10-02 08:00:05'),"
W "(4, 2, '活动审核结果', '你提交的「期末复习经验分享会」未通过审核，原因：活动时间与图书馆闭馆时间冲突。', 'AUDIT', 0, '2026-11-17 10:20:30');"
W ''

W 'INSERT INTO sys_notice (id, title, content, publisher_id, status, create_time, update_time) VALUES'
W "(1, '关于开展 2026 年冬季校园文化节的通知', '各学院、各学生组织：冬季校园文化节将于 12 月举办，包含文艺演出、体育竞技、志愿服务等系列活动，欢迎踊跃报名参与。', 1, 1, '2026-11-01 09:00:00', '2026-11-01 09:00:00');"
W ''

W 'INSERT INTO sys_config (id, config_key, config_value, remark) VALUES'
W "(1, 'violation_limit', '2', '累计爽约次数上限，达到后限制报名'),"
W "(2, 'ban_days', '30', '触发爽约限制后的封禁天数'),"
W "(3, 'checkin_code_minutes', '30', '签到码有效期（分钟）'),"
W "(4, 'activity_default_capacity', '100', '新建活动默认人数上限');"

[System.IO.File]::WriteAllText($out, $sb.ToString(), (New-Object System.Text.UTF8Encoding($false)))
Write-Host "已生成: $out"
Write-Host "用户 $($userRows.Count) 条，报名 $($regRows.Count) 条"

# ---------- 自检：signed_count 与明细是否一致 ----------
# signed_count 统计的是"占用了名额"的人：已报名(1)、已签到(2)、爽约(4)
# 已取消(3) 不占名额，不算
$expect = @{ 1 = 37; 2 = 58; 3 = 61; 4 = 12; 5 = 46; 6 = 0; 7 = 0; 8 = 18 }
foreach ($k in $expect.Keys | Sort-Object) {
    $cnt = ($regRows | Where-Object {
        $f = $_ -replace '^\(|\)$', ''
        $cols = $f -split ','
        ($cols[1].Trim() -eq "$k") -and ($cols[3].Trim() -in @('1', '2', '4'))
    }).Count
    $flag = if ($cnt -eq $expect[$k]) { 'OK' } else { 'MISMATCH' }
    Write-Host ("活动 {0}: 明细 {1} / signed_count {2} -> {3}" -f $k, $cnt, $expect[$k], $flag)
}
