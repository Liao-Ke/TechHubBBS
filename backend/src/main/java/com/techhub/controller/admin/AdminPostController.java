package com.techhub.controller.admin;

import com.techhub.common.PageResult;
import com.techhub.common.R;
import com.techhub.dto.post.PostVO;
import com.techhub.service.AdminPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    private final AdminPostService adminPostService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public R<PageResult<PostVO>> listAllPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return R.ok(adminPostService.listAllPosts(page, size, keyword));
    }

    @PatchMapping("/{id}/type")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public R<Void> setPostType(@PathVariable Long id, @RequestParam int type) {
        adminPostService.setPostType(id, type);
        return R.ok("帖子类型已更新");
    }

    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public R<Void> toggleLock(@PathVariable Long id) {
        adminPostService.togglePostLock(id);
        return R.ok("帖子锁定状态已切换");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> forceDelete(@PathVariable Long id) {
        adminPostService.forceDeletePost(id);
        return R.ok("帖子已强制删除");
    }
}
