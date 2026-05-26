package com.techhub.dto.category;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CategoryNoticeVO {
    private String id;
    private String categoryId;
    private String title;
    private String content;
    private Integer type;
    private String authorId;
    private String authorName;
    private Integer isPinned;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
