package com.techhub.dto.post;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PostListQuery {
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    @Min(value = 1, message = "每页至少1条")
    @Max(value = 50, message = "每页最多50条")
    private Integer size = 20;
    private Long categoryId;
    private String keyword;
    private String sort;  // "new" or "hot"
}
