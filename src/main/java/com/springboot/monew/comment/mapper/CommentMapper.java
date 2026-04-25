package com.springboot.monew.comment.mapper;

import com.springboot.monew.comment.dto.CommentDto;
import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.users.document.UserActivityDocument.CommentItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {

  @Mapping(source = "comment.article.id", target = "articleId")
  @Mapping(source = "comment.user.id", target = "userId")
  @Mapping(source = "comment.user.nickname", target = "userNickname")
  @Mapping(source = "likeByMe", target = "likeByMe")
  CommentDto toCommentDto(Comment comment, boolean likeByMe);

  // 사용자 활동 문서에 저장할 댓글 활동 Item으로 변환한다.
  @Mapping(source = "article.id", target = "articleId")
  @Mapping(source = "article.title", target = "articleTitle")
  @Mapping(source = "user.id", target = "userId")
  @Mapping(source = "user.nickname", target = "userNickname")
  CommentItem toCommentItem(Comment comment);
}
