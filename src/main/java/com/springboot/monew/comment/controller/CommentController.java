package com.springboot.monew.comment.controller;

import com.springboot.monew.comment.dto.CommentDto;
import com.springboot.monew.comment.dto.CommentLikeDto;
import com.springboot.monew.comment.dto.CommentRegisterRequest;
import com.springboot.monew.comment.dto.CommentUpdateRequest;
import com.springboot.monew.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController implements CommentApiDocs{
    private final CommentService commentService;

    // TODO:댓글 목록 조회 API
    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok().build();
    }

    // 댓글 등록 API
    @PostMapping
    public ResponseEntity<CommentDto> create(@Valid @RequestBody CommentRegisterRequest request) {
        CommentDto commentDto = commentService.create(request);
        return ResponseEntity.created(URI.create("/api/comments/" + commentDto.id()))
                .body(commentDto);
    }

    // 관심사 댓글 좋아요 API
    @PostMapping("/{commentId}/comment-likes")
    public ResponseEntity<CommentLikeDto> like(
            @PathVariable UUID commentId,
            @RequestHeader("Monew-Request-User-ID") UUID userId
    ) {
        CommentLikeDto commentLikeDto = commentService.like(commentId, userId);
        return ResponseEntity.created(URI.create("/api/comments/"+ commentLikeDto.commentId() + "/comment-likes"+ commentLikeDto.id()))
                .body(commentLikeDto);
    }

    // TODO: 댓글 좋아요 취소 API
    @DeleteMapping("/{commentId}/comment-likes")
    public ResponseEntity<?> unlike() {
        return ResponseEntity.ok().build();
    }

    // 댓글 논리 삭제 API
    @DeleteMapping("/{commentId}")
    public void softDelete(@PathVariable UUID commentId) {
        commentService.softDelete(commentId);
    }

    // 댓글 정보 수정 API
    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentDto> update(
            @PathVariable UUID commentId,
            @RequestHeader("Monew-Request-User-ID") UUID userId,  // 여기!
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        return ResponseEntity.ok(commentService.update(commentId, userId, request));
    }

    // TODO: 댓글 물리 삭제 API
    @DeleteMapping("/{commentId}/hard")
    public ResponseEntity<?> hardDelete() {
        return ResponseEntity.ok().build();
    }

}
