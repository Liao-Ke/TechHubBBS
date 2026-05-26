package com.techhub.dto.category;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryNoticeUpdateRequest {
    @Size(max = 100, message = "公告标题不能超过100字")
    private String title;

    private String content;

    private Integer type;

    private Integer isPinned;

    private Integer status;
}
