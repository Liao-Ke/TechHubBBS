package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.ResultCode;
import com.techhub.dto.category.CategoryNoticeCreateRequest;
import com.techhub.dto.category.CategoryNoticeUpdateRequest;
import com.techhub.dto.category.CategoryNoticeVO;
import com.techhub.entity.CategoryNotice;
import com.techhub.entity.User;
import com.techhub.mapper.CategoryNoticeMapper;
import com.techhub.mapper.UserMapper;
import com.techhub.security.SecurityUtils;
import com.techhub.service.CategoryNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryNoticeServiceImpl implements CategoryNoticeService {

    private final CategoryNoticeMapper categoryNoticeMapper;
    private final UserMapper userMapper;

    @Override
    public List<CategoryNoticeVO> listByCategory(Long categoryId, Integer type) {
        LambdaQueryWrapper<CategoryNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CategoryNotice::getCategoryId, categoryId)
                .eq(CategoryNotice::getStatus, 1);
        if (type != null) {
            wrapper.eq(CategoryNotice::getType, type);
        }
        wrapper.orderByDesc(CategoryNotice::getIsPinned)
                .orderByDesc(CategoryNotice::getCreateTime);

        List<CategoryNotice> notices = categoryNoticeMapper.selectList(wrapper);
        return notices.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public PageResult<CategoryNoticeVO> getNoticePage(Integer type, int page, int size) {
        LambdaQueryWrapper<CategoryNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CategoryNotice::getStatus, 1);
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
        return new PageResult<>(vos, result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    @Transactional
    public CategoryNoticeVO create(Long categoryId, CategoryNoticeCreateRequest request, Long authorId) {
        if (authorId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "需要登录");
        }

        CategoryNotice notice = new CategoryNotice();
        notice.setCategoryId(categoryId);
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setType(request.getType());
        notice.setAuthorId(authorId);
        notice.setIsPinned(request.getIsPinned() != null ? request.getIsPinned() : 0);
        notice.setStatus(1);
        categoryNoticeMapper.insert(notice);
        return toVO(notice);
    }

    @Override
    public CategoryNoticeVO getById(Long id) {
        CategoryNotice notice = categoryNoticeMapper.selectOne(
                new LambdaQueryWrapper<CategoryNotice>()
                        .eq(CategoryNotice::getId, id)
                        .eq(CategoryNotice::getStatus, 1)
        );
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        return toVO(notice);
    }

    @Override
    @Transactional
    public void update(Long noticeId, CategoryNoticeUpdateRequest request) {
        CategoryNotice notice = categoryNoticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }

        checkPermission(notice);

        if (request.getTitle() != null) {
            notice.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            notice.setContent(request.getContent());
        }
        if (request.getType() != null) {
            notice.setType(request.getType());
        }
        if (request.getIsPinned() != null) {
            notice.setIsPinned(request.getIsPinned());
        }
        if (request.getStatus() != null) {
            notice.setStatus(request.getStatus());
        }

        categoryNoticeMapper.updateById(notice);
    }

    @Override
    @Transactional
    public void delete(Long noticeId) {
        CategoryNotice notice = categoryNoticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }

        checkPermission(notice);

        categoryNoticeMapper.deleteById(noticeId);
    }

    /**
     * Check if current user has permission to edit/delete the notice.
     * Only the author, ADMIN, or MODERATOR can modify.
     */
    private void checkPermission(CategoryNotice notice) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentRole = SecurityUtils.getCurrentRole();

        boolean isAuthor = notice.getAuthorId().equals(currentUserId);
        boolean isAdminOrMod = "ADMIN".equals(currentRole) || "MODERATOR".equals(currentRole);

        if (!isAuthor && !isAdminOrMod) {
            throw new BusinessException(ResultCode.FORBIDDEN, "没有权限编辑此公告");
        }
    }

    private CategoryNoticeVO toVO(CategoryNotice notice) {
        CategoryNoticeVO vo = new CategoryNoticeVO();
        BeanUtils.copyProperties(notice, vo);
        vo.setId(notice.getId().toString());
        vo.setCategoryId(notice.getCategoryId().toString());
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
