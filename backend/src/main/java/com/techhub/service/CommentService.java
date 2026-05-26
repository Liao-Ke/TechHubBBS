package com.techhub.service;

import com.techhub.common.PageResult;
import com.techhub.dto.comment.CommentCreateRequest;
import com.techhub.dto.comment.CommentVO;

public interface CommentService {

    /**
     * 分页查询帖子的评论列表（按创建时间升序）。
     *
     * @param postId        帖子 ID
     * @param page          页码
     * @param size          每页条数
     * @param currentUserId 当前登录用户 ID，未登录时为 null
     */
    PageResult<CommentVO> listByPost(Long postId, int page, int size, Long currentUserId);

    /**
     * 发表评论。
     *
     * @param postId  帖子 ID
     * @param request 评论内容
     * @return 评论 VO（含作者信息）
     */
    CommentVO create(Long postId, CommentCreateRequest request);

    /**
     * 删除评论（仅作者或管理员/版主可删除）。
     *
     * @param commentId 评论 ID
     */
    void delete(Long commentId);
}
