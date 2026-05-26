package com.techhub.dto.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentCreateRequest {
    @NotBlank(message = "评论内容不能为空")
    private String content;

    private Long parentId;        // null for top-level

    private Long replyToUserId;   // null if not replying to specific user
}
