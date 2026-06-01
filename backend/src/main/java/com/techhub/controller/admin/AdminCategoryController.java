package com.techhub.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.techhub.common.R;
import com.techhub.dto.category.CategoryCreateRequest;
import com.techhub.dto.category.CategoryUpdateRequest;
import com.techhub.entity.Category;
import com.techhub.mapper.CategoryMapper;
import com.techhub.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<List<Category>> listAll() {
        return R.ok(categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSortOrder)
        ));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Category> create(@Valid @RequestBody CategoryCreateRequest request) {
        return R.ok(categoryService.create(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryUpdateRequest request) {
        categoryService.update(id, request);
        return R.ok("更新成功");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok("删除成功");
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        CategoryUpdateRequest request = new CategoryUpdateRequest();
        request.setStatus(status);
        categoryService.update(id, request);
        return R.ok(status == 1 ? "版块已启用" : "版块已禁用");
    }
}
