package com.campus.activity.service;

import com.campus.activity.entity.ActivityCategory;

import java.util.List;

public interface CategoryService {

    /** 启用中的分类，按 sort 升序 */
    List<ActivityCategory> listEnabled();

    /** 全部分类（含停用），管理员维护时使用 */
    List<ActivityCategory> listAll();

    void create(ActivityCategory category);

    void update(ActivityCategory category);

    void delete(Integer id);
}
