package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.techhub.common.BusinessException;
import com.techhub.common.ResultCode;
import com.techhub.dto.DraftSaveRequest;
import com.techhub.entity.PostDraft;
import com.techhub.mapper.PostDraftMapper;
import com.techhub.security.SecurityUtils;
import com.techhub.service.DraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DraftServiceImpl implements DraftService {

    private final PostDraftMapper postDraftMapper;

    @Override
    @Transactional
    public PostDraft saveDraft(DraftSaveRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "需要登录");
        }

        LambdaQueryWrapper<PostDraft> wrapper = new LambdaQueryWrapper<PostDraft>()
                .eq(PostDraft::getUserId, userId);
        if (request.getPostId() != null) {
            wrapper.eq(PostDraft::getPostId, request.getPostId());
        } else {
            wrapper.isNull(PostDraft::getPostId);
        }

        PostDraft existing = postDraftMapper.selectOne(wrapper);
        if (existing != null) {
            existing.setTitle(request.getTitle());
            existing.setContent(request.getContent());
            existing.setCategoryId(request.getCategoryId());
            existing.setVisibility(request.getVisibility());
            existing.setLastSavedAt(LocalDateTime.now());
            postDraftMapper.updateById(existing);
            return existing;
        } else {
            PostDraft draft = new PostDraft();
            draft.setUserId(userId);
            draft.setPostId(request.getPostId());
            draft.setTitle(request.getTitle());
            draft.setContent(request.getContent());
            draft.setCategoryId(request.getCategoryId());
            draft.setVisibility(request.getVisibility());
            draft.setLastSavedAt(LocalDateTime.now());
            postDraftMapper.insert(draft);
            return draft;
        }
    }

    @Override
    public List<PostDraft> listDrafts() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "需要登录");
        }

        LambdaQueryWrapper<PostDraft> wrapper = new LambdaQueryWrapper<PostDraft>()
                .eq(PostDraft::getUserId, userId)
                .orderByDesc(PostDraft::getUpdateTime);
        return postDraftMapper.selectList(wrapper);
    }

    @Override
    public PostDraft checkDraft(Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "需要登录");
        }

        LambdaQueryWrapper<PostDraft> wrapper = new LambdaQueryWrapper<PostDraft>()
                .eq(PostDraft::getUserId, userId)
                .eq(PostDraft::getPostId, postId);
        return postDraftMapper.selectOne(wrapper);
    }

    @Override
    @Transactional
    public void deleteDraft(Long draftId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "需要登录");
        }

        PostDraft draft = postDraftMapper.selectById(draftId);
        if (draft == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "草稿不存在");
        }

        if (!draft.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此草稿");
        }

        postDraftMapper.deleteById(draftId);
    }
}
