package com.techhub.service;

import com.techhub.dto.category.CategoryCreateRequest;
import com.techhub.dto.category.CategoryUpdateRequest;
import com.techhub.entity.Category;

import java.util.List;

public interface CategoryService {
    List<Category> listEnabled();
    Category create(CategoryCreateRequest request);
    void update(Long id, CategoryUpdateRequest request);
    void delete(Long id);
}
