package com.springboot.monew.newsarticles.repository;

import com.springboot.monew.newsarticles.entity.NewsArticle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, UUID> {

    //원본링크가 있는지 check
    boolean existsByOriginalLink(String originalLink);
}
