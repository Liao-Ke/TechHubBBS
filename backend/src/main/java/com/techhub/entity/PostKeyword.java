package com.techhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("post_keyword")
public class PostKeyword {
    private Long postId;

    private String keyword;

    private Double tfidfWeight;
}
