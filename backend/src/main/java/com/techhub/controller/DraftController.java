package com.techhub.controller;

import com.techhub.common.R;
import com.techhub.dto.DraftSaveRequest;
import com.techhub.entity.PostDraft;
import com.techhub.service.DraftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/drafts")
@RequiredArgsConstructor
public class DraftController {

    private final DraftService draftService;

    @PostMapping
    public R<PostDraft> saveDraft(@Valid @RequestBody DraftSaveRequest request) {
        return R.ok(draftService.saveDraft(request));
    }

    @GetMapping
    public R<List<PostDraft>> listDrafts() {
        return R.ok(draftService.listDrafts());
    }

    @GetMapping("/check")
    public R<PostDraft> checkDraft(@RequestParam(required = false) Long postId) {
        return R.ok(draftService.checkDraft(postId));
    }

    @GetMapping("/{id}")
    public R<PostDraft> getDraft(@PathVariable Long id) {
        return R.ok(draftService.getById(id));
    }

    @DeleteMapping("/{id}")
    public R<Void> deleteDraft(@PathVariable Long id) {
        draftService.deleteDraft(id);
        return R.ok("删除成功");
    }
}
