package com.springboot.monew.users.mapper;

import com.springboot.monew.interest.dto.response.SubscriptionDto;
import com.springboot.monew.users.document.UserActivityDocument;
import com.springboot.monew.users.document.UserActivityDocument.ArticleViewItem;
import com.springboot.monew.users.document.UserActivityDocument.CommentItem;
import com.springboot.monew.users.document.UserActivityDocument.CommentLikeItem;
import com.springboot.monew.users.document.UserActivityDocument.SubscriptionItem;
import com.springboot.monew.users.dto.response.CommentActivityDto;
import com.springboot.monew.users.dto.response.CommentLikeActivityDto;
import com.springboot.monew.users.dto.response.TemporaryArticleViewDto;
import com.springboot.monew.users.dto.response.UserActivityDto;
import com.springboot.monew.users.entity.User;
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

  SubscriptionDto toDto(SubscriptionItem item);

  CommentActivityDto toDto(CommentItem item);

  CommentLikeActivityDto toDto(CommentLikeItem item);

  TemporaryArticleViewDto toDto(ArticleViewItem item);
}
