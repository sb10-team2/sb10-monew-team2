package com.springboot.monew.newsarticle.mapper;

import com.springboot.monew.newsarticle.dto.response.CollectedArticle;
import com.springboot.monew.newsarticle.dto.response.NewsArticleDto;
import com.springboot.monew.newsarticle.entity.NewsArticle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NewsArticleMapper {

  NewsArticle toEntity(CollectedArticle article);

  @Mapping(target = "commentCount", source = "commentCount")
  @Mapping(target = "viewedByMe", source = "viewedByMe")
  @Mapping(target = "sourceUrl", source = "article.originalLink")
  @Mapping(target = "publishDate", source = "article.publishedAt")
  NewsArticleDto toDto(NewsArticle article, Long commentCount, Boolean viewedByMe);


}
