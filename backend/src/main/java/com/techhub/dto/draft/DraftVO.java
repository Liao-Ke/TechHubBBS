package com.techhub.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DraftVO {
    private String id;
    private String postId;
    private String title;
    private String content;
    private String categoryId;
    private String categoryName;
    private Integer visibility;
    private LocalDateTime lastSavedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
