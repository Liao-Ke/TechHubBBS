package com.techhub.service;

public interface InteractionService {

    /** 点赞帖子 */
    void likePost(Long postId);

    /** 取消点赞帖子（幂等） */
    void unlikePost(Long postId);

    /** 收藏帖子 */
    void favoritePost(Long postId);

    /** 取消收藏帖子（幂等） */
    void unfavoritePost(Long postId);

    /** 点赞评论 */
    void likeComment(Long commentId);

    /** 取消点赞评论（幂等） */
    void unlikeComment(Long commentId);
}
