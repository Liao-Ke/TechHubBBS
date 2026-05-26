package com.techhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_like")
public class UserLike {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String targetType;     // "POST" or "COMMENT"
    private Long targetId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
