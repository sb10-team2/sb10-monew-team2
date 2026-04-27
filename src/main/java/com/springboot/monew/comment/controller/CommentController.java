package com.springboot.monew.comment.controller;

import com.springboot.monew.comment.dto.*;
import com.springboot.monew.comment.service.CommentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "댓글 관리", description = "댓글 관련 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController implements CommentApiDocs {
  private final CommentService commentService;

  // 댓글 목록 조회 API
  @GetMapping
  public CursorPageResponseCommentDto<CommentDto> list(
      @Valid CommentPageRequest request, @RequestHeader("Monew-Request-User-ID") UUID userId) {
    return commentService.list(request, userId);
  }

  // 댓글 등록 API
  @PostMapping
  public ResponseEntity<CommentDto> create(@Valid @RequestBody CommentRegisterRequest request) {
    CommentDto commentDto = commentService.create(request);
    return ResponseEntity.created(URI.create("/api/comments/" + commentDto.id())).body(commentDto);
  }

  // 댓글 논리 삭제 API
  @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> softDelete(@PathVariable UUID commentId) {
    commentService.softDelete(commentId);
    return ResponseEntity.noContent().build();
  }

  // 댓글 정보 수정 API
  @PatchMapping("/{commentId}")
  public ResponseEntity<CommentDto> update(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID userId, // 여기!
      @Valid @RequestBody CommentUpdateRequest request) {
    return ResponseEntity.ok(commentService.update(commentId, userId, request));
  }

  // 댓글 물리 삭제 API
  @DeleteMapping("/{commentId}/hard")
  public ResponseEntity<Void> hardDelete(@PathVariable UUID commentId) {
    commentService.hardDelete(commentId);
    return ResponseEntity.noContent().build();
  }
}
