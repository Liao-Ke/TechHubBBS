package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.techhub.common.BusinessException;
import com.techhub.common.ResultCode;
import com.techhub.dto.category.CategoryCreateRequest;
import com.techhub.dto.category.CategoryUpdateRequest;
import com.techhub.entity.Category;
import com.techhub.entity.Post;
import com.techhub.mapper.CategoryMapper;
import com.techhub.mapper.PostMapper;
import com.techhub.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final PostMapper postMapper;

    @Override
    public List<Category> listEnabled() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSortOrder)
        );
    }

    @Override
    @Transactional
    public Category create(CategoryCreateRequest request) {
        // Check name uniqueness
        if (categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getName, request.getName())) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "版块名称已存在");
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        category.setStatus(1); // default enabled
        categoryMapper.insert(category);
        return category;
    }

    @Override
    @Transactional
    public void update(Long id, CategoryUpdateRequest request) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "版块不存在");
        }

        if (request.getName() != null) {
            category.setName(request.getName());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }

        categoryMapper.updateById(category);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // Check if category has posts
        long postCount = postMapper.selectCount(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getCategoryId, id)
                        .eq(Post::getDeleted, 0)
        );
        if (postCount > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "版块下有帖子，无法删除，请先禁用");
        }
        categoryMapper.deleteById(id);
    }
}
