package com.techhub.controller.admin;

import com.techhub.common.PageResult;
import com.techhub.common.R;
import com.techhub.dto.user.UserProfileVO;
import com.techhub.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<PageResult<UserProfileVO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return R.ok(adminUserService.listUsers(page, size, keyword));
    }

    @PatchMapping("/{id}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> banUser(@PathVariable Long id, @RequestParam boolean ban) {
        adminUserService.banUser(id, ban);
        return R.ok(ban ? "用户已封禁" : "用户已解封");
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> changeRole(@PathVariable Long id, @RequestParam String role) {
        adminUserService.changeRole(id, role);
        return R.ok("角色已更新");
    }
}
