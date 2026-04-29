package com.springboot.monew.user.mapper;

import com.springboot.monew.interest.dto.response.SubscriptionDto;
import com.springboot.monew.newsarticles.dto.response.NewsArticleViewDto;
import com.springboot.monew.user.document.UserActivityDocument;
import com.springboot.monew.user.document.UserActivityDocument.ArticleViewItem;
import com.springboot.monew.user.document.UserActivityDocument.CommentItem;
import com.springboot.monew.user.document.UserActivityDocument.CommentLikeItem;
import com.springboot.monew.user.document.UserActivityDocument.SubscriptionItem;
import com.springboot.monew.user.dto.response.CommentActivityDto;
import com.springboot.monew.user.dto.response.CommentLikeActivityDto;
import com.springboot.monew.user.dto.response.UserActivityDto;
import com.springboot.monew.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserActivityMapper {

  @Mapping(target = "subscriptions", expression = "java(java.util.List.of())")
  @Mapping(target = "comments", expression = "java(java.util.List.of())")
  @Mapping(target = "commentLikes", expression = "java(java.util.List.of())")
  @Mapping(target = "articleViews", expression = "java(java.util.List.of())")
  UserActivityDto toEmptyDto(User user);

  UserActivityDto toDto(UserActivityDocument document);

  @Mapping(target = "interestSubscriberCount", ignore = true)
  SubscriptionDto toDto(SubscriptionItem item);

  CommentActivityDto toDto(CommentItem item);

  CommentLikeActivityDto toDto(CommentLikeItem item);

  NewsArticleViewDto toDto(ArticleViewItem item);
}
