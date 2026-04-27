package com.springboot.monew.newsarticles.mapper;

import com.springboot.monew.newsarticles.dto.response.CollectedArticle;
import com.springboot.monew.newsarticles.dto.response.NewsArticleDto;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NewsArticleMapper {

  NewsArticle toEntity(CollectedArticle article);

  @Mapping(target = "commentCount", source = "commentCount")
  @Mapping(target = "viewedByMe", source = "viewedByMe")
  @Mapping(target = "sourceUrl", source = "article.originalLink")
  NewsArticleDto toDto(NewsArticle article, Long commentCount, Boolean viewedByMe);
}
