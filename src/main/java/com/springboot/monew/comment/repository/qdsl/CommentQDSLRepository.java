package com.springboot.monew.comment.repository.qdsl;

import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.entity.CommentDirection;
import com.springboot.monew.comment.entity.CommentOrderBy;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CommentQDSLRepository {

  List<Comment> findComments(UUID articleId, CommentOrderBy orderBy,
      CommentDirection direction, String cursor, Instant after, int limit);


}
