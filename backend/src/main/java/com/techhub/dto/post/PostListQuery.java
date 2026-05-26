package com.techhub.dto.post;

import lombok.Data;

@Data
public class PostListQuery {
    private Integer page = 1;
    private Integer size = 20;
    private Long categoryId;
    private String keyword;
    private String sort;  // "new" or "hot"
}
