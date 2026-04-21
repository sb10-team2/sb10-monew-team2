package com.springboot.monew.comment.controller;

import com.springboot.monew.comment.dto.CommentLikeDto;
import com.springboot.monew.comment.service.CommentLikeService;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments/{commentId}/comment-likes")
public class CommentLikeController {
  private final CommentLikeService commentLikeService;

  // 관심사 댓글 좋아요 API
  @PostMapping
  public ResponseEntity<CommentLikeDto> like(
      @PathVariable UUID commentId, @RequestHeader("Monew-Request-User-ID") UUID userId) {
    CommentLikeDto commentLikeDto = commentLikeService.like(commentId, userId);
    return ResponseEntity.created(
            URI.create(
                "/api/comments/"
                    + commentLikeDto.commentId()
                    + "/comment-likes/"
                    + commentLikeDto.id()))
        .body(commentLikeDto);
  }

  // 댓글 좋아요 취소 API
  @DeleteMapping
  public void unlike(
      @PathVariable UUID commentId, @RequestHeader("Monew-Request-User-ID") UUID userId) {
    commentLikeService.unlike(commentId, userId);
  }
}
