package com.springboot.monew.comment.mapper;

import com.springboot.monew.comment.dto.CommentLikeDto;
import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.user.document.UserActivityDocument.CommentLikeItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentLikeMapper {

  @Mapping(source = "user.id", target = "likeBy")
  @Mapping(source = "comment.id", target = "commentId")
  @Mapping(source = "comment.article.id", target = "articleId")
  @Mapping(source = "comment.user.id", target = "commentUserId")
  @Mapping(source = "comment.user.nickname", target = "commentUserNickname")
  @Mapping(source = "comment.content", target = "commentContent")
  @Mapping(source = "comment.likeCount", target = "commentLikeCount")
  @Mapping(source = "comment.createdAt", target = "commentCreatedAt")
  CommentLikeDto toCommentLikeDto(CommentLike commentLike);

  // CommentLike 엔티티를 사용자 활동 문서에 저장할 댓글 좋아요 활동 Item으로 변환한다.
  @Mapping(source = "comment.id", target = "commentId")
  @Mapping(source = "comment.article.id", target = "articleId")
  @Mapping(source = "comment.article.title", target = "articleTitle")
  @Mapping(source = "comment.user.id", target = "commentUserId")
  @Mapping(source = "comment.user.nickname", target = "commentUserNickname")
  @Mapping(source = "comment.content", target = "commentContent")
  @Mapping(source = "comment.likeCount", target = "commentLikeCount")
  @Mapping(source = "comment.createdAt", target = "commentCreatedAt")
  CommentLikeItem toCommentLikeItem(CommentLike commentLike);
}
