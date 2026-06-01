package com.techhub.dto.comment;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentVO {
    private String id;
    private String content;
    private String postId;
    private String userId;
    private String username;           // joined from user
    private String avatarUrl;          // joined from user
    private String postTitle;          // joined from post (admin view)
    private Long parentId;
    private Long replyToUserId;
    private Integer likeCount;
    private Integer recommendCount;
    private Boolean isDivine;
    private LocalDateTime divineTime;
    private Boolean liked;             // current user's like status
    private LocalDateTime createTime;
}
