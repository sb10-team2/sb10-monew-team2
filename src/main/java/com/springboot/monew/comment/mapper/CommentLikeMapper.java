package com.springboot.monew.comment.mapper;

import com.springboot.monew.comment.dto.CommentLikeDto;
import com.springboot.monew.comment.entity.CommentLike;
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
}
