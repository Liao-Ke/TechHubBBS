package com.techhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.techhub.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    @Select("<script>" +
            "SELECT c.id, c.content, c.post_id AS postId, c.user_id AS userId, " +
            "c.parent_id AS parentId, c.reply_to_user_id AS replyToUserId, " +
            "c.like_count AS likeCount, c.recommend_count AS recommendCount, " +
            "c.is_divine AS isDivine, c.divine_time AS divineTime, c.create_time AS createTime, " +
            "p.title AS postTitle, u.username AS username, u.avatar_url AS avatarUrl " +
            "FROM comment c " +
            "LEFT JOIN post p ON c.post_id = p.id " +
            "LEFT JOIN user u ON c.user_id = u.id " +
            "<where>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (c.content LIKE CONCAT('%', #{keyword}, '%') " +
            "    OR p.title LIKE CONCAT('%', #{keyword}, '%'))" +
            "  </if>" +
            "</where>" +
            "ORDER BY c.create_time DESC" +
            "</script>")
    IPage<Comment> selectPageWithPostTitle(Page<Comment> page, @Param("keyword") String keyword);
}
