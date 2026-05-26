package com.techhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("category_notice")
public class CategoryNotice {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long categoryId;

    private String title;

    private String content;

    private Integer type;

    private Long authorId;

    private Integer isPinned;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
