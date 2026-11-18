package com.campus.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.activity.common.BusinessException;
import com.campus.activity.common.ResultCode;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityCategory;
import com.campus.activity.mapper.ActivityCategoryMapper;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final ActivityCategoryMapper categoryMapper;
    private final ActivityMapper activityMapper;

    @Override
    public List<ActivityCategory> listEnabled() {
        return categoryMapper.selectList(new LambdaQueryWrapper<ActivityCategory>()
                .eq(ActivityCategory::getStatus, 1)
                .orderByAsc(ActivityCategory::getSort));
    }

    @Override
    public void create(ActivityCategory category) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new BusinessException("分类名称不能为空");
        }
        Long exists = categoryMapper.selectCount(new LambdaQueryWrapper<ActivityCategory>()
                .eq(ActivityCategory::getName, category.getName().trim()));
        if (exists != null && exists > 0) {
            throw new BusinessException("该分类已存在");
        }
        category.setId(null);
        category.setName(category.getName().trim());
        category.setSort(category.getSort() == null ? 0 : category.getSort());
        category.setStatus(category.getStatus() == null ? 1 : category.getStatus());
        categoryMapper.insert(category);
    }

    @Override
    public void update(ActivityCategory category) {
        if (category.getId() == null) {
            throw new BusinessException("缺少分类 id");
        }
        if (categoryMapper.selectById(category.getId()) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分类不存在");
        }
        categoryMapper.updateById(category);
    }

    @Override
    public void delete(Integer id) {
        Long used = activityMapper.selectCount(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getCategoryId, id));
        if (used != null && used > 0) {
            throw new BusinessException("该分类下还有 " + used + " 个活动，不能删除");
        }
        categoryMapper.deleteById(id);
    }
}
