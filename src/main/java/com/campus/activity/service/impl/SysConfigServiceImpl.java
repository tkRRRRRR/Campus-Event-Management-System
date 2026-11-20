package com.campus.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.activity.common.BusinessException;
import com.campus.activity.common.ResultCode;
import com.campus.activity.entity.SysConfig;
import com.campus.activity.mapper.SysConfigMapper;
import com.campus.activity.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 系统参数。爽约阈值、封禁天数、签到码有效期、默认名额都在这里配，
 * 业务代码读参数时走 SysConfig，不写死数字。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    /** 这几项必须是正整数，改错会让业务逻辑出现除零或负数判断 */
    private static final Set<String> INT_KEYS = new HashSet<>(Arrays.asList(
            "violation_limit", "ban_days", "checkin_code_minutes", "activity_default_capacity",
            "quality_weight_checkin", "quality_weight_score", "quality_weight_complete"));

    private final SysConfigMapper configMapper;

    @Override
    public List<SysConfig> list() {
        return configMapper.selectList(new LambdaQueryWrapper<SysConfig>()
                .orderByAsc(SysConfig::getId));
    }

    @Override
    public void updateValue(Integer id, String value) {
        SysConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "参数不存在");
        }
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException("参数值不能为空");
        }
        String trimmed = value.trim();
        if (INT_KEYS.contains(config.getConfigKey())) {
            int number;
            try {
                number = Integer.parseInt(trimmed);
            } catch (NumberFormatException e) {
                throw new BusinessException("该参数必须是整数");
            }
            if (number <= 0) {
                throw new BusinessException("该参数必须大于 0");
            }
        }

        SysConfig update = new SysConfig();
        update.setId(id);
        update.setConfigValue(trimmed);
        configMapper.updateById(update);
        log.info("系统参数已修改: {} = {}", config.getConfigKey(), trimmed);
    }
}
