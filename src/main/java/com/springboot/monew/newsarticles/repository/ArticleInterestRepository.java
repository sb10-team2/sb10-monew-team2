package com.springboot.monew.newsarticles.repository;

import com.springboot.monew.newsarticles.entity.ArticleInterest;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleInterestRepository extends JpaRepository<ArticleInterest, UUID> {

  //뉴스기사 ID들로부터 뉴스기사_관심사 테이블의 데이터를 조회한다.
  List<ArticleInterest> findAllByNewsArticleIn(List<NewsArticle> newsArticles);

}
