package com.techhub.dto.post;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PostVO {
    private String id;                  // String for snowflake
    private String title;
    private String content;
    private String categoryId;
    private String categoryName;        // joined from category table
    private String authorId;
    private String authorName;          // joined from user table
    private String authorAvatar;        // joined from user table
    private Integer type;               // 0/1/2
    private Integer status;
    private Integer visibility;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer divineCommentCount;
    private Boolean liked;              // current user's like status
    private Boolean favorited;          // current user's favorite status
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
