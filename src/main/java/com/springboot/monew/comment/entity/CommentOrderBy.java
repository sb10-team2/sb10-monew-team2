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
      return comment.getLikeCount() + "|" + comment.getCreatedAt().toString();
    }
  };

  public abstract String getCursor(Comment comment);
}
