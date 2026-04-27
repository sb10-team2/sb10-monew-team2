package com.springboot.monew.comment.mapper;

import com.springboot.monew.comment.dto.CommentDto;
import com.springboot.monew.comment.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {

  @Mapping(source = "comment.article.id", target = "articleId")
  @Mapping(source = "comment.user.id", target = "userId")
  @Mapping(source = "comment.user.nickname", target = "userNickname")
  @Mapping(source = "likeByMe", target = "likedByMe")
  CommentDto toCommentDto(Comment comment, boolean likeByMe);
}
