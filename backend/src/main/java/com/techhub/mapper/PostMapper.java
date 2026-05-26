package com.techhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.techhub.dto.post.PostListQuery;
import com.techhub.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    List<Post> selectPageWithVisibility(
            @Param("query") PostListQuery query,
            @Param("userId") Long userId,
            @Param("isLoggedIn") boolean isLoggedIn
    );
}
