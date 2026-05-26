package com.techhub.service;

/**
 * 帖子关键词与相似度服务。
 */
public interface PostKeywordService {

    /**
     * 更新帖子的关键词提取与相似度矩阵。
     */
    void updatePostKeywords(Long postId, String title, String content);
}
