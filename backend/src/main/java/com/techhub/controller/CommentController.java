package com.techhub.controller;

import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.R;
import com.techhub.common.ResultCode;
import com.techhub.dto.comment.CommentCreateRequest;
import com.techhub.dto.comment.CommentVO;
import com.techhub.security.SecurityUtils;
import com.techhub.service.CommentService;
import com.techhub.service.DivineCommentService;
import jakarta.validation.Valid;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final DivineCommentService divineCommentService;

    /**
     * 获取帖子评论列表（分页）。
     */
    @GetMapping("/api/v1/posts/{postId}/comments")
    public R<PageResult<CommentVO>> listComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return R.ok(commentService.listByPost(postId, page, size, currentUserId));
    }

    /**
     * 发表评论。
     */
    @PostMapping("/api/v1/posts/{postId}/comments")
    public R<CommentVO> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request) {
        if (!SecurityUtils.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return R.ok(commentService.create(postId, request));
    }

    /**
     * 删除评论。
     */
    @DeleteMapping("/api/v1/comments/{id}")
    public R<Void> deleteComment(@PathVariable Long id) {
        if (!SecurityUtils.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        commentService.delete(id);
        return R.ok("删除成功");
    }

    /**
     * 推荐神评。
     */
    @PostMapping("/api/v1/comments/{id}/recommend")
    public R<Void> recommendComment(@PathVariable Long id) {
        if (!SecurityUtils.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        divineCommentService.recommend(id);
        return R.ok("推荐成功");
    }

    /**
     * 取消推荐神评。
     */
    @DeleteMapping("/api/v1/comments/{id}/recommend")
    public R<Void> cancelRecommendComment(@PathVariable Long id) {
        if (!SecurityUtils.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        divineCommentService.cancelRecommend(id);
        return R.ok("取消推荐成功");
    }

    /**
     * 获取帖子神评列表。
     */
    @GetMapping("/api/v1/posts/{postId}/comments/divine")
    public R<List<CommentVO>> listDivineComments(@PathVariable Long postId) {
        return R.ok(divineCommentService.listDivineComments(postId));
    }
}
