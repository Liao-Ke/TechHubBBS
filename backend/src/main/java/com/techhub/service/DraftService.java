package com.techhub.service;

import com.techhub.dto.DraftSaveRequest;
import com.techhub.entity.PostDraft;

import java.util.List;

public interface DraftService {
    PostDraft saveDraft(DraftSaveRequest request);
    List<PostDraft> listDrafts();
    PostDraft checkDraft(Long postId);
    PostDraft getById(Long id);
    void deleteDraft(Long draftId);
}
