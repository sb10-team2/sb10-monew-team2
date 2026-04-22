package com.springboot.monew.newsarticles.repository;

import com.springboot.monew.newsarticles.entity.ArticleView;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleViewRepository extends JpaRepository<ArticleView, UUID> {

  Boolean existsByNewsArticleIdAndUserId(UUID articleId, UUID userId);

}
