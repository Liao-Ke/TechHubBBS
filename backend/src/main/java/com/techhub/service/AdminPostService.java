package com.techhub.service;

import com.techhub.common.PageResult;
import com.techhub.dto.post.PostVO;

/**
 * 管理员/版主帖子管理服务。
 */
public interface AdminPostService {

    /**
     * 分页查询所有帖子（无可见性过滤），支持关键词搜索。
     */
    PageResult<PostVO> listAllPosts(int page, int size, String keyword);

    /**
     * 设置帖子类型（加精/置顶）。
     *
     * @param type 1=精华, 2=置顶, 0=普通
     */
    void setPostType(Long postId, int type);

    /**
     * 切换帖子锁定状态。
     */
    void togglePostLock(Long postId);

    /**
     * 强制物理删除帖子（仅管理员）。
     */
    void forceDeletePost(Long postId);
}
