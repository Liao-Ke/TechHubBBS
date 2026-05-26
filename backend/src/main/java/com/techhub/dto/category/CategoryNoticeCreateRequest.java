package com.techhub.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryNoticeCreateRequest {
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 100, message = "公告标题不能超过100字")
    private String title;

    @NotBlank(message = "公告内容不能为空")
    private String content;

    private Integer type;

    private Integer isPinned;
}
