package com.springboot.monew.newsarticles.repository;

import com.springboot.monew.newsarticles.entity.ArticleInterest;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, UUID> {

    //원본링크목록 조회
    List<NewsArticle> findAllByOriginalLinkIn(Collection<String> originalLinks);


}
