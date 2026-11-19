package com.campus.activity.controller;

import com.campus.activity.annotation.RequireRole;
import com.campus.activity.common.Constants;
import com.campus.activity.common.Result;
import com.campus.activity.entity.SysConfig;
import com.campus.activity.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 系统参数配置，仅管理员可见可改。
 */
@RestController
@RequestMapping("/api/config")
@RequireRole(Constants.ROLE_ADMIN)
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService configService;

    @GetMapping("/list")
    public Result<List<SysConfig>> list() {
        return Result.ok(configService.list());
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        configService.updateValue(id, body.get("configValue"));
        return Result.ok("参数已保存", null);
    }
}
