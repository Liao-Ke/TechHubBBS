package com.techhub.dto;

import lombok.Data;

@Data
public class DraftSaveRequest {
    private Long postId;
    private String title;
    private String content;
    private Long categoryId;
    private Integer visibility;
}
