package com.techhub.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostCreateRequest {
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不能超过200字")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    @NotNull(message = "版块不能为空")
    private Long categoryId;

    private Integer visibility;  // 0-3, defaults to 0 (PUBLIC) in service

    private Long draftPostId;    // nullable — if provided, delete draft after publish
}
