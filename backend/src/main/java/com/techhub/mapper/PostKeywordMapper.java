package com.techhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.techhub.entity.PostKeyword;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PostKeywordMapper extends BaseMapper<PostKeyword> {

    @Select("SELECT * FROM post_keyword WHERE post_id = #{postId}")
    List<PostKeyword> selectByPostId(@Param("postId") Long postId);

    @Delete("DELETE FROM post_keyword WHERE post_id = #{postId}")
    int deleteByPostId(@Param("postId") Long postId);

    @Select("SELECT * FROM post_keyword ORDER BY post_id")
    List<PostKeyword> selectAllKeywords();

    @Select("SELECT COUNT(DISTINCT post_id) FROM post_keyword WHERE keyword = #{keyword}")
    long countPostsByKeyword(@Param("keyword") String keyword);

    @Select("SELECT COUNT(DISTINCT post_id) FROM post_keyword")
    long countTotalPosts();
}
