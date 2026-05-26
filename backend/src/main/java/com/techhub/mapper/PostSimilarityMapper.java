package com.techhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.techhub.entity.PostSimilarity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PostSimilarityMapper extends BaseMapper<PostSimilarity> {

    @Select("SELECT * FROM post_similarity WHERE post_id_a = #{postId} OR post_id_b = #{postId} ORDER BY similarity_score DESC LIMIT #{limit}")
    List<PostSimilarity> selectTopSimilar(@Param("postId") Long postId, @Param("limit") int limit);

    @Select("SELECT * FROM post_similarity WHERE (post_id_a = #{postIdA} AND post_id_b = #{postIdB}) OR (post_id_a = #{postIdB} AND post_id_b = #{postIdA})")
    PostSimilarity selectByPostIds(@Param("postIdA") Long postIdA, @Param("postIdB") Long postIdB);

    @Delete("DELETE FROM post_similarity WHERE post_id_a = #{postId} OR post_id_b = #{postId}")
    int deleteByPostId(@Param("postId") Long postId);

    @Select("SELECT * FROM post_similarity")
    List<PostSimilarity> selectAll();
}
