package com.springboot.monew.newsarticle.repository.qdsl;

import com.springboot.monew.newsarticle.dto.request.NewsArticlePageRequest;
import com.springboot.monew.newsarticle.dto.response.NewsArticleCursorRow;
import java.util.List;
import java.util.UUID;

public interface NewsArticleQDSLRepository {

  List<NewsArticleCursorRow> findNewsArticles(NewsArticlePageRequest request, UUID userId);

  //전체 데이터 개수 조회
  long countNewsArticles(NewsArticlePageRequest request);

}
