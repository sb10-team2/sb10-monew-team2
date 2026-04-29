package com.springboot.monew.newsarticles.mapper;

import com.springboot.monew.newsarticles.dto.response.NewsArticleViewDto;
import com.springboot.monew.newsarticles.entity.ArticleView;
import com.springboot.monew.users.document.UserActivityDocument.ArticleViewItem;
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
  NewsArticleViewDto toDto(ArticleView articleView, long commentCount);

  // ArticleView 엔티티와 댓글 수를 사용자 활동 문서에 저장할 기사 조회 활동 Item으로 변환한다.
  @Mapping(target = "viewedBy", source = "articleView.user.id")
  @Mapping(target = "articleId", source = "articleView.newsArticle.id")
  @Mapping(target = "source", source = "articleView.newsArticle.source")
  @Mapping(target = "sourceUrl", source = "articleView.newsArticle.originalLink")
  @Mapping(target = "articleTitle", source = "articleView.newsArticle.title")
  @Mapping(target = "articlePublishedDate", source = "articleView.newsArticle.publishedAt")
  @Mapping(target = "articleSummary", source = "articleView.newsArticle.summary")
  @Mapping(target = "articleCommentCount", source = "commentCount")
  @Mapping(target = "articleViewCount", source = "articleView.newsArticle.viewCount")
  ArticleViewItem toArticleViewItem(ArticleView articleView, Long commentCount);
}
