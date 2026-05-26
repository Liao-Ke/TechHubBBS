package com.techhub.service;

import com.techhub.dto.category.CategoryNoticeCreateRequest;
import com.techhub.dto.category.CategoryNoticeUpdateRequest;
import com.techhub.dto.category.CategoryNoticeVO;

import java.util.List;

public interface CategoryNoticeService {

    List<CategoryNoticeVO> listByCategory(Long categoryId, Integer type);

    CategoryNoticeVO create(Long categoryId, CategoryNoticeCreateRequest request, Long authorId);

    CategoryNoticeVO getById(Long id);

    void update(Long noticeId, CategoryNoticeUpdateRequest request);

    void delete(Long noticeId);
}
