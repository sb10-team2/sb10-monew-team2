package com.springboot.monew.comment.mapper;

import com.springboot.monew.comment.dto.CommentLikeDto;
import com.springboot.monew.comment.entity.CommentLike;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentLikeMapper {

    @Mapping(source = "userId", target = "likeBy", ignore = true) // Todo: User 미구현 - 좋아요한 사용자Id
    @Mapping(source = "comment.id", target = "commentId")
    @Mapping(source = "comment.article.id", target = "articleId") // Todo: Article 미구현 - 기사 Id
    @Mapping(source = "comment.user.id", target = "commentUserId") // Todo: User 미구현 - 댓글 작성자 Id
    @Mapping(source = "comment.user.nickName", target = "userNickname") // Todo: User 미구현 - 댓글 작성자 닉네임
    @Mapping(source = "comment.content", target = "commentContent")
    @Mapping(source = "comment.likeCount", target = "commentLikeCount")
    @Mapping(source = "comment.createdAt", target = "commentCreatedAt")
    CommentLikeDto toCommentLikeDto(CommentLike commentLike);
}
