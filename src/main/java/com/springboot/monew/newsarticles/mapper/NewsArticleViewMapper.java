package com.springboot.monew.newsarticles.mapper;

import com.springboot.monew.newsarticles.dto.response.NewsArticleViewDto;
import com.springboot.monew.newsarticles.entity.ArticleView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NewsArticleViewMapper {

  @Mapping(target = "viewedBy", source = "articleView.user.id")
  @Mapping(target = "articleId", source = "articleView.newsArticle.id")
  @Mapping(target = "source", source = "articleView.newsArticle.source")
  @Mapping(target = "sourceUrl", source = "articleView.newsArticle.originalLink")
  @Mapping(target = "articleTitle", source = "articleView.newsArticle.title")
  @Mapping(target = "articlePublishedDate", source = "articleView.newsArticle.publishedAt")
  @Mapping(target = "articleSummary", source = "articleView.newsArticle.summary")
  @Mapping(target = "articleCommentCount", source = "commentCount")
  @Mapping(target = "articleViewCount", source = "articleView.newsArticle.viewCount")
  NewsArticleViewDto toDto(ArticleView articleView, Long commentCount);

}
