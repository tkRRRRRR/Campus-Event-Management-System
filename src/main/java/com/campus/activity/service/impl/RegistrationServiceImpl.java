package com.campus.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.common.BusinessException;
import com.campus.activity.common.Constants;
import com.campus.activity.common.PageResult;
import com.campus.activity.common.ResultCode;
import com.campus.activity.dto.RegistrationQuery;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityRegistration;
import com.campus.activity.entity.Notice;
import com.campus.activity.entity.SysUser;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityRegistrationMapper;
import com.campus.activity.mapper.NoticeMapper;
import com.campus.activity.mapper.SysUserMapper;
import com.campus.activity.service.RegistrationService;
import com.campus.activity.vo.MyRegistrationVO;
import com.campus.activity.vo.RegistrationMemberVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ActivityMapper activityMapper;
    private final ActivityRegistrationMapper registrationMapper;
    private final SysUserMapper userMapper;
    private final NoticeMapper noticeMapper;

    /**
     * 对照实验开关：开启后用"先查后改"的旧写法，用来在论文里复现超卖。
     * 生产环境必须保持 false（application.yml 默认 false）。
     */
    @Value("${app.registration.naive:false}")
    private boolean naiveMode;

    @Override
    @Transactional(rollbackFor = Exception.class)
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
            throw new BusinessException("报名已于 " + DATE_FORMAT.format(activity.getSignupDeadline()) + " 截止");
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
                            + DATE_FORMAT.format(activity.getStartTime()) + "，地点 " + activity.getLocation() + "。",
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Integer activityId, Integer userId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在");
        }
        ActivityRegistration registration = registrationMapper.selectOne(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getActivityId, activityId)
                        .eq(ActivityRegistration::getUserId, userId));
        if (registration == null) {
            throw new BusinessException("你没有报名该活动");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(activity.getSignupDeadline())) {
            throw new BusinessException("报名已截止，无法取消");
        }
        if (!now.isBefore(activity.getStartTime())) {
            throw new BusinessException("活动已开始，无法取消报名");
        }

        if (registration.getStatus() == Constants.REG_SIGNED_UP) {
            activityMapper.decreaseSignedCount(activityId);
        } else if (registration.getStatus() != Constants.REG_PENDING) {
            throw new BusinessException("当前状态不可取消报名");
        }

        ActivityRegistration update = new ActivityRegistration();
        update.setId(registration.getId());
        update.setStatus(Constants.REG_CANCELED);
        if (registrationMapper.update(update, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getId, registration.getId())
                .eq(ActivityRegistration::getStatus, registration.getStatus())) != 1) {
            throw new BusinessException("报名状态已变化，请刷新后重试");
        }
        log.info("取消报名: activityId={}, userId={}", activityId, userId);
    }

    @Override
    public PageResult<MyRegistrationVO> myPage(RegistrationQuery query, Integer userId) {
        Page<MyRegistrationVO> page = buildPage(query);
        registrationMapper.selectMyPage(page, userId, query);
        return PageResult.of(page);
    }

    @Override
    public PageResult<RegistrationMemberVO> memberPage(Integer activityId, RegistrationQuery query,
                                                       Integer currentUserId, boolean admin) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在");
        }
        if (!admin && !activity.getOrganizerId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能查看自己发布活动的报名名单");
        }
        Page<RegistrationMemberVO> page = buildPage(query);
        registrationMapper.selectMemberPage(page, activityId, query);
        return PageResult.of(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditSignup(Integer registrationId, boolean pass, String reason, Integer organizerId) {
        ActivityRegistration registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报名记录不存在");
        }
        if (registration.getStatus() != Constants.REG_PENDING) {
            throw new BusinessException("该报名不在待审核状态");
        }

        Activity activity = activityMapper.selectById(registration.getActivityId());
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在");
        }
        if (!activity.getOrganizerId().equals(organizerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能审核自己发布活动的报名");
        }

        ActivityRegistration update = new ActivityRegistration();
        update.setId(registrationId);
        if (pass) {
            int affected = activityMapper.increaseSignedCount(activity.getId());
            if (affected != 1) {
                throw new BusinessException("名额已满，无法通过该报名");
            }
            update.setStatus(Constants.REG_SIGNED_UP);
            sendNotice(registration.getUserId(), "报名审核通过",
                    "你报名的「" + activity.getTitle() + "」已通过审核，请按时参加。",
                    Constants.NOTICE_AUDIT);
        } else {
            update.setStatus(Constants.REG_CANCELED);
            String tail = (reason == null || reason.trim().isEmpty())
                    ? ""
                    : "，原因：" + reason.trim();
            sendNotice(registration.getUserId(), "报名未通过",
                    "你报名的「" + activity.getTitle() + "」未通过组织者审核" + tail + "。",
                    Constants.NOTICE_AUDIT);
        }
        if (registrationMapper.update(update, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getId, registrationId)
                .eq(ActivityRegistration::getStatus, Constants.REG_PENDING)) != 1) {
            throw new BusinessException("报名状态已变化，请刷新后重试");
        }
    }

    @Override
    public byte[] exportMembers(Integer activityId, String keyword, Integer status,
                                Integer currentUserId, boolean admin) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在");
        }
        if (!admin && !activity.getOrganizerId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能导出自己发布活动的报名名单");
        }

        RegistrationQuery query = new RegistrationQuery();
        query.setStatus(status);
        query.setKeyword(keyword);
        Page<RegistrationMemberVO> page = new Page<>(1, 5000);
        registrationMapper.selectMemberPage(page, activityId, query);
        List<RegistrationMemberVO> rows = page.getRecords();

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("报名名单");
            int widthBase = 256;

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.LEFT);

            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headFont.setColor(IndexedColors.WHITE.getIndex());
            headStyle.setFont(headFont);
            headStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headStyle.setAlignment(HorizontalAlignment.CENTER);
            headStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headStyle.setBorderBottom(BorderStyle.THIN);
            headStyle.setBorderTop(BorderStyle.THIN);
            headStyle.setBorderLeft(BorderStyle.THIN);
            headStyle.setBorderRight(BorderStyle.THIN);

            CellStyle bodyStyle = workbook.createCellStyle();
            bodyStyle.setAlignment(HorizontalAlignment.CENTER);
            bodyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            bodyStyle.setBorderBottom(BorderStyle.THIN);
            bodyStyle.setBorderTop(BorderStyle.THIN);
            bodyStyle.setBorderLeft(BorderStyle.THIN);
            bodyStyle.setBorderRight(BorderStyle.THIN);

            Row title = sheet.createRow(0);
            Cell titleCell = title.createCell(0);
            titleCell.setCellValue(activity.getTitle() + " —— 报名名单");
            titleCell.setCellStyle(titleStyle);
            if (sheet.getRow(0) != null) {
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));
            }

            String[] headers = {"序号", "姓名", "学号", "用户名", "手机号", "报名状态", "报名时间", "签到时间", "签到方式"};
            Row head = sheet.createRow(1);
            head.setHeightInPoints(22);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
            }

            int rowIndex = 2;
            for (int i = 0; i < rows.size(); i++) {
                RegistrationMemberVO vo = rows.get(i);
                Row row = sheet.createRow(rowIndex++);
                String[] values = {
                        String.valueOf(i + 1),
                        nullToDash(vo.getRealName()),
                        nullToDash(vo.getStudentNo()),
                        nullToDash(vo.getUsername()),
                        nullToDash(vo.getPhone()),
                        regStatusText(vo.getStatus()),
                        vo.getSignupTime() == null ? "-" : vo.getSignupTime().format(DATE_TIME),
                        vo.getCheckinTime() == null ? "-" : vo.getCheckinTime().format(DATE_TIME),
                        checkinTypeText(vo.getCheckinType())
                };
                for (int c = 0; c < values.length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(values[c]);
                    cell.setCellStyle(bodyStyle);
                }
            }

            int[] widths = {6, 12, 14, 16, 16, 12, 20, 20, 12};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * widthBase);
            }
            sheet.createFreezePane(0, 2);
            if (!rows.isEmpty()) {
                sheet.setAutoFilter(new CellRangeAddress(1, rows.size() + 1, 0, headers.length - 1));
            }

            workbook.write(out);
            log.info("导出报名名单: activityId={}, 行数={}", activityId, rows.size());
            return out.toByteArray();
        } catch (IOException e) {
            log.error("导出报名名单失败", e);
            throw new BusinessException("导出失败，请稍后重试");
        }
    }

    /* ---------------- 私有方法 ---------------- */

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String nullToDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private String regStatusText(Integer status) {
        if (status == null) {
            return "-";
        }
        switch (status) {
            case Constants.REG_SIGNED_UP:
                return "已报名";
            case Constants.REG_CHECKED_IN:
                return "已签到";
            case Constants.REG_CANCELED:
                return "已取消";
            case Constants.REG_ABSENT:
                return "爽约";
            case Constants.REG_PENDING:
                return "待审核";
            default:
                return "未知";
        }
    }

    private String checkinTypeText(Integer type) {
        if (type == null) {
            return "-";
        }
        return type == 2 ? "补签" : "签到码";
    }

    private <T> Page<T> buildPage(RegistrationQuery query) {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : Math.min(query.getPageSize(), 50);
        return new Page<>(pageNum, pageSize);
    }

    private void sendNotice(Integer userId, String title, String content, String type) {
        Notice notice = new Notice();
        notice.setUserId(userId);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setNoticeType(type);
        notice.setIsRead(0);
        noticeMapper.insert(notice);
    }
}
