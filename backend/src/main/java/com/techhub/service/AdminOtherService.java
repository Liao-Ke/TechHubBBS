package com.techhub.service;

import com.techhub.common.PageResult;
import com.techhub.dto.category.CategoryNoticeVO;

/**
 * 管理员杂项管理服务（神评强制干预、公告全量查询）。
 */
public interface AdminOtherService {

    /**
     * 强制设置/取消神评。
     */
    void forceSetDivine(Long commentId, boolean divine);

    /**
     * 分页查询所有公告，支持按版块和类型过滤。
     */
    PageResult<CategoryNoticeVO> listNotices(int page, int size, Long categoryId, Integer type);
}
