package com.techhub.dto.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推荐帖子展示对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationVO {
    private String postId;
    private String title;
    private String authorName;
    private String authorAvatar;
    private Integer likeCount;
    private Integer commentCount;
    private Integer viewCount;
    private Double similarityScore;  // 相似度得分，冷启动热门帖子为 null
    private String reason;           // 推荐理由: "content_based" / "hot"
}
