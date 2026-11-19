package com.campus.activity.service;

import com.campus.activity.entity.SysConfig;

import java.util.List;

public interface SysConfigService {

    List<SysConfig> list();

    /** 只改值，key 不允许改（代码里按 key 取值） */
    void updateValue(Integer id, String value);
}
