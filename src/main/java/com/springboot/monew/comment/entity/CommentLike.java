package com.springboot.monew.comment.entity;

import com.springboot.monew.common.entity.BaseEntity;
import com.springboot.monew.users.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "comment_likes")
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentLike extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "comment_id", nullable = false)
  private Comment comment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  public CommentLike(Comment comment, User user) {
    this.comment = comment;
    this.user = user;
  }
}
