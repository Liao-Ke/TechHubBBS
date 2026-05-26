package com.techhub.dto.user;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserProfileVO {
    private String id;    // String for JS snowflake precision
    private String username;
    private String avatarUrl;
    private String bio;
    private String role;
    private Integer status;
    private LocalDateTime createTime;
}
