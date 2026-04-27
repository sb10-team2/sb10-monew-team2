package com.springboot.monew.comment.entity;

import com.springboot.monew.common.entity.BaseUpdatableEntity;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.users.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseUpdatableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, columnDefinition = "UUID")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "article_id", nullable = false, columnDefinition = "UUID")
  private NewsArticle article;

  // 댓글 내용
  @Column(name = "content", nullable = false, length = 200)
  private String content;

  // 좋아요 수
  @Column(name = "like_count", nullable = false)
  private long likeCount;

  // 삭제 여부 -> 논리 삭제
  @Column(name = "is_deleted", nullable = false)
  private boolean isDeleted;

  public Comment(
      User user,
      NewsArticle article,
      String content) {
    this.user = user;
    this.article = article;
    this.content = content;
    this.likeCount = 0;
    this.isDeleted = false;
  }

  // === 비즈니스 로직 ===
  // 메시지 내용 수정
  public void updateContent(String content) {
    this.content = content;
  }

  // 논리 삭제 시
  public void delete() {
    this.isDeleted = true;
  }
}
