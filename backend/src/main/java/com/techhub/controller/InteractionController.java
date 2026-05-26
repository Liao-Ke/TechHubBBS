package com.techhub.controller;

import com.techhub.common.BusinessException;
import com.techhub.common.R;
import com.techhub.common.ResultCode;
import com.techhub.security.SecurityUtils;
import com.techhub.service.InteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    // ==================== 帖子点赞 ====================

    @PostMapping("/api/v1/posts/{id}/likes")
    public R<Void> likePost(@PathVariable Long id) {
        if (!SecurityUtils.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        interactionService.likePost(id);
        return R.ok("点赞成功");
    }

    @DeleteMapping("/api/v1/posts/{id}/likes")
    public R<Void> unlikePost(@PathVariable Long id) {
        if (!SecurityUtils.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        interactionService.unlikePost(id);
        return R.ok("已取消");
    }

    // ==================== 帖子收藏 ====================

    @PostMapping("/api/v1/posts/{id}/favorites")
    public R<Void> favoritePost(@PathVariable Long id) {
        if (!SecurityUtils.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        interactionService.favoritePost(id);
        return R.ok("收藏成功");
    }

    @DeleteMapping("/api/v1/posts/{id}/favorites")
    public R<Void> unfavoritePost(@PathVariable Long id) {
        if (!SecurityUtils.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        interactionService.unfavoritePost(id);
        return R.ok("已取消");
    }

    // ==================== 评论点赞 ====================

    @PostMapping("/api/v1/comments/{id}/likes")
    public R<Void> likeComment(@PathVariable Long id) {
        if (!SecurityUtils.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        interactionService.likeComment(id);
        return R.ok("点赞成功");
    }

    @DeleteMapping("/api/v1/comments/{id}/likes")
    public R<Void> unlikeComment(@PathVariable Long id) {
        if (!SecurityUtils.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        interactionService.unlikeComment(id);
        return R.ok("已取消");
    }
}
