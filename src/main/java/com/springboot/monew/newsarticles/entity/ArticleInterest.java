package com.springboot.monew.newsarticles.entity;

import com.springboot.monew.common.entity.BaseEntity;
import com.springboot.monew.interest.entity.Interest;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

//뉴스기사 - 관심사 연결 테이블
//관심사에 해당하는 뉴스기사가 무엇인지 파악.
@Entity
@Getter
@NoArgsConstructor
@Table(
    name = "article_interests",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UK_ARTICLE_INTERESTS_ARTICLE_INTEREST",
            columnNames = {"news_article_id", "interest_id"}
        )
    }
)
public class ArticleInterest extends BaseEntity {

  // 관심사 N : 뉴스기사 1
  // 지연로딩: 지금 당장 필요 없으니까 나중에 진짜 쓸때 가져옴.
  // optional = false : 필수관계(NOT NULL)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)   //뉴스기사 1 관심사 N
  @JoinColumn(
      name = "news_article_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_ARTICLE_INTERESTS_NEWS_ARTICLE")
  )
  private NewsArticle newsArticle;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "interest_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_ARTICLE_INTERESTS_INTEREST")
  )
  private Interest interest;

  public ArticleInterest(NewsArticle newsArticles, Interest interest) {
    this.newsArticle = newsArticles;
    this.interest = interest;
  }


}
