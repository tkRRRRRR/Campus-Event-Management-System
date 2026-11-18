package com.campus.activity.controller;

import com.campus.activity.annotation.RequireRole;
import com.campus.activity.common.Constants;
import com.campus.activity.common.Result;
import com.campus.activity.entity.ActivityCategory;
import com.campus.activity.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /** 所有登录用户都能拿分类，用于筛选和发布活动 */
    @GetMapping("/list")
    public Result<List<ActivityCategory>> list() {
        return Result.ok(categoryService.listEnabled());
    }

    @PostMapping
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<Void> create(@RequestBody ActivityCategory category) {
        categoryService.create(category);
        return Result.ok("分类已添加", null);
    }

    @PutMapping("/{id}")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<Void> update(@PathVariable Integer id, @RequestBody ActivityCategory category) {
        category.setId(id);
        categoryService.update(category);
        return Result.ok("分类已更新", null);
    }

    @DeleteMapping("/{id}")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<Void> delete(@PathVariable Integer id) {
        categoryService.delete(id);
        return Result.ok("分类已删除", null);
    }
}
