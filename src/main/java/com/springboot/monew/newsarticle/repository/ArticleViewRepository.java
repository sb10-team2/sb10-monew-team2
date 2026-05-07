package com.springboot.monew.newsarticle.repository;

import com.springboot.monew.newsarticle.entity.ArticleView;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleViewRepository extends JpaRepository<ArticleView, UUID> {

  Boolean existsByNewsArticleIdAndUserId(UUID articleId, UUID userId);

}
