package com.campus.activity.controller;

import com.campus.activity.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 探活接口，用于部署后确认服务是否正常。
 */
@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("time", LocalDateTime.now());
        data.put("app", "campus-activity");
        return Result.ok(data);
    }
}
