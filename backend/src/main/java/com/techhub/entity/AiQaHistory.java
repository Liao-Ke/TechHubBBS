package com.techhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_qa_history")
public class AiQaHistory {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long postId;

    private String question;

    private String answer;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
