package com.techhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_profile")
public class UserProfile {
    @TableId(type = IdType.INPUT)
    private Long userId;

    private String keywordWeights;

    private LocalDateTime lastUpdateTime;
}
