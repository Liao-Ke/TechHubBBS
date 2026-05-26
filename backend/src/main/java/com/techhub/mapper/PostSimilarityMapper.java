package com.techhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.techhub.entity.PostSimilarity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PostSimilarityMapper extends BaseMapper<PostSimilarity> {

    @Select("SELECT * FROM post_similarity WHERE post_id_a = #{postId} ORDER BY similarity_score DESC LIMIT #{limit}")
    List<PostSimilarity> selectTopSimilar(@Param("postId") Long postId, @Param("limit") int limit);
}
