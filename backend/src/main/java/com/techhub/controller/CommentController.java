package com.techhub.controller;

import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.R;
import com.techhub.common.ResultCode;
import com.techhub.dto.comment.CommentCreateRequest;
import com.techhub.dto.comment.CommentVO;
import com.techhub.security.SecurityUtils;
import com.techhub.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

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
}
