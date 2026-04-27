package com.springboot.monew.comment.controller;

import com.springboot.monew.comment.dto.CommentLikeDto;
import com.springboot.monew.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

public interface CommentLikeApiDocs {

  @Operation(
      summary = "댓글 좋아요",
      description = """
          `POST /api/comments/{commentId}/comment-likes`

          댓글에 좋아요를 추가합니다. 동일한 댓글에 중복 좋아요는 불가합니다.""",
      operationId = "like"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "댓글 좋아요 성공",
          content = @Content(schema = @Schema(implementation = CommentLikeDto.class))
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Monew-Request-User-ID 헤더 누락",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "404",
          description = """
              리소스 없음
              - 댓글을 찾을 수 없음 (CM01)
              - 사용자를 찾을 수 없음 (UR03)""",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "409",
          description = "이미 좋아요를 누른 댓글입니다 (CM03)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<CommentLikeDto> like(
      @Parameter(description = "댓글 ID") @PathVariable UUID commentId,
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID") UUID userId
  );

  @Operation(
      summary = "댓글 좋아요 취소",
      description = """
          `DELETE /api/comments/{commentId}/comment-likes`

          댓글 좋아요를 취소합니다. 좋아요를 누르지 않은 댓글은 취소할 수 없습니다.""",
      operationId = "unlike"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "댓글 좋아요 취소 성공"
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Monew-Request-User-ID 헤더 누락",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "404",
          description = """
              리소스 없음
              - 댓글을 찾을 수 없음 (CM01)
              - 좋아요를 누르지 않은 댓글입니다 (CM04)""",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  void unlike(
      @Parameter(description = "댓글 ID") @PathVariable UUID commentId,
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID") UUID userId
  );
}
