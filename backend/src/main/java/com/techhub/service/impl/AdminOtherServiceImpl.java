package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.techhub.common.PageResult;
import com.techhub.dto.category.CategoryNoticeVO;
import com.techhub.entity.CategoryNotice;
import com.techhub.entity.User;
import com.techhub.mapper.CategoryNoticeMapper;
import com.techhub.mapper.UserMapper;
import com.techhub.service.AdminOtherService;
import com.techhub.service.DivineCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOtherServiceImpl implements AdminOtherService {

    private final DivineCommentService divineCommentService;
    private final CategoryNoticeMapper categoryNoticeMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public void forceSetDivine(Long commentId, boolean divine) {
        divineCommentService.forceSetDivine(commentId, divine);
    }

    @Override
    public PageResult<CategoryNoticeVO> listNotices(int page, int size, Long categoryId, Integer type) {
        LambdaQueryWrapper<CategoryNotice> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(CategoryNotice::getCategoryId, categoryId);
        }
        if (type != null) {
            wrapper.eq(CategoryNotice::getType, type);
        }
        wrapper.orderByDesc(CategoryNotice::getIsPinned)
                .orderByDesc(CategoryNotice::getCreateTime);

        Page<CategoryNotice> mpPage = new Page<>(page, size);
        Page<CategoryNotice> result = categoryNoticeMapper.selectPage(mpPage, wrapper);

        List<CategoryNoticeVO> vos = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(vos, result.getTotal(), result.getSize(), result.getCurrent());
    }

    private CategoryNoticeVO toVO(CategoryNotice notice) {
        CategoryNoticeVO vo = new CategoryNoticeVO();
        BeanUtils.copyProperties(notice, vo);
        vo.setId(notice.getId().toString());
        vo.setCategoryId(notice.getCategoryId() != null ? notice.getCategoryId().toString() : null);
        vo.setAuthorId(notice.getAuthorId() != null ? notice.getAuthorId().toString() : null);

        if (notice.getAuthorId() != null) {
            User author = userMapper.selectById(notice.getAuthorId());
            if (author != null) {
                vo.setAuthorName(author.getUsername());
            }
        }
        return vo;
    }
}
