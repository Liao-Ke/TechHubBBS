package com.techhub.controller.admin;

import com.techhub.common.PageResult;
import com.techhub.common.R;
import com.techhub.dto.category.CategoryNoticeVO;
import com.techhub.service.AdminOtherService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminOtherController {

    private final AdminOtherService adminOtherService;

    @PatchMapping("/comments/{id}/divine")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public R<Void> forceSetDivine(@PathVariable Long id, @RequestParam boolean divine) {
        adminOtherService.forceSetDivine(id, divine);
        return R.ok(divine ? "已强制设置为神评" : "已取消神评");
    }

    @GetMapping("/notices")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public R<PageResult<CategoryNoticeVO>> listNotices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer type) {
        return R.ok(adminOtherService.listNotices(page, size, categoryId, type));
    }
}
