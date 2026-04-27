package com.springboot.monew.newsarticles.repository;

import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.newsarticles.repository.qdsl.NewsArticleQDSLRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, UUID>,
    NewsArticleQDSLRepository {

  //원본링크목록 조회
  List<NewsArticle> findAllByOriginalLinkIn(Collection<String> originalLinks);

  //뉴스기사 조회수 증가
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE NewsArticle n SET n.viewCount = n.viewCount + 1 WHERE n.id = :articleId")
  void incrementViewCount(@Param("articleId") UUID articleId);


}
