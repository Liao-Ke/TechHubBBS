package com.techhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String content;

    private Long postId;

    private Long userId;

    private Long parentId;

    private Long replyToUserId;

    private Integer likeCount;

    private Integer recommendCount;

    private Integer isDivine;

    private LocalDateTime divineTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
