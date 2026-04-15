package com.springboot.monew.exception.comment;

import com.springboot.monew.exception.ErrorCode;

import java.util.Map;
import java.util.UUID;

public class CommentNotFoundException extends CommentException {
    public CommentNotFoundException(UUID commentId) {
        super(ErrorCode.COMMENT_NOT_FOUND, Map.of("commentId", commentId));
    }
}
