package com.techhub.controller;

import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.R;
import com.techhub.common.ResultCode;
import com.techhub.dto.post.PostCreateRequest;
import com.techhub.dto.post.PostListQuery;
import com.techhub.dto.post.PostUpdateRequest;
import com.techhub.dto.post.PostVO;
import com.techhub.security.SecurityUtils;
import com.techhub.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Validated
public class PostController {

    private final PostService postService;

    @GetMapping
    public R<PageResult<PostVO>> listPosts(@Valid PostListQuery query) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return R.ok(postService.listPosts(query, currentUserId));
    }

    @PostMapping
    public R<PostVO> createPost(@Valid @RequestBody PostCreateRequest request) {
        if (!SecurityUtils.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return R.ok(postService.createPost(request));
    }

    @GetMapping("/{id}")
    public R<PostVO> getPostDetail(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return R.ok(postService.getPostDetail(id, currentUserId));
    }

    @PatchMapping("/{id}")
    public R<PostVO> updatePost(@PathVariable Long id,
                                @Valid @RequestBody PostUpdateRequest request) {
        if (!SecurityUtils.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return R.ok(postService.updatePost(id, request));
    }

    @DeleteMapping("/{id}")
    public R<Void> deletePost(@PathVariable Long id) {
        if (!SecurityUtils.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        postService.deletePost(id);
        return R.ok("删除成功");
    }
}
