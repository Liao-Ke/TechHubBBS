package com.techhub.controller;

import com.techhub.common.PageResult;
import com.techhub.common.R;
import com.techhub.dto.user.FollowStatusVO;
import com.techhub.dto.user.UserProfileVO;
import com.techhub.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{id}/follow")
    public R<Void> follow(@PathVariable Long id) {
        followService.follow(id);
        return R.ok("关注成功");
    }

    @DeleteMapping("/{id}/follow")
    public R<Void> unfollow(@PathVariable Long id) {
        followService.unfollow(id);
        return R.ok("取消关注成功");
    }

    @GetMapping("/{id}/follow")
    public R<FollowStatusVO> getFollowStatus(@PathVariable Long id) {
        return R.ok(followService.getFollowStatus(id));
    }

    @GetMapping("/{id}/followers")
    public R<PageResult<UserProfileVO>> getFollowers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(followService.getFollowers(id, page, size));
    }

    @GetMapping("/{id}/followings")
    public R<PageResult<UserProfileVO>> getFollowings(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(followService.getFollowings(id, page, size));
    }
}
