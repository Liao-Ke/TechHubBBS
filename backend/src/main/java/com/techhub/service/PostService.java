package com.techhub.service;

import com.techhub.common.PageResult;
import com.techhub.dto.post.PostCreateRequest;
import com.techhub.dto.post.PostListQuery;
import com.techhub.dto.post.PostUpdateRequest;
import com.techhub.dto.post.PostVO;

/**
 * 帖子服务接口 — 帖子 CRUD 与可见性过滤。
 */
public interface PostService {

    /**
     * 分页查询帖子列表，根据 visibility 过滤当前用户不可见的帖子。
     *
     * @param query         查询条件（分页、版块、关键词、排序）
     * @param currentUserId 当前登录用户 ID，null 表示未登录
     * @return 分页结果
     */
    PageResult<PostVO> listPosts(PostListQuery query, Long currentUserId);

    /**
     * 发布新帖子。
     *
     * @param request 帖子创建请求
     * @return 创建的帖子视图
     */
    PostVO createPost(PostCreateRequest request);

    /**
     * 获取帖子详情（含可见性校验，浏览量 +1）。
     *
     * @param postId        帖子 ID
     * @param currentUserId 当前登录用户 ID，null 表示未登录
     * @return 帖子详情视图
     */
    PostVO getPostDetail(Long postId, Long currentUserId);

    /**
     * 编辑帖子（仅作者或管理员可操作）。
     *
     * @param postId  帖子 ID
     * @param request 帖子更新请求（PATCH 语义，仅更新非 null 字段）
     * @return 更新后的帖子视图
     */
    PostVO updatePost(Long postId, PostUpdateRequest request);

    /**
     * 软删除帖子（仅作者或管理员可操作）。
     *
     * @param postId 帖子 ID
     */
    void deletePost(Long postId);
}
