package com.springboot.monew.newsarticles.entity;

import com.springboot.monew.common.entity.BaseEntity;
import com.springboot.monew.users.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

//사용자:뉴스기사 연결 테이블
@Entity
@Getter
@NoArgsConstructor
@Table(
    name = "article_views",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UK_ARTICLE_VIEWS_ARTICLE_USER",
            columnNames = {"news_article_id", "user_id"}
        )
    }
)
public class ArticleView extends BaseEntity {

  // 사용자 N : 뉴스기사 1
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "news_article_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_ARTICLE_VIEWS_NEWS_ARTICLE")
  )
  private NewsArticle newsArticle;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "user_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_ARTICLE_VIEWS_USER")
  )
  private User user;

  public ArticleView(NewsArticle newsArticle, User user) {
    this.newsArticle = newsArticle;
    this.user = user;
  }

}
