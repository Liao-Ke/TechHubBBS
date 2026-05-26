package com.techhub.dto.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 相似帖子展示对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelatedPostVO {
    private String postId;
    private String title;
    private String authorName;
    private Double similarityScore;
}
