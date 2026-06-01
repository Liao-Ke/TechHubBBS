package com.techhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("post_draft")
public class PostDraft {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long postId;

    private String title;

    private String content;

    private Long categoryId;

    private Integer visibility;

    @TableField(exist = false)
    private String categoryName;

    private LocalDateTime lastSavedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
