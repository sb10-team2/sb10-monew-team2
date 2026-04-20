package com.springboot.monew.comment.entity;

public enum CommentOrderBy {
  createdAt {
    @Override
    public String getCursor(Comment comment) {
      return comment.getCreatedAt().toString();
    }
  },
  likeCount {
    @Override
    public String getCursor(Comment comment) {
      return String.valueOf(comment.getLikeCount());
    }
  };

  public abstract String getCursor(Comment comment);
}
