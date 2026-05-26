package com.techhub.controller;

import com.techhub.common.R;
import com.techhub.dto.category.CategoryNoticeCreateRequest;
import com.techhub.dto.category.CategoryNoticeUpdateRequest;
import com.techhub.dto.category.CategoryNoticeVO;
import com.techhub.security.SecurityUtils;
import com.techhub.service.CategoryNoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class NoticeController {

    private final CategoryNoticeService noticeService;

    @GetMapping("/categories/{categoryId}/notices")
    public R<List<CategoryNoticeVO>> listByCategory(@PathVariable Long categoryId,
                                                     @RequestParam(required = false) Integer type) {
        return R.ok(noticeService.listByCategory(categoryId, type));
    }

    @PostMapping("/categories/{categoryId}/notices")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public R<CategoryNoticeVO> create(@PathVariable Long categoryId,
                                       @Valid @RequestBody CategoryNoticeCreateRequest request) {
        Long authorId = SecurityUtils.getCurrentUserId();
        return R.ok(noticeService.create(categoryId, request, authorId));
    }

    @GetMapping("/notices/{id}")
    public R<CategoryNoticeVO> getById(@PathVariable Long id) {
        return R.ok(noticeService.getById(id));
    }

    @PatchMapping("/notices/{id}")
    @PreAuthorize("isAuthenticated()")
    public R<Void> update(@PathVariable Long id,
                           @Valid @RequestBody CategoryNoticeUpdateRequest request) {
        noticeService.update(id, request);
        return R.ok("更新成功");
    }

    @DeleteMapping("/notices/{id}")
    @PreAuthorize("isAuthenticated()")
    public R<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return R.ok("删除成功");
    }
}
