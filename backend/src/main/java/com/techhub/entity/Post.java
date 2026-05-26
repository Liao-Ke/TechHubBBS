package com.techhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("post")
public class Post {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String title;

    private String content;

    private Long categoryId;

    private Long authorId;

    private Integer type;

    private Integer status;

    private Integer visibility;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Integer divineCommentCount;

    private Integer eligibleForDivine;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
