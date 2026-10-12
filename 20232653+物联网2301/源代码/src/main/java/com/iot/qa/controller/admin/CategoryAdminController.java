package com.iot.qa.controller.admin;

import com.iot.qa.common.Result;
import com.iot.qa.dto.CategoryForm;
import com.iot.qa.entity.QaCategory;
import com.iot.qa.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台：知识分类管理接口。
 */
@RestController
@RequestMapping("/api/admin/category")
@RequiredArgsConstructor
public class CategoryAdminController {

    private final CategoryService categoryService;

    @GetMapping("/list")
    public Result<List<QaCategory>> list() {
        return Result.success(categoryService.listWithCount());
    }

    @GetMapping("/options")
    public Result<List<QaCategory>> options() {
        return Result.success(categoryService.options());
    }

    @PostMapping("/save")
    public Result<Void> save(@Valid @RequestBody CategoryForm form) {
        categoryService.saveOrUpdateCategory(form);
        return Result.success("保存成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        categoryService.removeCategory(id);
        return Result.success("删除成功", null);
    }
}