package com.techhub.service;
import com.techhub.dto.comment.CommentVO;
import com.techhub.entity.Comment;
import java.util.List;

public interface DivineCommentService {
    int LIKE_THRESHOLD = 10;
    int RECOMMEND_THRESHOLD = 5;
    int MIN_REGISTER_DAYS = 7;
    int MIN_POST_COMMENTS = 10;

    void recommend(Long commentId);
    void cancelRecommend(Long commentId);
    void checkAndUpdateDivineStatus(Comment comment);
    void forceSetDivine(Long commentId, boolean divine);
    List<CommentVO> listDivineComments(Long postId);
}
