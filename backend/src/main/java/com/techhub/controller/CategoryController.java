package com.techhub.controller;

import com.techhub.common.R;
import com.techhub.dto.category.CategoryCreateRequest;
import com.techhub.dto.category.CategoryUpdateRequest;
import com.techhub.entity.Category;
import com.techhub.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public R<List<Category>> listEnabled() {
        return R.ok(categoryService.listEnabled());
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
}
