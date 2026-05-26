package com.techhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("post_similarity")
public class PostSimilarity {
    private Long postIdA;

    private Long postIdB;

    private Double similarityScore;
}
