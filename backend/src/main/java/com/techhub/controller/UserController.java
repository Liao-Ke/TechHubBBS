package com.techhub.controller;

import com.techhub.common.PageResult;
import com.techhub.common.R;
import com.techhub.dto.post.PostListQuery;
import com.techhub.dto.post.PostVO;
import com.techhub.dto.user.UserDetailVO;
import com.techhub.dto.user.UserProfileVO;
import com.techhub.dto.user.UserUpdateRequest;
import com.techhub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public R<UserDetailVO> getCurrentUser() {
        return R.ok(userService.getCurrentUser());
    }

    @PatchMapping("/me")
    public R<UserDetailVO> updateProfile(@Valid @RequestBody UserUpdateRequest request) {
        userService.updateProfile(request);
        return R.ok(userService.getCurrentUser());
    }

    @GetMapping("/{id}")
    public R<UserProfileVO> getUserProfile(@PathVariable Long id) {
        return R.ok(userService.getUserProfile(id));
    }

    @GetMapping("/{id}/posts")
    public R<PageResult<PostVO>> getUserPosts(@PathVariable Long id, PostListQuery query) {
        return R.ok(userService.getUserPosts(id, query));
    }
}
