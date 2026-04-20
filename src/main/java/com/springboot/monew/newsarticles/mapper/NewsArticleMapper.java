package com.springboot.monew.newsarticles.mapper;

import com.springboot.monew.newsarticles.dto.response.CollectedArticle;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NewsArticleMapper {

  NewsArticle toEntity(CollectedArticle article);


}
